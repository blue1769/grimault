package com.blustar.grimault.domain.category.service

import com.blustar.grimault.domain.category.repository.CategoryRepository
import com.blustar.grimault.domain.category.repository.CategoryTreeQueryRepository
import com.blustar.grimault.global.error.ErrorCode
import com.blustar.grimault.global.error.GrimaultException
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class CategoryServiceTest {

    private val repository = mockk<CategoryRepository>()
    private val queryRepository = mockk<CategoryTreeQueryRepository>()
    private val service = CategoryService(repository, queryRepository)

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
