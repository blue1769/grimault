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

    @Column(name = "name")
    val name: String,

    @Column(name = "type")
    @Enumerated(EnumType.STRING)
    val type: AccountType,

    @Column(name = "sub_type")
    @Enumerated(EnumType.STRING)
    val subType: AccountSubType,

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "credit_card_metadata")
    val creditCardMetadata: CreditCardMetadata? = null,

    @Column(name = "is_active")
    val isActive: Boolean = true,

    @Column(name = "created_at")
    val createdAt: OffsetDateTime = OffsetDateTime.now(),
)
