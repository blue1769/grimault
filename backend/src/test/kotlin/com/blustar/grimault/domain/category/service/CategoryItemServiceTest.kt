package com.blustar.grimault.domain.category.service

import com.blustar.grimault.domain.category.dto.CategoryItemResponse
import com.blustar.grimault.domain.category.dto.UpdateCategoryItemRequest
import com.blustar.grimault.domain.category.dto.CreateCategoryItemRequest
import com.blustar.grimault.domain.category.entity.Category
import com.blustar.grimault.domain.category.entity.CategoryItem
import com.blustar.grimault.domain.category.entity.CategoryType
import com.blustar.grimault.domain.category.repository.CategoryItemRepository
import com.blustar.grimault.domain.category.repository.CategoryRepository
import com.blustar.grimault.global.error.ErrorCode
import com.blustar.grimault.global.error.GrimaultException
import io.mockk.*
import jakarta.persistence.EntityManager
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.CsvSource
import java.util.*

class CategoryItemServiceTest {

    private val repository = mockk<CategoryItemRepository>()
    private val groupRepository = mockk<CategoryRepository>()
    private val entityManager = mockk<EntityManager>()

    private val service = CategoryItemService(repository, groupRepository, entityManager)

    @ParameterizedTest
    @CsvSource("null, 1", "1, 2", "5, 6", nullValues = ["null"])
    fun `신규 소분류는 소속 대분류의 마지막 순번에 추가된다`(
        maxSortOrder: Int?,
        expectedSortOrder: Int,
    ) {
        // Arrange
        val categoryId = 1
        val category = Category(
            id = categoryId,
            type = CategoryType.EXPENSE,
            name = "테스트 대분류",
            sortOrder = 1,
        )
        val body = CreateCategoryItemRequest(name = "테스트 소분류")
        val savedId = 10
        val itemSlot = slot<CategoryItem>()
        every { groupRepository.findByIdForUpdate(categoryId) } returns category
        every { repository.findMaxSortOrder(categoryId) } returns maxSortOrder
        every { repository.save(capture(itemSlot)) } answers {
            val item = firstArg<CategoryItem>()
            CategoryItem(
                id = savedId,
                category = item.category,
                name = item.name,
                isActive = item.isActive,
                sortOrder = item.sortOrder,
                isFavorite = item.isFavorite,
            )
        }

        // Act
        val response = service.create(categoryId = categoryId, body = body)

        // Assert
        val savedItem = itemSlot.captured
        assertEquals(categoryId, savedItem.category.id)
        assertEquals(expectedSortOrder, savedItem.sortOrder)
        assertTrue(savedItem.isActive)
        assertFalse(savedItem.isFavorite)
        assertEquals(savedId, response.id)
        assertEquals(categoryId, response.categoryId)
        assertEquals(body.name, response.name)
        assertTrue(response.isActive)
        assertEquals(expectedSortOrder, response.sortOrder)
        assertFalse(response.isFavorite)
        verifySequence {
            groupRepository.findByIdForUpdate(categoryId)
            repository.findMaxSortOrder(categoryId)
            repository.save(any())
        }
    }

    @Test
    fun `존재하지 않는 소분류는 조회할 수 없다`() {
        // Arrange
        val id = 999
        every { repository.findById(id) } returns Optional.empty()

        // Act
        val exception = assertThrows(GrimaultException::class.java) {
            service.findById(id = id)
        }

        // Assert
        assertEquals(ErrorCode.CATEGORY_ITEM_NOT_FOUND, exception.errorCode)
    }

    @Test
    fun `존재하지 않는 대분류에는 소분류를 생성할 수 없다`() {
        // Arrange
        val categoryId = 999
        val body = CreateCategoryItemRequest(name = "테스트 소분류")
        every { groupRepository.findByIdForUpdate(categoryId) } returns null

        // Act
        val exception = assertThrows(GrimaultException::class.java) {
            service.create(categoryId = categoryId, body = body)
        }

        // Assert
        assertEquals(ErrorCode.CATEGORY_NOT_FOUND, exception.errorCode)
        verify(exactly = 0) { repository.findMaxSortOrder(any()) }
        verify(exactly = 0) { repository.save(any()) }
    }


