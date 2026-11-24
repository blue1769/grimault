package com.blustar.grimault.domain.ledger.service

import com.blustar.grimault.domain.account.entity.Account
import com.blustar.grimault.domain.account.entity.AccountSubtype
import com.blustar.grimault.domain.account.entity.AccountSubtype.BANK
import com.blustar.grimault.domain.account.entity.AccountType
import com.blustar.grimault.domain.account.repository.AccountRepository
import com.blustar.grimault.domain.category.entity.Category
import com.blustar.grimault.domain.category.entity.CategoryItem
import com.blustar.grimault.domain.category.entity.CategoryType
import com.blustar.grimault.domain.category.repository.CategoryItemRepository
import com.blustar.grimault.domain.ledger.dto.IncomeReceiptRequest
import com.blustar.grimault.domain.ledger.dto.RefundRequest
import com.blustar.grimault.domain.ledger.dto.SpendingRequest
import com.blustar.grimault.domain.ledger.dto.TransferRequest
import com.blustar.grimault.domain.ledger.entity.TransactionType
import com.blustar.grimault.domain.ledger.repository.TransactionRepository
import com.blustar.grimault.global.error.ErrorCode.INVALID_TRANSACTION_ACCOUNT
import com.blustar.grimault.global.error.GrimaultException
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import java.math.BigDecimal
import java.time.LocalDate
import java.util.Optional

class TransactionServiceInvalidAccountTest {
    private val accountRepository = mockk<AccountRepository>()
    private val categoryItemRepository = mockk<CategoryItemRepository>()
    private val transactionRepository = mockk<TransactionRepository>()
    private val service = TransactionService(accountRepository, categoryItemRepository, transactionRepository)

    @ParameterizedTest(name = "{0}")
    @CsvSource("존재하지 않는 금융 계정, false", "비활성 BANK 계정, true")
    fun `존재하지 않거나 비활성 상태인 금융 계정의 참조를 거부한다`(
        scenario: String,
        accountExists: Boolean,
    ) {
        val date = LocalDate.of(2026, 1, 2)
        val request = SpendingRequest(date, BigDecimal("2500"), "점심", 1, 2, "식당")
        val account = if (accountExists) {
            Account(1, scenario, AccountType.ASSET, BANK, date.minusDays(1), isActive = false)
        } else {
            null
        }
        every { accountRepository.findById(1) } returns Optional.ofNullable(account)

        val exception = assertThrows(GrimaultException::class.java) { service.create(request) }

        assertEquals(INVALID_TRANSACTION_ACCOUNT, exception.errorCode)
        verify(exactly = 0) { transactionRepository.save(any()) }
    }

    @ParameterizedTest(name = "{0}에서 {1} 사용")
    @CsvSource("SPENDING, LOAN", "INCOME_RECEIPT, CREDIT_CARD", "INCOME_RECEIPT, LOAN", "REFUND, LOAN")
    fun `거래 유형에서 허용하지 않는 금융 계정 subtype의 사용을 거부한다`(
        transactionType: TransactionType,
        subtype: AccountSubtype,
    ) {
        val date = LocalDate.of(2026, 1, 2)
        val amount = BigDecimal("2500")
        val account = Account(1, "부채 계정", AccountType.LIABILITY, subtype, date.minusDays(1))
        val categoryType = when (transactionType) {
            TransactionType.INCOME_RECEIPT -> CategoryType.INCOME
            else -> CategoryType.EXPENSE
        }
        val item = CategoryItem(
            2, Category(3, categoryType, "분류", sortOrder = 1), "항목", sortOrder = 1,
        )
        val request = when (transactionType) {
            TransactionType.SPENDING -> SpendingRequest(date, amount, "지출", 1, 2, "식당")
            TransactionType.INCOME_RECEIPT -> IncomeReceiptRequest(date, amount, "수입", 1, 2)
            TransactionType.REFUND -> RefundRequest(date, amount, "환불", 1, 2)
            else -> error("계정 subtype 검증에 사용할 수 없는 거래 유형: $transactionType")
        }
        every { accountRepository.findById(1) } returns Optional.of(account)
        every { categoryItemRepository.findById(2) } returns Optional.of(item)

        val exception = assertThrows(GrimaultException::class.java) { service.create(request) }

        assertEquals(INVALID_TRANSACTION_ACCOUNT, exception.errorCode)
        verify(exactly = 0) { transactionRepository.save(any()) }
    }

    @Test
    fun `동일한 금융 계정 간 이체를 거부한다`() {
        val request = TransferRequest(LocalDate.of(2026, 1, 2), BigDecimal("2500"), "계좌 이체", 1, 1)

        val exception = assertThrows(GrimaultException::class.java) { service.create(request) }

        assertEquals(INVALID_TRANSACTION_ACCOUNT, exception.errorCode)
        verify(exactly = 0) { transactionRepository.save(any()) }
    }

    @ParameterizedTest(name = "{0} ({1}) → {2} ({3})")
    @CsvSource(
        "EQUITY, CAPITAL, ASSET, BANK",
        "ASSET, BANK, EQUITY, CAPITAL",
        "LIABILITY, CREDIT_CARD, LIABILITY, LOAN",
    )
    fun `허용하지 않는 금융 계정 조합의 이체를 거부한다`(
        sourceType: AccountType,
        sourceSubtype: AccountSubtype,
        targetType: AccountType,
        targetSubtype: AccountSubtype,
    ) {
        val date = LocalDate.of(2026, 1, 2)
        val openingDate = date.minusDays(1)
        val source = Account(1, "원계정", sourceType, sourceSubtype, openingDate)
        val target = Account(2, "목적계정", targetType, targetSubtype, openingDate)
        val request = TransferRequest(date, BigDecimal("2500"), "계좌 이체", 1, 2)
        every { accountRepository.findById(1) } returns Optional.of(source)
        every { accountRepository.findById(2) } returns Optional.of(target)

        val exception = assertThrows(GrimaultException::class.java) { service.create(request) }

        assertEquals(INVALID_TRANSACTION_ACCOUNT, exception.errorCode)
        verify(exactly = 0) { transactionRepository.save(any()) }
    }
}
