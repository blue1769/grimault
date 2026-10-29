package com.blustar.grimault.domain.category.service

import com.blustar.grimault.domain.category.dto.CreateCategoryRequest
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
import org.junit.jupiter.api.Assertions.assertEquals
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

    private val service = CategoryService(repository, queryRepository, orderLockRepository)

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
}
