package com.blustar.grimault.domain.category.validation

import com.blustar.grimault.domain.category.dto.CreateCategoryRequest
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import jakarta.validation.ConstraintValidatorContext
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test

class CreateCategoryRequestValidatorTest {

    private val validator = CreateCategoryRequestValidator()

    @Test
    fun `유효하지 않은 분류 유형으로 대분류를 생성할 수 없다`() {
        // Arrange
        val request = CreateCategoryRequest(
            name = "테스트 대분류",
            type = "INVALID",
        )
        val context = mockk<ConstraintValidatorContext>()
        val violationBuilder = mockk<ConstraintValidatorContext.ConstraintViolationBuilder>()
        every { context.disableDefaultConstraintViolation() } just runs
        every { context.buildConstraintViolationWithTemplate(any()) } returns violationBuilder
        every { violationBuilder.addConstraintViolation() } returns context

        // Act
        val result = validator.isValid(request, context)

        // Assert
        assertFalse(result)
    }
}
