package com.blustar.grimault.domain.ledger.entity

import jakarta.persistence.*
import java.time.LocalDate
import java.time.OffsetDateTime

@Entity
@Table(name = "transaction")
class Transaction(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "transaction_date")
    val transactionDate: LocalDate,

    @Column(name = "merchant")
    val merchant: String,

    @Column(name = "description")
    val description: String,

    @Column(name = "payment_method")
    val paymentMethod: String? = null,

    @Column(name = "tags")
    val tags: String? = null,

    @Column(name = "is_waste")
    val isWaste: Boolean = false,

    @Column(name = "created_at")
    val createdAt: OffsetDateTime = OffsetDateTime.now(),
)
