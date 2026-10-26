package com.blustar.grimault.domain.account.validation

import com.blustar.grimault.domain.account.dto.UpdateAccountRequest
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import jakarta.validation.ConstraintValidatorContext
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class UpdateAccountRequestValidatorTest {

    private val validator = UpdateAccountRequestValidator()

    @ParameterizedTest(name = "{index}: {0}")
    @CsvSource(
        "시작 월만 전달, -1, , , ",
        "시작 월·일만 전달, -1, 1, , ",
        "종료일만 누락, -1, 1, 0, ",
    )
    fun `청구 기간 일부 필드만 전달하면 거부한다`(
        caseName: String,
        startMonthOffset: Int?,
        startDay: Int?,
        endMonthOffset: Int?,
        endDay: Int?,
    ) {
        // Arrange
        val request = UpdateAccountRequest(
            startMonthOffset = startMonthOffset,
            startDay = startDay,
            endMonthOffset = endMonthOffset,
            endDay = endDay,
        )
        val context = mockk<ConstraintValidatorContext>()
        val violationBuilder = mockk<ConstraintValidatorContext.ConstraintViolationBuilder>()
        every { context.disableDefaultConstraintViolation() } just runs
        every { context.buildConstraintViolationWithTemplate(any()) } returns violationBuilder
        every { violationBuilder.addConstraintViolation() } returns context

        // Act
        val result = validator.isValid(request, context)

        // Assert
        assertFalse(result, caseName)
    }
}
