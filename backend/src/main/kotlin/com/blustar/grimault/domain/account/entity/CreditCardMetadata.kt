package com.blustar.grimault.domain.account.entity

data class CreditCardMetadata(
    val issuer: CreditCardIssuer,
    val billingPeriod: CreditCardBillingPeriod,
    val settlementDay: Int,
)

data class CreditCardBillingPeriod(
    val startMonthOffset: Int,
    val startDay: Int,
    val endMonthOffset: Int,
    val endDay: Int,
)
