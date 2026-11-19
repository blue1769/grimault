package com.blustar.grimault.domain.ledger.dto

import com.blustar.grimault.domain.ledger.entity.TransactionType
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import io.swagger.v3.oas.annotations.media.DiscriminatorMapping
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.*
import java.math.BigDecimal
import java.time.LocalDate

private const val VALID_TEXT_PATTERN = "(?s)(?!.*\\x00).*[^\\p{javaWhitespace}\\p{javaSpaceChar}].*"

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
    @get:NotNull
    abstract val type: TransactionType

    @get:NotNull
    abstract val date: LocalDate

    @get:NotNull
    @get:Positive
    @get:Digits(integer = 13, fraction = 2)
    abstract val amount: BigDecimal

    @get:NotNull
    @get:Size(max = 255)
    @get:Pattern(
        regexp = VALID_TEXT_PATTERN,
        message = "Description must contain non-blank text without NUL characters",
    )
    abstract val description: String

    @get:Pattern(regexp = VALID_TEXT_PATTERN)
    abstract val tags: String?
}

@Schema(description = "일반 소비 거래 생성 요청")
data class SpendingRequest(
    override val date: LocalDate,
    override val amount: BigDecimal,
    override val description: String,

    @field:NotNull
    @field:Positive
    val accountId: Int,

    @field:NotNull
    @field:Positive
    val categoryItemId: Int,

    @field:NotNull
    @field:Size(max = 100)
    @field:Pattern(
        regexp = VALID_TEXT_PATTERN,
        message = "Merchant must contain non-blank text without NUL characters",
    )
    val merchant: String,

    @field:Size(max = 50)
    @field:Pattern(regexp = VALID_TEXT_PATTERN)
    val paymentMethod: String? = null,
    override val tags: String? = null,

    @field:NotNull
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

    @field:NotNull
    @field:Positive
    val accountId: Int,

    @field:NotNull
    @field:Positive
    val categoryItemId: Int,

    @field:Size(max = 100)
    @field:Pattern(regexp = VALID_TEXT_PATTERN)
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

    @field:NotNull
    @field:Positive
    val sourceAccountId: Int,

    @field:NotNull
    @field:Positive
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

    @field:NotNull
    @field:Positive
    val accountId: Int,

    @field:NotNull
    @field:Positive
    val categoryItemId: Int,

    @field:Size(max = 100)
    @field:Pattern(regexp = VALID_TEXT_PATTERN)
    val merchant: String? = null,
    override val tags: String? = null,
) : CreateTransactionRequest() {

    override val type: TransactionType
        get() = TransactionType.REFUND

}