    @Test
    fun `비활성 소분류의 이름을 수정해도 다른 속성은 유지된다`() {
        // Arrange
        val category = Category(id = 1, type = CategoryType.EXPENSE, name = "지출", sortOrder = 1)
        val item = CategoryItem(
            id = 10, category = category, name = "식자재",
            isActive = false, sortOrder = 2, isFavorite = true,
        )
        val body = UpdateCategoryItemRequest(name = "식료품")
        every { repository.findById(10) } returns Optional.of(item)
        every { groupRepository.findByIdForUpdate(1) } returns category
        every { entityManager.refresh(item) } just Runs

        // Act
        val response = service.update(10, body)

        // Assert
        assertEquals(10, item.id)
        assertSame(category, item.category)
        assertEquals("식료품", item.name)
        assertFalse(item.isActive)
        assertEquals(2, item.sortOrder)
        assertTrue(item.isFavorite)
        assertEquals(CategoryItemResponse(10, 1, "식료품", false, 2, true), response)
        verifySequence {
            repository.findById(10)
            groupRepository.findByIdForUpdate(1)
            entityManager.refresh(item)
        }
        verify(exactly = 0) { repository.findMaxSortOrder(any()) }
        verify(exactly = 0) { repository.deferOrderConstraint() }
    }

    @ParameterizedTest
    @CsvSource("false, true, true", "true, false, false")
    fun `즐겨찾기를 지정하거나 해제할 수 있다`(
        initialFavorite: Boolean,
        requestedFavorite: Boolean,
        expectedFavorite: Boolean,
    ) {
        // Arrange
        val category = Category(id = 1, type = CategoryType.EXPENSE, name = "지출", sortOrder = 1)
        val item = CategoryItem(
            id = 10, category = category, name = "식자재",
            isActive = true, sortOrder = 2, isFavorite = initialFavorite,
        )
        val body = UpdateCategoryItemRequest(isFavorite = requestedFavorite)
        every { repository.findById(10) } returns Optional.of(item)
        every { groupRepository.findByIdForUpdate(1) } returns category
        every { entityManager.refresh(item) } just Runs

        // Act
        val response = service.update(10, body)

        // Assert
        assertEquals(10, item.id)
        assertSame(category, item.category)
        assertEquals("식자재", item.name)
        assertTrue(item.isActive)
        assertEquals(2, item.sortOrder)
        assertEquals(expectedFavorite, item.isFavorite)
        assertEquals(CategoryItemResponse(10, 1, "식자재", true, 2, expectedFavorite), response)
        verifySequence {
            repository.findById(10)
            groupRepository.findByIdForUpdate(1)
            entityManager.refresh(item)
        }
        verify(exactly = 0) { repository.findMaxSortOrder(any()) }
        verify(exactly = 0) { repository.deferOrderConstraint() }
    }

    @Test
    fun `이름과 즐겨찾기를 한 요청에서 함께 수정할 수 있다`() {
        // Arrange
        val category = Category(id = 1, type = CategoryType.EXPENSE, name = "지출", sortOrder = 1)
        val item = CategoryItem(
            id = 10, category = category, name = "식자재",
            isActive = true, sortOrder = 2, isFavorite = false,
        )
        val body = UpdateCategoryItemRequest(name = "식료품", isFavorite = true)
        every { repository.findById(10) } returns Optional.of(item)
        every { groupRepository.findByIdForUpdate(1) } returns category
        every { entityManager.refresh(item) } just Runs

        // Act
        val response = service.update(10, body)

        // Assert
        assertEquals(10, item.id)
        assertSame(category, item.category)
        assertEquals("식료품", item.name)
        assertTrue(item.isActive)
        assertEquals(2, item.sortOrder)
        assertTrue(item.isFavorite)
        assertEquals(CategoryItemResponse(10, 1, "식료품", true, 2, true), response)
        verifySequence {
            repository.findById(10)
            groupRepository.findByIdForUpdate(1)
            entityManager.refresh(item)
        }
    }


    @Test
    fun `다른 대분류로 이동하면 목적지의 마지막 순번에 배치된다`() {
        // Arrange
        val originItems = mutableListOf<CategoryItem>()
        val origin = Category(id = 1, type = CategoryType.EXPENSE, name = "출발지", sortOrder = 1, items = originItems)
        val target = Category(id = 2, type = CategoryType.EXPENSE, name = "목적지", sortOrder = 2)
        val sibling = CategoryItem(id = 9, category = origin, name = "형제", sortOrder = 1)
        val item = CategoryItem(id = 10, category = origin, name = "식자재", isActive = false, sortOrder = 2, isFavorite = true)
        val following = CategoryItem(id = 11, category = origin, name = "다음", sortOrder = 3)
        originItems.addAll(listOf(sibling, item, following))
        every { repository.findById(10) } returns Optional.of(item)
        every { groupRepository.findByIdForUpdate(1) } returns origin
        every { groupRepository.findByIdForUpdate(2) } returns target
        every { entityManager.refresh(item) } just Runs
        every { repository.findMaxSortOrder(2) } returns 3
        every { repository.deferOrderConstraint() } just Runs

        // Act
        val response = service.update(10, UpdateCategoryItemRequest(categoryId = 2))

        // Assert
        assertEquals(10, item.id)
        assertSame(target, item.category)
        assertEquals(4, item.sortOrder)
        assertEquals("식자재", item.name)
        assertFalse(item.isActive)
        assertTrue(item.isFavorite)
        assertEquals(CategoryItemResponse(10, 2, "식자재", false, 4, true), response)
        assertEquals(1, sibling.sortOrder)
        assertEquals(2, following.sortOrder)
        verifySequence {
            repository.findById(10)
            groupRepository.findByIdForUpdate(1)
            groupRepository.findByIdForUpdate(2)
            entityManager.refresh(item)
            repository.findMaxSortOrder(2)
            repository.deferOrderConstraint()
        }
    }

