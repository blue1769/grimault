package com.blustar.grimault.domain.ledger.dto

import com.blustar.grimault.domain.ledger.entity.TransactionType
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import io.swagger.v3.oas.annotations.media.DiscriminatorMapping
import io.swagger.v3.oas.annotations.media.Schema
import java.math.BigDecimal
import java.time.LocalDate

@Schema(
    description = "금융 거래 생성 요청",
    oneOf = [
        SpendingRequest::class,
        IncomeReceiptRequest::class,
        TransferRequest::class,
        RefundRequest::class,
    ],
    discriminatorProperty = "type",
    discriminatorMapping = [
        DiscriminatorMapping(value = "SPENDING", schema = SpendingRequest::class),
        DiscriminatorMapping(value = "INCOME_RECEIPT", schema = IncomeReceiptRequest::class),
        DiscriminatorMapping(value = "TRANSFER", schema = TransferRequest::class),
        DiscriminatorMapping(value = "REFUND", schema = RefundRequest::class),
    ],
)
@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "type",
)
@JsonSubTypes(
    JsonSubTypes.Type(SpendingRequest::class, name = "SPENDING"),
    JsonSubTypes.Type(IncomeReceiptRequest::class, name = "INCOME_RECEIPT"),
    JsonSubTypes.Type(TransferRequest::class, name = "TRANSFER"),
    JsonSubTypes.Type(RefundRequest::class, name = "REFUND"),
)
sealed class CreateTransactionRequest {
    abstract val type: TransactionType
    abstract val date: LocalDate
    abstract val amount: BigDecimal
    abstract val description: String
    abstract val tags: String?
}

@Schema(description = "일반 소비 거래 생성 요청")
data class SpendingRequest(
    override val date: LocalDate,
    override val amount: BigDecimal,
    override val description: String,
    val accountId: Int,
    val categoryItemId: Int,
    val merchant: String,
    val paymentMethod: String? = null,
    override val tags: String? = null,
    val isWaste: Boolean? = false,
) : CreateTransactionRequest() {

    override val type: TransactionType
        get() = TransactionType.SPENDING

}

@Schema(description = "일반 수입 거래 생성 요청")
data class IncomeReceiptRequest(
    override val date: LocalDate,
    override val amount: BigDecimal,
    override val description: String,
    val accountId: Int,
    val categoryItemId: Int,
    val merchant: String? = null,
    override val tags: String? = null,
) : CreateTransactionRequest() {

    override val type: TransactionType
        get() = TransactionType.INCOME_RECEIPT

}

@Schema(description = "자산 이체 거래 생성 요청")
data class TransferRequest(
    override val date: LocalDate,
    override val amount: BigDecimal,
    override val description: String,
    val sourceAccountId: Int,
    val targetAccountId: Int,
    override val tags: String? = null,
) : CreateTransactionRequest() {

    override val type: TransactionType
        get() = TransactionType.TRANSFER

}

@Schema(description = "환불 거래 생성 요청")
data class RefundRequest(
    override val date: LocalDate,
    override val amount: BigDecimal,
    override val description: String,
    val accountId: Int,
    val categoryItemId: Int,
    val merchant: String? = null,
    override val tags: String? = null,
) : CreateTransactionRequest() {

    override val type: TransactionType
        get() = TransactionType.REFUND

}
