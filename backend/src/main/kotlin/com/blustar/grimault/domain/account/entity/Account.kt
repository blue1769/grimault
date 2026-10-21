package com.blustar.grimault.domain.account.entity

import jakarta.persistence.*
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.OffsetDateTime

@Entity
@Table(name = "account")
class Account(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Int? = null,

    name: String,

    @Column(name = "type")
    @Enumerated(EnumType.STRING)
    val type: AccountType,

    @Column(name = "subtype")
    @Enumerated(EnumType.STRING)
    val subtype: AccountSubtype,

    creditCardMetadata: CreditCardMetadata? = null,

    isActive: Boolean = true,

    @Column(name = "created_at")
    val createdAt: OffsetDateTime = OffsetDateTime.now(),
) {

    @Column(name = "name")
    final var name: String = name
        private set

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "credit_card_metadata")
    final var creditCardMetadata: CreditCardMetadata? = creditCardMetadata
        private set

    @Column(name = "is_active")
    final var isActive: Boolean = isActive
        private set

    fun rename(newName: String) {
        name = newName
    }

    fun updateCreditCardDetails(
        settlementDay: Int?,
        billingPeriod: CreditCardBillingPeriod?,
    ) {
        val current = requireNotNull(creditCardMetadata)

        creditCardMetadata = current.copy(
            settlementDay = settlementDay ?: current.settlementDay,
            billingPeriod = billingPeriod ?: current.billingPeriod,
        )
    }

}
