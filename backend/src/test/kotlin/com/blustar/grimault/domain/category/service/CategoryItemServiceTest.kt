package com.blustar.grimault.domain.category.service

import com.blustar.grimault.domain.category.dto.CreateCategoryItemRequest
import com.blustar.grimault.domain.category.repository.CategoryItemRepository
import com.blustar.grimault.domain.category.repository.CategoryRepository
import com.blustar.grimault.global.error.ErrorCode
import com.blustar.grimault.global.error.GrimaultException
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.util.*

class CategoryItemServiceTest {

    private val repository = mockk<CategoryItemRepository>()
    private val groupRepository = mockk<CategoryRepository>()

    private val service = CategoryItemService(repository, groupRepository)

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
