package com.blustar.grimault.domain.account.validation

import com.blustar.grimault.domain.account.dto.CreateAccountRequest
import com.blustar.grimault.domain.account.entity.AccountSubType
import com.blustar.grimault.domain.account.entity.AccountSubType.*
import com.blustar.grimault.domain.account.entity.AccountType
import com.blustar.grimault.domain.account.entity.AccountType.*
import com.blustar.grimault.domain.account.entity.CreditCardIssuer
import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext

class CreateAccountRequestValidator :
    ConstraintValidator<ValidCreateAccountRequest, CreateAccountRequest> {

    override fun isValid(
        request: CreateAccountRequest?,
        context: ConstraintValidatorContext,
    ): Boolean {
        if (request == null) {
            return true
        }

        val type = AccountType.fromNameOrNull(request.type.trim())
            ?: return invalid(context, "Invalid account kind")

        val subType = AccountSubType.fromNameOrNull(request.subtype.trim())
            ?: return invalid(context, "Invalid account kind")

        if (!validateTypeDependencies(type, subType)) {
            return invalid(context, "Invalid account kind")
        }

        if (subType == CREDIT_CARD) {
            return validateCreditCardMetadata(request, context)
        }

        return true
    }

    private fun validateTypeDependencies(
        type: AccountType,
        subType: AccountSubType,
    ): Boolean {
        return when (type) {
            ASSET -> subType in setOf(BANK, CASH, PREPAID)
            LIABILITY -> subType in setOf(CREDIT_CARD, LOAN)
            EQUITY -> subType in setOf(CAPITAL)
        }
    }

    @Suppress("RedundantIf")
    private fun validateCreditCardMetadata(
        request: CreateAccountRequest,
        context: ConstraintValidatorContext,
    ): Boolean {
        operator fun Pair<Int, Int>.compareTo(end: Pair<Int, Int>): Int {
            if (first != end.first) {
                return first.compareTo(end.first)
            }

            return second.compareTo(end.second)
        }

        val hasMissingRequiredFields = request.issuer.isNullOrBlank() ||
                request.settlementDay == null ||
                request.startMonthOffset == null ||
                request.startDay == null ||
                request.endMonthOffset == null ||
                request.endDay == null

        if (hasMissingRequiredFields) {
            return invalid(context, "Missing mandatory credit card metadata")
        }

        CreditCardIssuer.fromNameOrNull(request.issuer.trim())
            ?: return invalid(context, "Invalid credit card issuer")

        val start = request.startMonthOffset to request.startDay
        val end = request.endMonthOffset to request.endDay

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
