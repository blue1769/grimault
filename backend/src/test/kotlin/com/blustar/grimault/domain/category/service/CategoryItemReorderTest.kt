package com.blustar.grimault.domain.category.service

import com.blustar.grimault.domain.category.dto.CategoryItemResponse
import com.blustar.grimault.domain.category.dto.ReorderCategoryEntryRequest
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
import java.util.Optional

class CategoryItemReorderTest {

    private val repository = mockk<CategoryItemRepository>()
    private val groupRepository = mockk<CategoryRepository>()
    private val entityManager = mockk<EntityManager>()
    private val service = CategoryItemService(repository, groupRepository, entityManager)

    @Test
    fun `앞에서 뒤로 이동하면 Forward 재정렬을 수행한다`() {
        // Arrange
        val category = Category(id = 1, type = CategoryType.EXPENSE, name = "식비", sortOrder = 1)
        val item = CategoryItem(id = 10, category = category, name = "식자재", sortOrder = 2)
        val reordered = CategoryItem(id = 10, category = category, name = "식자재", sortOrder = 4)
        val body = ReorderCategoryEntryRequest(sortOrder = 4)
        every { repository.findById(10) } returnsMany listOf(Optional.of(item), Optional.of(reordered))
        every { groupRepository.findByIdForUpdate(1) } returns category
        every { entityManager.refresh(item) } just Runs
        every { repository.findMaxSortOrder(1) } returns 5
        every { repository.deferOrderConstraint() } just Runs
        every { entityManager.flush() } just Runs
        every { repository.reorderForward(10, 1, 2, 4) } returns 3
        every { entityManager.clear() } just Runs

        // Act
        val response = service.reorder(id = 10, body = body)

        // Assert
        assertEquals(4, response.sortOrder)
        verifySequence {
            repository.findById(10)
            groupRepository.findByIdForUpdate(1)
            entityManager.refresh(item)
            repository.findMaxSortOrder(1)
            repository.deferOrderConstraint()
            entityManager.flush()
            repository.reorderForward(10, 1, 2, 4)
            entityManager.clear()
            repository.findById(10)
        }
        verify(exactly = 0) { repository.reorderBackward(any(), any(), any(), any()) }
    }

    @Test
    fun `뒤에서 앞으로 이동하면 Backward 재정렬을 수행한다`() {
        // Arrange
        val category = Category(id = 1, type = CategoryType.EXPENSE, name = "식비", sortOrder = 1)
        val item = CategoryItem(id = 10, category = category, name = "식자재", sortOrder = 4)
        val reordered = CategoryItem(id = 10, category = category, name = "식자재", sortOrder = 2)
        val body = ReorderCategoryEntryRequest(sortOrder = 2)
        every { repository.findById(10) } returnsMany listOf(Optional.of(item), Optional.of(reordered))
        every { groupRepository.findByIdForUpdate(1) } returns category
        every { entityManager.refresh(item) } just Runs
        every { repository.findMaxSortOrder(1) } returns 5
        every { repository.deferOrderConstraint() } just Runs
        every { entityManager.flush() } just Runs
        every { repository.reorderBackward(10, 1, 4, 2) } returns 3
        every { entityManager.clear() } just Runs

        // Act
        val response = service.reorder(id = 10, body = body)

        // Assert
        assertEquals(2, response.sortOrder)
        verifySequence {
            repository.findById(10)
            groupRepository.findByIdForUpdate(1)
            entityManager.refresh(item)
            repository.findMaxSortOrder(1)
            repository.deferOrderConstraint()
            entityManager.flush()
            repository.reorderBackward(10, 1, 4, 2)
            entityManager.clear()
            repository.findById(10)
        }
        verify(exactly = 0) { repository.reorderForward(any(), any(), any(), any()) }
    }