    @Test
    fun `소분류가 없는 목적지로 이동하면 순번은 1부터 시작한다`() {
        // Arrange
        val originItems = mutableListOf<CategoryItem>()
        val origin = Category(id = 1, type = CategoryType.EXPENSE, name = "출발지", sortOrder = 1, items = originItems)
        val target = Category(id = 2, type = CategoryType.EXPENSE, name = "목적지", sortOrder = 2)
        val item = CategoryItem(id = 10, category = origin, name = "식자재", isActive = false, sortOrder = 1, isFavorite = true)
        originItems.add(item)
        every { repository.findById(10) } returns Optional.of(item)
        every { groupRepository.findByIdForUpdate(1) } returns origin
        every { groupRepository.findByIdForUpdate(2) } returns target
        every { entityManager.refresh(item) } just Runs
        every { repository.findMaxSortOrder(2) } returns null
        every { repository.deferOrderConstraint() } just Runs

        // Act
        val response = service.update(10, UpdateCategoryItemRequest(categoryId = 2))

        // Assert
        assertEquals(10, item.id)
        assertSame(target, item.category)
        assertEquals(1, item.sortOrder)
        assertEquals("식자재", item.name)
        assertFalse(item.isActive)
        assertTrue(item.isFavorite)
        assertEquals(CategoryItemResponse(10, 2, "식자재", false, 1, true), response)
        assertTrue(origin.items.filter { it.category.id == origin.id }.isEmpty())
    }

    @ParameterizedTest
    @MethodSource("compactCases")
    fun `소분류 제거 후 남은 순번은 상대적 순서를 유지하며 압축된다`(
        pairs: List<Pair<Int, Int>>,
        removedItemId: Int,
        expected: Map<Int, Int>,
    ) {
        // Arrange: 입력 조건은 compactCases에서 제공한다.

        // Act
        val result = CategoryReorderer.compact(pairs, removedItemId)

        // Assert
        assertFalse(result.containsKey(removedItemId))
        assertEquals(expected, result)
    }

    @Test
    fun `중간 소분류를 이동하면 출발지 형제 항목의 순번이 갱신된다`() {
        // Arrange
        val originItems = mutableListOf<CategoryItem>()
        val origin = Category(id = 1, type = CategoryType.EXPENSE, name = "출발지", sortOrder = 1, items = originItems)
        val target = Category(id = 2, type = CategoryType.EXPENSE, name = "목적지", sortOrder = 2)
        val a = CategoryItem(id = 10, category = origin, name = "A", isActive = false, sortOrder = 1, isFavorite = true)
        val b = CategoryItem(id = 20, category = origin, name = "B", isActive = false, sortOrder = 2, isFavorite = true)
        val c = CategoryItem(id = 30, category = origin, name = "C", sortOrder = 3)
        originItems.addAll(listOf(a, b, c))
        every { repository.findById(20) } returns Optional.of(b)
        every { groupRepository.findByIdForUpdate(1) } returns origin
        every { groupRepository.findByIdForUpdate(2) } returns target
        every { entityManager.refresh(b) } just Runs
        every { repository.findMaxSortOrder(2) } returns 2
        every { repository.deferOrderConstraint() } just Runs

        // Act
        val response = service.update(20, UpdateCategoryItemRequest(categoryId = 2))

        // Assert
        assertEquals(1, a.sortOrder)
        assertEquals(2, c.sortOrder)
        assertSame(origin, a.category)
        assertSame(origin, c.category)
        assertSame(target, b.category)
        assertEquals(3, b.sortOrder)
        assertEquals(CategoryItemResponse(10, 1, "A", false, 1, true), CategoryItemResponse.from(a))
        assertEquals(CategoryItemResponse(20, 2, "B", false, 3, true), CategoryItemResponse.from(b))
        assertEquals(CategoryItemResponse(30, 1, "C", true, 2, false), CategoryItemResponse.from(c))
        assertEquals(CategoryItemResponse.from(b), response)
        verify(exactly = 1) { repository.deferOrderConstraint() }
    }

