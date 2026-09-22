package com.blustar.grimault.domain.account.entity

import jakarta.persistence.*
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

    @Column(name = "settlement_day")
    val settlementDay: Short? = null,

    @Column(name = "is_active")
    val isActive: Boolean = true,

    @Column(name = "created_at")
    val createdAt: OffsetDateTime = OffsetDateTime.now(),
)
