package com.blustar.grimault.domain.account.validation

import com.blustar.grimault.domain.account.dto.CreateAccountRequest
import com.blustar.grimault.domain.account.entity.AccountSubtype
import com.blustar.grimault.domain.account.entity.AccountSubtype.*
import com.blustar.grimault.domain.account.entity.AccountType
import com.blustar.grimault.domain.account.entity.AccountType.*
import com.blustar.grimault.domain.account.entity.CreditCardIssuer
import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext

class CreateAccountRequestValidator :
    ConstraintValidator<ValidCreateAccountRequest, CreateAccountRequest> {

    @Suppress("RedundantIf")
    override fun isValid(
        body: CreateAccountRequest?,
        context: ConstraintValidatorContext,
    ): Boolean {
        if (body == null) {
            return true
        }

        if (body.name.isBlank()) {
            return invalid(context, "Account name must not be blank")
        }

        val type = AccountType.fromNameOrNull(body.type.trim())
            ?: return invalid(context, "Invalid account kind")

        val subtype = AccountSubtype.fromNameOrNull(body.subtype.trim())
            ?: return invalid(context, "Invalid account kind")

        if (!validateTypeDependencies(type, subtype)) {
            return invalid(context, "Invalid account kind")
        }

        if (subtype == CREDIT_CARD) {
            return validateCreditCardMetadata(body, context)
        }

        val hasAnyCreditCardMetadata: Boolean = body.issuer != null ||
                body.settlementDay != null ||
                body.startMonthOffset != null ||
                body.startDay != null ||
                body.endMonthOffset != null ||
                body.endDay != null

        if (hasAnyCreditCardMetadata) {
            return invalid(context, "Unexpected credit card metadata")
        }

        return true
    }

    private fun validateTypeDependencies(
        type: AccountType,
        subtype: AccountSubtype,
    ): Boolean {
        return when (type) {
            ASSET -> subtype in setOf(BANK, CASH, PREPAID)
            LIABILITY -> subtype in setOf(CREDIT_CARD, LOAN)
            EQUITY -> subtype in setOf(CAPITAL)
        }
    }

    @Suppress("RedundantIf")
    private fun validateCreditCardMetadata(
        body: CreateAccountRequest,
        context: ConstraintValidatorContext,
    ): Boolean {
        operator fun Pair<Int, Int>.compareTo(end: Pair<Int, Int>): Int {
            if (first != end.first) {
                return first.compareTo(end.first)
            }

            return second.compareTo(end.second)
        }

        val hasMissingRequiredFields = body.issuer.isNullOrBlank() ||
                body.settlementDay == null ||
                body.startMonthOffset == null ||
                body.startDay == null ||
                body.endMonthOffset == null ||
                body.endDay == null

        if (hasMissingRequiredFields) {
            return invalid(context, "Missing mandatory credit card metadata")
        }

        CreditCardIssuer.fromNameOrNull(body.issuer.trim())
            ?: return invalid(context, "Invalid credit card issuer")

        val start = body.startMonthOffset to body.startDay
        val end = body.endMonthOffset to body.endDay

        if (start >= end) {
            return invalid(context, "Invalid credit card billing period")
        }

        return true
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