    @Test
    fun `잠금 대기 중 소속이 바뀌면 기존 소속을 기준으로 수정을 진행하지 않는다`() {
        // Arrange
        val origin = Category(id = 1, type = CategoryType.EXPENSE, name = "출발지", sortOrder = 1)
        val target = Category(id = 2, type = CategoryType.EXPENSE, name = "목적지", sortOrder = 2)
        val item = spyk(CategoryItem(id = 10, category = origin, name = "식자재", isActive = false, sortOrder = 2))
        every { repository.findById(10) } returns Optional.of(item)
        every { groupRepository.findByIdForUpdate(1) } returns origin
        // 실제 DB 경합이 아닌 refresh 이후의 상태 전환을 모형화한다.
        every { entityManager.refresh(item) } answers { item.moveTo(target) }

        // Act
        val exception = assertThrows(GrimaultException::class.java) {
            service.update(10, UpdateCategoryItemRequest(name = "변경명", isFavorite = true))
        }

        // Assert
        assertEquals(ErrorCode.CATEGORY_ITEM_UPDATE_CONFLICT, exception.errorCode)
        assertEquals(10, item.id)
        assertSame(target, item.category)
        assertEquals("식자재", item.name)
        assertFalse(item.isActive)
        assertFalse(item.isFavorite)
        assertEquals(2, item.sortOrder)
        verifySequence {
            repository.findById(10)
            groupRepository.findByIdForUpdate(1)
            entityManager.refresh(item)
        }
        verify(exactly = 0) { item.rename(any()) }
        verify(exactly = 0) { item.favorite(any()) }
        verify(exactly = 0) { item.reorder(any()) }
        // 상태 전환 모형에서 수행한 한 번 외에는 이동하지 않는다.
        verify(exactly = 1) { item.moveTo(target) }
        verify(exactly = 0) { repository.findMaxSortOrder(any()) }
        verify(exactly = 0) { repository.deferOrderConstraint() }
    }


    @Test
    fun `존재하지 않는 소분류는 수정할 수 없다`() {
        // Arrange
        val body = UpdateCategoryItemRequest(name = "변경명")
        every { repository.findById(999) } returns Optional.empty()

        // Act
        val exception = assertThrows(GrimaultException::class.java) {
            service.update(999, body)
        }

        // Assert
        assertEquals(ErrorCode.CATEGORY_ITEM_NOT_FOUND, exception.errorCode)
        verifySequence { repository.findById(999) }
        verify { groupRepository wasNot Called }
        verify { entityManager wasNot Called }
        verify(exactly = 0) { repository.findMaxSortOrder(any()) }
        verify(exactly = 0) { repository.deferOrderConstraint() }
    }


    @Test
    fun `존재하지 않는 대분류로 이동할 수 없다`() {
        // Arrange
        val originItems = mutableListOf<CategoryItem>()
        val origin = Category(id = 1, type = CategoryType.EXPENSE, name = "출발지", sortOrder = 1, items = originItems)
        val item = spyk(CategoryItem(id = 10, category = origin, name = "식자재", isActive = false, sortOrder = 2, isFavorite = true))
        val following = spyk(CategoryItem(id = 11, category = origin, name = "다음", sortOrder = 3))
        originItems.addAll(listOf(item, following))
        val body = UpdateCategoryItemRequest(categoryId = 999)
        every { repository.findById(10) } returns Optional.of(item)
        every { groupRepository.findByIdForUpdate(1) } returns origin
        every { groupRepository.findByIdForUpdate(999) } returns null

        // Act
        val exception = assertThrows(GrimaultException::class.java) {
            service.update(10, body)
        }

        // Assert
        assertEquals(ErrorCode.CATEGORY_NOT_FOUND, exception.errorCode)
        assertEquals(10, item.id)
        assertSame(origin, item.category)
        assertEquals("식자재", item.name)
        assertFalse(item.isActive)
        assertTrue(item.isFavorite)
        assertEquals(2, item.sortOrder)
        assertEquals(3, following.sortOrder)
        verifySequence {
            repository.findById(10)
            groupRepository.findByIdForUpdate(1)
            groupRepository.findByIdForUpdate(999)
        }
        verify { entityManager wasNot Called }
        verify(exactly = 0) { repository.findMaxSortOrder(any()) }
        verify(exactly = 0) { repository.deferOrderConstraint() }
        verify(exactly = 0) { item.moveTo(any()) }
        verify(exactly = 0) { item.reorder(any()) }
        verify(exactly = 0) { following.reorder(any()) }
    }


