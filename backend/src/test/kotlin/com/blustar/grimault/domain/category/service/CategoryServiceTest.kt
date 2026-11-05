package com.blustar.grimault.domain.category.service

import com.blustar.grimault.domain.category.dto.CategoryResponse
import com.blustar.grimault.domain.category.dto.ReorderCategoryEntryRequest
import com.blustar.grimault.domain.category.dto.CreateCategoryRequest
import com.blustar.grimault.domain.category.dto.UpdateCategoryRequest
import com.blustar.grimault.domain.category.entity.Category
import com.blustar.grimault.domain.category.entity.CategoryType
import com.blustar.grimault.domain.category.repository.CategoryOrderLockRepository
import com.blustar.grimault.domain.category.repository.CategoryRepository
import com.blustar.grimault.domain.category.repository.CategoryTreeQueryRepository
import com.blustar.grimault.global.error.ErrorCode
import com.blustar.grimault.global.error.GrimaultException
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.Runs
import io.mockk.slot
import io.mockk.verifySequence
import io.mockk.verify
import jakarta.persistence.EntityManager
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import java.util.*

class CategoryServiceTest {

    private val repository = mockk<CategoryRepository>()
    private val queryRepository = mockk<CategoryTreeQueryRepository>()
    private val orderLockRepository = mockk<CategoryOrderLockRepository>()
    private val entityManager = mockk<EntityManager>()

    private val service = CategoryService(repository, queryRepository, orderLockRepository, entityManager)

    @ParameterizedTest
    @CsvSource("null, 1", "1, 2", "5, 6", nullValues = ["null"])
    fun `신규 대분류는 전체 정렬 범위의 마지막 순번에 추가된다`(
        maxSortOrder: Int?,
        expectedSortOrder: Int,
    ) {
        // Arrange
        val body = CreateCategoryRequest(name = "테스트 대분류", type = "EXPENSE")
        val savedId = 10
        val categorySlot = slot<Category>()
        every { orderLockRepository.lock() } just Runs
        every { repository.findMaxSortOrder() } returns maxSortOrder
        every { repository.save(capture(categorySlot)) } answers {
            val category = firstArg<Category>()
            Category(
                id = savedId,
                type = category.type,
                name = category.name,
                isActive = category.isActive,
                sortOrder = category.sortOrder,
            )
        }

        // Act
        val response = service.create(body)

        // Assert
        val savedCategory = categorySlot.captured
        assertEquals(expectedSortOrder, savedCategory.sortOrder)
        assertTrue(savedCategory.isActive)
        assertEquals(savedId, response.id)
        assertEquals(CategoryType.EXPENSE, response.type)
        assertEquals(body.name, response.name)
        assertTrue(response.isActive)
        assertEquals(expectedSortOrder, response.sortOrder)
        verifySequence {
            orderLockRepository.lock()
            repository.findMaxSortOrder()
            repository.save(any())
        }
    }

    @Test
    fun `대분류 이름을 변경해도 다른 속성은 유지된다`() {
        // Arrange
        val id = 10
        val category = Category(
            id = id,
            type = CategoryType.EXPENSE,
            name = "식비",
            isActive = false,
            sortOrder = 3,
        )
        val body = UpdateCategoryRequest(name = "외식비")
        every { orderLockRepository.lock() } just Runs
        every { repository.findById(id) } returns Optional.of(category)

        // Act
        val response = service.update(id = id, body = body)

        // Assert
        assertEquals("외식비", category.name)
        assertEquals(10, category.id)
        assertEquals(CategoryType.EXPENSE, category.type)
        assertFalse(category.isActive)
        assertEquals(3, category.sortOrder)
        assertEquals(category.id, response.id)
        assertEquals(category.type, response.type)
        assertEquals(category.name, response.name)
        assertEquals(category.isActive, response.isActive)
        assertEquals(category.sortOrder, response.sortOrder)
    }

    @Test
    fun `존재하지 않는 대분류의 이름은 수정할 수 없다`() {
        // Arrange
        val id = 999
        val body = UpdateCategoryRequest(name = "외식비")
        every { orderLockRepository.lock() } just Runs
        every { repository.findById(id) } returns Optional.empty()

        // Act
        val exception = assertThrows(GrimaultException::class.java) {
            service.update(id = id, body = body)
        }

        // Assert
        assertEquals(ErrorCode.CATEGORY_NOT_FOUND, exception.errorCode)
    }

    @Test
    fun `존재하지 않는 대분류는 조회할 수 없다`() {
        // Arrange
        val id = 999
        every { repository.findById(id) } returns Optional.empty()

        // Act
        val exception = assertThrows(GrimaultException::class.java) {
            service.findById(id = id)
        }

        // Assert
        assertEquals(ErrorCode.CATEGORY_NOT_FOUND, exception.errorCode)
    }

    @Test
    fun `유효하지 않은 분류 유형은 조회할 수 없다`() {
        // Arrange
        val typeName = "INVALID_TYPE"

        // Act
        val exception = assertThrows(GrimaultException::class.java) {
            service.findAll(typeName = typeName)
        }

        // Assert
        assertEquals(ErrorCode.INVALID_CATEGORY_TYPE, exception.errorCode)
        verify(exactly = 0) { queryRepository.findAll(any(), any()) }
    }

