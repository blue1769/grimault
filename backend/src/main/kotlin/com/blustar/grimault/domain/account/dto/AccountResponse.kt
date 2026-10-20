package com.blustar.grimault.domain.account.dto

import com.blustar.grimault.domain.account.entity.Account
import com.blustar.grimault.domain.account.entity.AccountSubtype
import com.blustar.grimault.domain.account.entity.AccountType
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "금융 계정 응답")
data class AccountResponse(
    val id: Int,
    val name: String,

    @Schema(description = "회계 대분류 (ASSET: 자산, LIABILITY: 부채, EQUITY: 자본)")
    val type: AccountType,

    @Schema(description = "계정 세부 분류 (BANK: 은행, CASH: 현금, PREPAID: 선불/페이머니, CREDIT_CARD: 신용카드, LOAN: 대출, CAPITAL: 기초자본)")
    val subtype: AccountSubtype,

    @Schema(description = "신용카드 회사 이름", example = "비씨")
    val issuer: String?,

    @Schema(description = "신용카드 이용 기간", example = "전월 7일 ~ 당월 6일")
    val billingPeriodLabel: String?,

    @Schema(description = "신용카드 대금 결제일 (1-31, 신용카드 전용)", example = "20")
    val settlementDay: Int?,

    @get:Schema(description = "계정 실사용 여부")
    @get:JsonProperty("isActive")
    val isActive: Boolean,
) {
    companion object {
        fun from(account: Account): AccountResponse {
            val verifiedId = requireNotNull(account.id) {
                "Account id must not be null"
            }

            return AccountResponse(
                id = verifiedId,
                name = account.name,
                type = account.type,
                subtype = account.subtype,
                issuer = account.creditCardMetadata?.issuer?.displayName,
                billingPeriodLabel = account.creditCardMetadata?.billingPeriod?.toDisplayLabel(),
                settlementDay = account.creditCardMetadata?.settlementDay,
                isActive = account.isActive
            )
        }
    }
}
