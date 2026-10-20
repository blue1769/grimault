package com.blustar.grimault.domain.account.dto

import com.blustar.grimault.domain.account.entity.*
import com.blustar.grimault.domain.account.validation.ValidCreateAccountRequest
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

@Schema(description = "금융 계정 생성 요청")
@ValidCreateAccountRequest
data class CreateAccountRequest(
    @field:NotBlank
    @field:Size(max = 50)
    val name: String,

    @field:NotBlank
    val type: String,

    @field:NotBlank
    val subtype: String,

    val issuer: String? = null,

    @field:Min(1)
    @field:Max(31)
    val settlementDay: Int? = null,

    @field:Min(-2)
    @field:Max(0)
    val startMonthOffset: Int? = null,

    @field:Min(1)
    @field:Max(31)
    val startDay: Int? = null,

    @field:Min(-2)
    @field:Max(0)
    val endMonthOffset: Int? = null,

    @field:Min(1)
    @field:Max(31)
    val endDay: Int? = null,
) {

    fun toEntity(): Account = Account(
        name = name,
        type = requireNotNull(AccountType.fromNameOrNull(type)),
        subtype = requireNotNull(AccountSubtype.fromNameOrNull(subtype)),
        creditCardMetadata = creditCardMetadata(),
        isActive = true,
    )

    private fun creditCardMetadata(): CreditCardMetadata? {
        val subtype = requireNotNull(AccountSubtype.fromNameOrNull(subtype))

        if (subtype != AccountSubtype.CREDIT_CARD) {
            return null
        }

        val issuer = requireNotNull(CreditCardIssuer.fromNameOrNull(issuer))
        val settlementDay = requireNotNull(settlementDay)
        val startMonthOffset = requireNotNull(startMonthOffset)
        val startDay = requireNotNull(startDay)
        val endMonthOffset = requireNotNull(endMonthOffset)
        val endDay = requireNotNull(endDay)

        return CreditCardMetadata(
            issuer = issuer,
            settlementDay = settlementDay,
            billingPeriod = CreditCardBillingPeriod(
                startMonthOffset = startMonthOffset,
                startDay = startDay,
                endMonthOffset = endMonthOffset,
                endDay = endDay,
            )
        )
    }

}
