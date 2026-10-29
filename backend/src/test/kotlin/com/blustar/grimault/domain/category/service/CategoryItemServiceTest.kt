package com.blustar.grimault.domain.category.service

import com.blustar.grimault.domain.category.dto.CreateCategoryItemRequest
import com.blustar.grimault.domain.category.entity.Category
import com.blustar.grimault.domain.category.entity.CategoryItem
import com.blustar.grimault.domain.category.entity.CategoryType
import com.blustar.grimault.domain.category.repository.CategoryItemRepository
import com.blustar.grimault.domain.category.repository.CategoryRepository
import com.blustar.grimault.global.error.ErrorCode
import com.blustar.grimault.global.error.GrimaultException
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import io.mockk.verifySequence
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import java.util.*

class CategoryItemServiceTest {

    private val repository = mockk<CategoryItemRepository>()
    private val groupRepository = mockk<CategoryRepository>()

    private val service = CategoryItemService(repository, groupRepository)

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

}
