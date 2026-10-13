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
) {

    fun toDisplayLabel(): String =
        "${deriveOffsetString(startMonthOffset)} ${startDay}일 ~ ${deriveOffsetString(endMonthOffset)} ${endDay}일"

    private fun deriveOffsetString(offset: Int): String =
        when (offset) {
            -2 -> "전전월"
            -1 -> "전월"
            0 -> "당월"
            else -> error("Unsupported month offset: $offset")
        }

}
