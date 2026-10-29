package com.blustar.grimault.domain.account.validation

import com.blustar.grimault.domain.account.dto.UpdateAccountRequest
import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext

class UpdateAccountRequestValidator :
    ConstraintValidator<ValidUpdateAccountRequest, UpdateAccountRequest> {

    override fun isValid(
        body: UpdateAccountRequest?,
        context: ConstraintValidatorContext,
    ): Boolean {
        if (body == null) {
            return true
        }

        if (body.name != null && body.name.isBlank()) {
            return invalid(context, "Account name must not be blank")
        }

        if (body.hasNoUpdateFields) {
            return invalid(context, "No fields to update")
        }

        if (!hasValidBillingPeriod(body)) {
            return invalid(context, "Incomplete billing period")
        }

        return true
    }

    @Suppress("RedundantIf")
    private fun hasValidBillingPeriod(body: UpdateAccountRequest): Boolean {
        if (body.hasNoBillingPeriod) {
            return true
        }

        if (body.hasCompleteBillingPeriod) {
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
