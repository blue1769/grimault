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

        if (request.name != null && request.name.isBlank()) {
            return invalid(context, "Account name must not be blank")
        }

        if (request.hasNoUpdateFields) {
            return invalid(context, "No fields to update")
        }

        if (!hasValidBillingPeriod(request)) {
            return invalid(context, "Incomplete billing period")
        }

        return true
    }

    @Suppress("RedundantIf")
    private fun hasValidBillingPeriod(request: UpdateAccountRequest): Boolean {
        if (request.hasNoBillingPeriod) {
            return true
        }

        if (request.hasCompleteBillingPeriod) {
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
