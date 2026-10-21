package com.blustar.grimault.domain.account.validation

import com.blustar.grimault.domain.account.dto.UpdateAccountRequest
import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext

class UpdateAccountRequestValidator :
    ConstraintValidator<ValidUpdateAccountRequest, UpdateAccountRequest> {

    override fun isValid(
        request: UpdateAccountRequest?,
        context: ConstraintValidatorContext,
    ): Boolean {
        if (request == null) {
            return true
        }

        if (!hasUpdateFields(request)) {
            return invalid(context, "No fields to update")
        }

        if (!hasValidBillingPeriod(request)) {
            return invalid(context, "Incomplete billing period")
        }

        return true
    }

    @Suppress("RedundantIf")
    private fun hasUpdateFields(request: UpdateAccountRequest): Boolean {
        val hasNoUpdateFields = request.name == null &&
                request.settlementDay == null &&
                request.startMonthOffset == null &&
                request.startDay == null &&
                request.endMonthOffset == null &&
                request.endDay == null

        if (hasNoUpdateFields) {
            return false
        }

        return true
    }

    @Suppress("RedundantIf")
    private fun hasValidBillingPeriod(request: UpdateAccountRequest): Boolean {
        val hasNoBillingPeriod = request.startMonthOffset == null &&
                request.startDay == null &&
                request.endMonthOffset == null &&
                request.endDay == null

        val hasCompleteBillingPeriod = request.startMonthOffset != null &&
                request.startDay != null &&
                request.endMonthOffset != null &&
                request.endDay != null

        if (hasNoBillingPeriod) {
            return true
        }

        if (hasCompleteBillingPeriod) {
            return true
        }

        return false
    }

    private fun invalid(
        context: ConstraintValidatorContext,
        message: String,
    ): Boolean {
        context.disableDefaultConstraintViolation()
        context
            .buildConstraintViolationWithTemplate(message)
            .addConstraintViolation()

        return false
    }

}
