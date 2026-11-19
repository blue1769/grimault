package com.blustar.grimault.domain.ledger.mapper

import com.blustar.grimault.domain.account.entity.Account
import com.blustar.grimault.domain.account.entity.AccountType.*
import com.blustar.grimault.domain.category.entity.CategoryItem
import com.blustar.grimault.domain.category.entity.CategoryType.EXPENSE
import com.blustar.grimault.domain.category.entity.CategoryType.INCOME
import com.blustar.grimault.domain.ledger.dto.*
import com.blustar.grimault.domain.ledger.entity.LedgerEntry
import com.blustar.grimault.domain.ledger.entity.LedgerEntryType
import com.blustar.grimault.domain.ledger.entity.Transaction
import com.blustar.grimault.domain.ledger.entity.TransactionType.*
import java.math.BigDecimal

object TransactionMapper {

    fun toEntity(
        body: SpendingRequest,
        account: Account,
        item: CategoryItem,
    ): Transaction = toEntity(
        body,
        EntryPair(
            debit = EntrySubject.Categorical(item),
            credit = EntrySubject.Financial(account),
        ),
    )

    fun toEntity(
        body: IncomeReceiptRequest,
        account: Account,
        item: CategoryItem,
    ): Transaction = toEntity(
        body,
        EntryPair(
            debit = EntrySubject.Financial(account),
            credit = EntrySubject.Categorical(item),
        ),
    )

    fun toEntity(
        body: TransferRequest,
        source: Account,
        target: Account,
    ): Transaction = toEntity(
        body,
        EntryPair(
            debit = EntrySubject.Financial(target),
            credit = EntrySubject.Financial(source),
        ),
    )

    fun toEntity(
        body: RefundRequest,
        account: Account,
        item: CategoryItem,
    ): Transaction = toEntity(
        body,
        EntryPair(
            debit = EntrySubject.Financial(account),
            credit = EntrySubject.Categorical(item),
        ),
    )

    private fun toEntity(
        body: CreateTransactionRequest,
        pair: EntryPair,
    ): Transaction {
        val transaction = Transaction(
            transactionDate = body.date,
            type = body.type,
            merchant = when (body) {
                is SpendingRequest -> body.merchant
                is IncomeReceiptRequest -> body.merchant
                is TransferRequest -> null
                is RefundRequest -> body.merchant
            },
            description = body.description,
            paymentMethod = (body as? SpendingRequest)?.paymentMethod,
            tags = body.tags,
            isWaste = (body as? SpendingRequest)?.isWaste ?: false,
        )

        transaction.entries.addAll(
            listOf(
                pair.debit.toLedgerEntry(transaction, body.amount),
                pair.credit.toLedgerEntry(transaction, body.amount.negate()),
            )
        )

        return transaction
    }

    fun toResponse(transaction: Transaction): TransactionResponse {
        val entries = transaction.entries

        val debit = entries.single { it.amount.signum() > 0 }
        val credit = entries.single { it.amount.signum() < 0 }

        val account = when (transaction.type) {
            SPENDING -> requireNotNull(credit.account)
            INCOME_RECEIPT, REFUND -> requireNotNull(debit.account)
            TRANSFER -> null
            OPENING_BALANCE -> listOf(debit, credit)
                .single { it.entryType != LedgerEntryType.EQUITY }
                .let { requireNotNull(it.account) }
        }

        val item = when (transaction.type) {
            SPENDING -> requireNotNull(debit.categoryItem)
            INCOME_RECEIPT, REFUND -> requireNotNull(credit.categoryItem)
            TRANSFER, OPENING_BALANCE -> null
        }

        val transferMetadata = if (transaction.type == TRANSFER) {
            val source = requireNotNull(credit.account)
            val target = requireNotNull(debit.account)

            TransferMetadata(
                sourceId = requireNotNull(source.id),
                sourceName = source.name,
                targetId = requireNotNull(target.id),
                targetName = target.name,
            )
        } else {
            null
        }

        return TransactionResponse(
            id = requireNotNull(transaction.id),
            type = transaction.type,
            date = transaction.transactionDate,
            amount = debit.amount,
            merchant = when (transaction.type) {
                TRANSFER, OPENING_BALANCE -> null
                else -> transaction.merchant
            },
            description = transaction.description,
            accountId = account?.let { requireNotNull(it.id) },
            accountName = account?.name,
            categoryItemId = item?.let { requireNotNull(it.id) },
            categoryLabel = item?.let { "${it.category.name}>${it.name}" },
            transferMetadata = transferMetadata,
            paymentMethod = transaction.paymentMethod
                .takeIf { transaction.type == SPENDING },
            tags = transaction.tags
                .takeUnless { transaction.type == OPENING_BALANCE },
            isWaste = transaction.type == SPENDING && transaction.isWaste,
        )
    }

    private fun EntrySubject.toLedgerEntry(
        transaction: Transaction,
        amount: BigDecimal,
    ): LedgerEntry = LedgerEntry(
        transaction = transaction,
        account = (this as? EntrySubject.Financial)?.account,
        categoryItem = (this as? EntrySubject.Categorical)?.item,
        amount = amount,
        entryType = entryType,
    )

    private sealed interface EntrySubject {
        val entryType: LedgerEntryType

        data class Financial(val account: Account) : EntrySubject {
            override val entryType: LedgerEntryType
                get() = when (account.type) {
                    ASSET -> LedgerEntryType.ASSET
                    LIABILITY -> LedgerEntryType.LIABILITY
                    EQUITY -> LedgerEntryType.EQUITY
                }
        }

        data class Categorical(val item: CategoryItem) : EntrySubject {
            override val entryType: LedgerEntryType
                get() = when (item.category.type) {
                    INCOME -> LedgerEntryType.REVENUE
                    EXPENSE -> LedgerEntryType.EXPENSE
                }
        }
    }

    private data class EntryPair(
        val debit: EntrySubject,
        val credit: EntrySubject,
    )

}
