package com.blustar.grimault.domain.account.validation

import com.blustar.grimault.domain.account.dto.CreateAccountRequest
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import jakarta.validation.ConstraintValidatorContext
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class CreateAccountRequestValidatorTest {

    private val validator = CreateAccountRequestValidator()

    @ParameterizedTest(name = "{index}: {0} 누락")
    @ValueSource(strings = ["issuer", "settlementDay", "startMonthOffset", "startDay", "endMonthOffset", "endDay"])
    fun `신용카드 필수 메타정보가 하나라도 누락되면 거부한다`(missingField: String) {
        // Arrange
        val request = CreateAccountRequest(
            name = "테스트 신용카드",
            type = "LIABILITY",
            subtype = "CREDIT_CARD",
            issuer = if (missingField == "issuer") null else "HYUNDAI",
            settlementDay = if (missingField == "settlementDay") null else 15,
            startMonthOffset = if (missingField == "startMonthOffset") null else -1,
            startDay = if (missingField == "startDay") null else 1,
            endMonthOffset = if (missingField == "endMonthOffset") null else 0,
            endDay = if (missingField == "endDay") null else 15,
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

    @Test
    fun `비신용카드 계정에 신용카드 전용 메타정보가 포함되면 거부한다`() {
        // Arrange
        val request = CreateAccountRequest(
            name = "테스트 은행 계정",
            type = "ASSET",
            subtype = "BANK",
            issuer = "HYUNDAI",
            settlementDay = null,
            startMonthOffset = null,
            startDay = null,
            endMonthOffset = null,
            endDay = null,
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