    @Test
    fun `현재 소속과 동일한 대분류로 이동할 수 없다`() {
        // Arrange
        val originItems = mutableListOf<CategoryItem>()
        val origin = Category(id = 1, type = CategoryType.EXPENSE, name = "출발지", sortOrder = 1, items = originItems)
        val item = spyk(CategoryItem(id = 10, category = origin, name = "식자재", isActive = false, sortOrder = 2, isFavorite = true))
        val following = spyk(CategoryItem(id = 11, category = origin, name = "다음", sortOrder = 3))
        originItems.addAll(listOf(item, following))
        val body = UpdateCategoryItemRequest(categoryId = 1)
        every { repository.findById(10) } returns Optional.of(item)
        every { groupRepository.findByIdForUpdate(1) } returns origin
        every { entityManager.refresh(item) } just Runs

        // Act
        val exception = assertThrows(GrimaultException::class.java) {
            service.update(10, body)
        }

        // Assert
        assertEquals(ErrorCode.INVALID_CATEGORY_ITEM_MOVE, exception.errorCode)
        assertEquals(10, item.id)
        assertSame(origin, item.category)
        assertEquals("식자재", item.name)
        assertFalse(item.isActive)
        assertTrue(item.isFavorite)
        assertEquals(2, item.sortOrder)
        assertEquals(3, following.sortOrder)
        verifySequence {
            repository.findById(10)
            groupRepository.findByIdForUpdate(1)
            entityManager.refresh(item)
        }
        verify(exactly = 0) { repository.findMaxSortOrder(any()) }
        verify(exactly = 0) { repository.deferOrderConstraint() }
        verify(exactly = 0) { item.moveTo(any()) }
        verify(exactly = 0) { item.reorder(any()) }
        verify(exactly = 0) { following.reorder(any()) }
    }


    @Test
    fun `유형이 다른 대분류 사이에서는 이동할 수 없다`() {
        // Arrange
        val originItems = mutableListOf<CategoryItem>()
        val origin = Category(id = 1, type = CategoryType.EXPENSE, name = "출발지", sortOrder = 1, items = originItems)
        val target = Category(id = 2, type = CategoryType.INCOME, name = "수입", sortOrder = 2)
        val item = spyk(CategoryItem(id = 10, category = origin, name = "식자재", isActive = false, sortOrder = 2, isFavorite = true))
        val following = spyk(CategoryItem(id = 11, category = origin, name = "다음", sortOrder = 3))
        originItems.addAll(listOf(item, following))
        val body = UpdateCategoryItemRequest(categoryId = 2)
        every { repository.findById(10) } returns Optional.of(item)
        every { groupRepository.findByIdForUpdate(1) } returns origin
        every { groupRepository.findByIdForUpdate(2) } returns target
        every { entityManager.refresh(item) } just Runs

        // Act
        val exception = assertThrows(GrimaultException::class.java) {
            service.update(10, body)
        }

        // Assert
        assertEquals(ErrorCode.INVALID_CATEGORY_TYPE, exception.errorCode)
        assertEquals(10, item.id)
        assertSame(origin, item.category)
        assertEquals("식자재", item.name)
        assertFalse(item.isActive)
        assertTrue(item.isFavorite)
        assertEquals(2, item.sortOrder)
        assertEquals(3, following.sortOrder)
        verifySequence {
            repository.findById(10)
            groupRepository.findByIdForUpdate(1)
            groupRepository.findByIdForUpdate(2)
            entityManager.refresh(item)
        }
        verify(exactly = 0) { repository.findMaxSortOrder(any()) }
        verify(exactly = 0) { repository.deferOrderConstraint() }
        verify(exactly = 0) { item.moveTo(any()) }
        verify(exactly = 0) { item.reorder(any()) }
        verify(exactly = 0) { following.reorder(any()) }
    }

    companion object {
        @JvmStatic
        fun compactCases(): List<Arguments> {
            val pairs = listOf(10 to 1, 20 to 2, 30 to 3, 40 to 4)
            return listOf(
                Arguments.of(pairs, 10, mapOf(20 to 1, 30 to 2, 40 to 3)),
                Arguments.of(pairs, 20, mapOf(10 to 1, 30 to 2, 40 to 3)),
                Arguments.of(pairs, 40, mapOf(10 to 1, 20 to 2, 30 to 3)),
                Arguments.of(listOf(10 to 1), 10, emptyMap<Int, Int>()),
            )
        }
    }

}