    @Test
    fun `앞에서 뒤로 이동하면 Forward 재정렬을 수행한다`() {
        // Arrange
        val id = 10
        val category = Category(id = id, type = CategoryType.EXPENSE, name = "식비", sortOrder = 2)
        val reordered = Category(id = id, type = CategoryType.EXPENSE, name = "식비", sortOrder = 4)
        val body = ReorderCategoryEntryRequest(sortOrder = 4)
        every { orderLockRepository.lock() } just Runs
        every { repository.findMaxSortOrder() } returns 5
        every { repository.findById(id) } returnsMany listOf(Optional.of(category), Optional.of(reordered))
        every { repository.deferOrderConstraint() } just Runs
        every { entityManager.flush() } just Runs
        every { repository.reorderForward(id, 2, 4) } returns 3
        every { entityManager.clear() } just Runs

        // Act
        val response = service.reorder(id = id, body = body)

        // Assert
        assertEquals(4, response.sortOrder)
        verifySequence {
            orderLockRepository.lock()
            repository.findMaxSortOrder()
            repository.findById(id)
            repository.deferOrderConstraint()
            entityManager.flush()
            repository.reorderForward(id, 2, 4)
            entityManager.clear()
            repository.findById(id)
        }
        verify(exactly = 0) { repository.reorderBackward(any(), any(), any()) }
    }

    @Test
    fun `뒤에서 앞으로 이동하면 Backward 재정렬을 수행한다`() {
        // Arrange
        val id = 10
        val category = Category(id = id, type = CategoryType.EXPENSE, name = "식비", sortOrder = 4)
        val reordered = Category(id = id, type = CategoryType.EXPENSE, name = "식비", sortOrder = 2)
        val body = ReorderCategoryEntryRequest(sortOrder = 2)
        every { orderLockRepository.lock() } just Runs
        every { repository.findMaxSortOrder() } returns 5
        every { repository.findById(id) } returnsMany listOf(Optional.of(category), Optional.of(reordered))
        every { repository.deferOrderConstraint() } just Runs
        every { entityManager.flush() } just Runs
        every { repository.reorderBackward(id, 4, 2) } returns 3
        every { entityManager.clear() } just Runs

        // Act
        val response = service.reorder(id = id, body = body)

        // Assert
        assertEquals(2, response.sortOrder)
        verifySequence {
            orderLockRepository.lock()
            repository.findMaxSortOrder()
            repository.findById(id)
            repository.deferOrderConstraint()
            entityManager.flush()
            repository.reorderBackward(id, 4, 2)
            entityManager.clear()
            repository.findById(id)
        }
        verify(exactly = 0) { repository.reorderForward(any(), any(), any()) }
    }

    @Test
    fun `동일 순번 요청은 변경 없이 정상 처리한다`() {
        // Arrange
        val id = 10
        val category = Category(
            id = id,
            type = CategoryType.EXPENSE,
            name = "식비",
            isActive = false,
            sortOrder = 3,
        )
        val body = ReorderCategoryEntryRequest(sortOrder = 3)
        every { orderLockRepository.lock() } just Runs
        every { repository.findMaxSortOrder() } returns 5
        every { repository.findById(id) } returns Optional.of(category)

        // Act
        val response = service.reorder(id = id, body = body)

        // Assert
        assertEquals(CategoryResponse(id, CategoryType.EXPENSE, "식비", false, 3), response)
        verifySequence {
            orderLockRepository.lock()
            repository.findMaxSortOrder()
            repository.findById(id)
        }
        verify(exactly = 0) {
            repository.deferOrderConstraint()
            repository.reorderForward(any(), any(), any())
            repository.reorderBackward(any(), any(), any())
            entityManager.flush()
            entityManager.clear()
        }
    }

    @Test
    fun `목표 순번이 최대 순번을 초과하면 예외를 발생시킨다`() {
        // Arrange
        val id = 10
        val body = ReorderCategoryEntryRequest(sortOrder = 6)
        every { orderLockRepository.lock() } just Runs
        every { repository.findMaxSortOrder() } returns 5

        // Act
        val exception = assertThrows(GrimaultException::class.java) {
            service.reorder(id = id, body = body)
        }

        // Assert
        assertEquals(ErrorCode.INVALID_CATEGORY_ORDER, exception.errorCode)
        verifySequence {
            orderLockRepository.lock()
            repository.findMaxSortOrder()
        }
        verify(exactly = 0) {
            repository.findById(any())
            repository.deferOrderConstraint()
            repository.reorderForward(any(), any(), any())
            repository.reorderBackward(any(), any(), any())
            entityManager.flush()
            entityManager.clear()
        }
    }

    @Test
    fun `존재하지 않는 대분류의 순번은 변경할 수 없다`() {
        // Arrange
        val id = 999
        val body = ReorderCategoryEntryRequest(sortOrder = 2)
        every { orderLockRepository.lock() } just Runs
        every { repository.findMaxSortOrder() } returns 5
        every { repository.findById(id) } returns Optional.empty()

        // Act
        val exception = assertThrows(GrimaultException::class.java) {
            service.reorder(id = id, body = body)
        }

        // Assert
        assertEquals(ErrorCode.CATEGORY_NOT_FOUND, exception.errorCode)
        verifySequence {
            orderLockRepository.lock()
            repository.findMaxSortOrder()
            repository.findById(id)
        }
        verify(exactly = 0) {
            repository.deferOrderConstraint()
            repository.reorderForward(any(), any(), any())
            repository.reorderBackward(any(), any(), any())
            entityManager.flush()
            entityManager.clear()
        }
    }
}
