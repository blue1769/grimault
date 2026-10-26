package com.blustar.grimault.domain.account.validation

import com.blustar.grimault.domain.account.dto.CreateAccountRequest
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import jakarta.validation.ConstraintValidatorContext
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test

class CreateAccountRequestValidatorTest {

    private val validator = CreateAccountRequestValidator()

    @Test
    fun `계정 대분류와 소분류의 조합이 유효하지 않으면 거부한다`() {
        // Arrange
        val request = CreateAccountRequest(
            name = "테스트 계정",
            type = "ASSET",
            subtype = "LOAN",
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