    @Test
    fun `동일 순번 요청은 변경 없이 정상 처리한다`() {
        // Arrange
        val category = Category(id = 1, type = CategoryType.EXPENSE, name = "식비", sortOrder = 1)
        val item = CategoryItem(
            id = 10, category = category, name = "식자재",
            isActive = false, sortOrder = 3, isFavorite = true,
        )
        val body = ReorderCategoryEntryRequest(sortOrder = 3)
        every { repository.findById(10) } returns Optional.of(item)
        every { groupRepository.findByIdForUpdate(1) } returns category
        every { entityManager.refresh(item) } just Runs
        every { repository.findMaxSortOrder(1) } returns 5

        // Act
        val response = service.reorder(id = 10, body = body)

        // Assert
        assertEquals(CategoryItemResponse(10, 1, "식자재", false, 3, true), response)
        assertEquals(CategoryItemResponse.from(item), response)
        verifySequence {
            repository.findById(10)
            groupRepository.findByIdForUpdate(1)
            entityManager.refresh(item)
            repository.findMaxSortOrder(1)
        }
        verify(exactly = 0) {
            repository.deferOrderConstraint()
            repository.reorderForward(any(), any(), any(), any())
            repository.reorderBackward(any(), any(), any(), any())
            entityManager.flush()
            entityManager.clear()
        }
    }

    @Test
    fun `목표 순번이 최대 순번을 초과하면 예외를 발생시킨다`() {
        // Arrange
        val category = Category(id = 1, type = CategoryType.EXPENSE, name = "식비", sortOrder = 1)
        val item = CategoryItem(id = 10, category = category, name = "식자재", sortOrder = 2)
        val body = ReorderCategoryEntryRequest(sortOrder = 6)
        every { repository.findById(10) } returns Optional.of(item)
        every { groupRepository.findByIdForUpdate(1) } returns category
        every { entityManager.refresh(item) } just Runs
        every { repository.findMaxSortOrder(1) } returns 5

        // Act
        val exception = assertThrows(GrimaultException::class.java) {
            service.reorder(id = 10, body = body)
        }

        // Assert
        assertEquals(ErrorCode.INVALID_CATEGORY_ORDER, exception.errorCode)
        verifySequence {
            repository.findById(10)
            groupRepository.findByIdForUpdate(1)
            entityManager.refresh(item)
            repository.findMaxSortOrder(1)
        }
        verify(exactly = 0) {
            repository.deferOrderConstraint()
            repository.reorderForward(any(), any(), any(), any())
            repository.reorderBackward(any(), any(), any(), any())
            entityManager.flush()
            entityManager.clear()
        }
    }

    @Test
    fun `존재하지 않는 소분류의 순번은 변경할 수 없다`() {
        // Arrange
        val body = ReorderCategoryEntryRequest(sortOrder = 2)
        every { repository.findById(999) } returns Optional.empty()

        // Act
        val exception = assertThrows(GrimaultException::class.java) {
            service.reorder(id = 999, body = body)
        }

        // Assert
        assertEquals(ErrorCode.CATEGORY_ITEM_NOT_FOUND, exception.errorCode)
        verifySequence { repository.findById(999) }
        verify(exactly = 0) {
            groupRepository.findByIdForUpdate(any())
            repository.findMaxSortOrder(any())
            repository.deferOrderConstraint()
            repository.reorderForward(any(), any(), any(), any())
            repository.reorderBackward(any(), any(), any(), any())
            entityManager.refresh(any())
            entityManager.flush()
            entityManager.clear()
        }
    }

    @Test
    fun `잠금 획득 중 소속 대분류가 변경되면 충돌로 처리한다`() {
        // Arrange
        val origin = Category(id = 1, type = CategoryType.EXPENSE, name = "출발지", sortOrder = 1)
        val target = Category(id = 2, type = CategoryType.EXPENSE, name = "목적지", sortOrder = 2)
        val item = CategoryItem(id = 10, category = origin, name = "식자재", sortOrder = 2)
        val body = ReorderCategoryEntryRequest(sortOrder = 3)
        every { repository.findById(10) } returns Optional.of(item)
        every { groupRepository.findByIdForUpdate(1) } returns origin
        // 실제 DB 경합이 아닌 refresh 이후의 소속 변경 상태를 모형화한다.
        every { entityManager.refresh(item) } answers { item.moveTo(target) }

        // Act
        val exception = assertThrows(GrimaultException::class.java) {
            service.reorder(id = 10, body = body)
        }

        // Assert
        assertSame(target, item.category)
        assertEquals(ErrorCode.CATEGORY_ITEM_UPDATE_CONFLICT, exception.errorCode)
        verifySequence {
            repository.findById(10)
            groupRepository.findByIdForUpdate(1)
            entityManager.refresh(item)
        }
        verify(exactly = 0) {
            repository.findMaxSortOrder(any())
            repository.deferOrderConstraint()
            repository.reorderForward(any(), any(), any(), any())
            repository.reorderBackward(any(), any(), any(), any())
            entityManager.flush()
            entityManager.clear()
        }
    }
}
