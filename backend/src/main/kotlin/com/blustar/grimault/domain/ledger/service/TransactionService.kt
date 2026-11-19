package com.blustar.grimault.domain.ledger.service

import com.blustar.grimault.domain.account.entity.Account
import com.blustar.grimault.domain.account.entity.AccountSubtype.*
import com.blustar.grimault.domain.account.entity.AccountType.*
import com.blustar.grimault.domain.account.repository.AccountRepository
import com.blustar.grimault.domain.category.entity.CategoryItem
import com.blustar.grimault.domain.category.entity.CategoryType.EXPENSE
import com.blustar.grimault.domain.category.entity.CategoryType.INCOME
import com.blustar.grimault.domain.category.repository.CategoryItemRepository
import com.blustar.grimault.domain.ledger.dto.*
import com.blustar.grimault.domain.ledger.entity.Transaction
import com.blustar.grimault.domain.ledger.mapper.TransactionMapper
import com.blustar.grimault.domain.ledger.repository.TransactionRepository
import com.blustar.grimault.global.error.ErrorCode.INVALID_TRANSACTION_ACCOUNT
import com.blustar.grimault.global.error.ErrorCode.INVALID_TRANSACTION_CATEGORY
import com.blustar.grimault.global.error.GrimaultException
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Service
@Transactional(readOnly = true)
class TransactionService(
    private val accountRepository: AccountRepository,
    private val categoryItemRepository: CategoryItemRepository,
    private val transactionRepository: TransactionRepository,
) {

    @Transactional
    fun create(body: CreateTransactionRequest): TransactionResponse {
        val transaction = when (body) {
            is SpendingRequest -> createSpending(body)
            is IncomeReceiptRequest -> createIncomeReceipt(body)
            is TransferRequest -> createTransfer(body)
            is RefundRequest -> createRefund(body)
        }

        return TransactionMapper.toResponse(
            transactionRepository.save(transaction)
        )
    }

    private fun createSpending(body: SpendingRequest): Transaction {
        val account = retrieveValidAccount(body.accountId, body.date)
        val item = retrieveValidCategoryItem(body.categoryItemId)

        if (account.subtype !in setOf(BANK, CASH, PREPAID, CREDIT_CARD)) {
            throw GrimaultException(
                INVALID_TRANSACTION_ACCOUNT,
                "Account subtype not allowed for spending: ${account.subtype}",
            )
        }

        if (item.category.type != EXPENSE) {
            throw GrimaultException(
                INVALID_TRANSACTION_CATEGORY,
                "Category type not allowed for spending: ${item.category.type}",
            )
        }

        return TransactionMapper.toEntity(body, account, item)
    }

    private fun createIncomeReceipt(body: IncomeReceiptRequest): Transaction {
        val account = retrieveValidAccount(body.accountId, body.date)
        val item = retrieveValidCategoryItem(body.categoryItemId)

        if (account.subtype !in setOf(BANK, CASH, PREPAID)) {
            throw GrimaultException(
                INVALID_TRANSACTION_ACCOUNT,
                "Account subtype not allowed for income-receipt: ${account.subtype}",
            )
        }

        if (item.category.type != INCOME) {
            throw GrimaultException(
                INVALID_TRANSACTION_CATEGORY,
                "Category type not allowed for income-receipt: ${item.category.type}",
            )
        }

        return TransactionMapper.toEntity(body, account, item)
    }

    private fun createTransfer(body: TransferRequest): Transaction {
        if (body.sourceAccountId == body.targetAccountId) {
            throw GrimaultException(
                INVALID_TRANSACTION_ACCOUNT,
                "Source and target accounts must be different: ${body.sourceAccountId}",
            )
        }

        val source = retrieveValidAccount(body.sourceAccountId, body.date)
        val target = retrieveValidAccount(body.targetAccountId, body.date)

        val isValidCombination = when (source.type) {
            ASSET -> target.type in setOf(ASSET, LIABILITY)
            LIABILITY -> target.type == ASSET
            EQUITY -> false
        }

        if (!isValidCombination) {
            throw GrimaultException(
                INVALID_TRANSACTION_ACCOUNT,
                "Invalid transfer account combination: ${source.type} -> ${target.type}",
            )
        }

        return TransactionMapper.toEntity(body, source, target)
    }

    private fun createRefund(body: RefundRequest): Transaction {
        val account = retrieveValidAccount(body.accountId, body.date)
        val item = retrieveValidCategoryItem(body.categoryItemId)

        if (account.subtype !in setOf(BANK, CASH, PREPAID, CREDIT_CARD)) {
            throw GrimaultException(
                INVALID_TRANSACTION_ACCOUNT,
                "Account subtype not allowed for refund: ${account.subtype}",
            )
        }

        if (item.category.type != EXPENSE) {
            throw GrimaultException(
                INVALID_TRANSACTION_CATEGORY,
                "Category type not allowed for refund: ${item.category.type}",
            )
        }

        return TransactionMapper.toEntity(body, account, item)
    }

    private fun retrieveValidAccount(
        accountId: Int,
        transactionDate: LocalDate,
    ): Account {
        val account = accountRepository.findByIdOrNull(accountId)
            ?: throw GrimaultException(
                INVALID_TRANSACTION_ACCOUNT,
                "Invalid account referenced: $accountId",
            )

        if (!account.isActive) {
            throw GrimaultException(
                INVALID_TRANSACTION_ACCOUNT,
                "Referenced account is inactive: $accountId",
            )
        }

        if (account.openingDate > transactionDate) {
            throw GrimaultException(
                INVALID_TRANSACTION_ACCOUNT,
                "Transaction date precedes account opening date: $accountId",
            )
        }

        return account
    }

    private fun retrieveValidCategoryItem(categoryItemId: Int): CategoryItem {
        val item = categoryItemRepository.findByIdOrNull(categoryItemId)
            ?: throw GrimaultException(
                INVALID_TRANSACTION_CATEGORY,
                "Invalid category referenced: $categoryItemId",
            )

        if (!item.category.isActive || !item.isActive) {
            throw GrimaultException(
                INVALID_TRANSACTION_CATEGORY,
                "Referenced category is inactive: $categoryItemId",
            )
        }

        return item
    }

}
