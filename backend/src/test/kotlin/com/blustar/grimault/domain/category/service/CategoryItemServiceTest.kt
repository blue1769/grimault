package com.blustar.grimault.domain.category.service

import com.blustar.grimault.domain.category.repository.CategoryItemRepository
import com.blustar.grimault.domain.category.repository.CategoryRepository
import com.blustar.grimault.global.error.ErrorCode
import com.blustar.grimault.global.error.GrimaultException
import io.mockk.every
import io.mockk.mockk
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
}
