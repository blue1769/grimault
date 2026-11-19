package com.blustar.grimault.domain.ledger.dto

import com.blustar.grimault.domain.ledger.entity.TransactionType
import io.swagger.v3.oas.annotations.media.Schema
import java.math.BigDecimal
import java.time.LocalDate

@Schema(description = "금융 거래 응답")
data class TransactionResponse(
    val id: Long,
    val type: TransactionType,
    val date: LocalDate,
    val amount: BigDecimal,
    val merchant: String?,
    val description: String,
    val accountId: Int?,
    val accountName: String?,
    val categoryItemId: Int?,
    val categoryLabel: String?,
    val transferMetadata: TransferMetadata?,
    val paymentMethod: String?,
    val tags: String?,
    val isWaste: Boolean,
)

@Schema(description = "이체 거래 계정 정보")
data class TransferMetadata(
    val sourceId: Int,
    val sourceName: String,
    val targetId: Int,
    val targetName: String,
)
