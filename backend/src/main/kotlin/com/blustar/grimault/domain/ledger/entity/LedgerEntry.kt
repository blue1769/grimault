package com.blustar.grimault.domain.ledger.entity

import com.blustar.grimault.domain.account.entity.Account
import com.blustar.grimault.domain.category.entity.Category
import jakarta.persistence.*
import java.math.BigDecimal

@Entity
@Table(name = "ledger_entry")
class LedgerEntry(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_id")
    val transaction: Transaction,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id")
    val account: Account?,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    val category: Category?,

    @Column(name = "amount")
    val amount: BigDecimal,

    @Column(name = "entry_type")
    @Enumerated(EnumType.STRING)
    val entryType: LedgerEntryType,
)