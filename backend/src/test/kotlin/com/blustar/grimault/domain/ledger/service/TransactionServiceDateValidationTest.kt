package com.blustar.grimault.domain.ledger.service

import com.blustar.grimault.domain.account.entity.Account
import com.blustar.grimault.domain.account.entity.AccountSubtype.*
import com.blustar.grimault.domain.account.entity.AccountType
import com.blustar.grimault.domain.account.repository.AccountRepository
import com.blustar.grimault.domain.category.entity.Category
import com.blustar.grimault.domain.category.entity.CategoryItem
import com.blustar.grimault.domain.category.entity.CategoryType
import com.blustar.grimault.domain.category.repository.CategoryItemRepository
import com.blustar.grimault.domain.ledger.dto.*
import com.blustar.grimault.domain.ledger.entity.Transaction
import com.blustar.grimault.domain.ledger.repository.TransactionRepository
import com.blustar.grimault.global.error.ErrorCode.INVALID_TRANSACTION_ACCOUNT
import com.blustar.grimault.global.error.GrimaultException
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import java.math.BigDecimal
import java.time.LocalDate
import java.util.Optional

class TransactionServiceDateValidationTest {
    private val accountRepository = mockk<AccountRepository>()
    private val categoryItemRepository = mockk<CategoryItemRepository>()
    private val transactionRepository = mockk<TransactionRepository>()
    private val service = TransactionService(accountRepository, categoryItemRepository, transactionRepository)

    @ParameterizedTest(name = "원장 시작일로부터 {0}일 후")
    @CsvSource("0", "1")
    fun `원장 시작일 당일 및 미래 날짜의 거래를 허용한다`(daysAfterOpening: Long) {
        val today = LocalDate.now()
        val date = today.plusDays(daysAfterOpening)
        val account = Account(1, "은행", AccountType.ASSET, BANK, today)
        val item = CategoryItem(
            2, Category(3, CategoryType.EXPENSE, "생활비", sortOrder = 1), "식비", sortOrder = 1,
        )
        val request = SpendingRequest(date, BigDecimal("2500"), "점심", 1, 2, "식당")
        every { accountRepository.findById(1) } returns Optional.of(account)
        every { categoryItemRepository.findById(2) } returns Optional.of(item)
        val saved = slot<Transaction>()
        every { transactionRepository.save(capture(saved)) } answers {
            firstArg<Transaction>().also { transaction ->
                Transaction::class.java.getDeclaredField("id").apply { isAccessible = true }
                    .set(transaction, 101L)
            }
        }

        val response = service.create(request)

        verify(exactly = 1) { transactionRepository.save(any()) }
        assertEquals(date, saved.captured.transactionDate)
        assertEquals(date, response.date)
    }

    @Test
    fun `금융 계정의 원장 시작일 이전 거래를 거부한다`() {
        val account = Account(1, "은행", AccountType.ASSET, BANK, LocalDate.of(2026, 1, 1))
        val item = CategoryItem(
            2, Category(3, CategoryType.EXPENSE, "생활비", sortOrder = 1), "식비", sortOrder = 1,
        )
        val request = SpendingRequest(
            LocalDate.of(2025, 12, 31), BigDecimal("2500"), "점심", 1, 2, "식당",
        )
        every { accountRepository.findById(1) } returns Optional.of(account)
        every { categoryItemRepository.findById(2) } returns Optional.of(item)

        val exception = assertThrows(GrimaultException::class.java) { service.create(request) }

        assertEquals(INVALID_TRANSACTION_ACCOUNT, exception.errorCode)
        verify(exactly = 0) { transactionRepository.save(any()) }
    }

    @ParameterizedTest(name = "원계정 시작일 {0}, 목적계정 시작일 {1}")
    @CsvSource("2026-02-01, 2026-01-01", "2026-01-01, 2026-02-01")
    fun `이체 거래의 모든 참여 계정에 대해 원장 시작일을 검증한다`(
        sourceOpeningDate: LocalDate,
        targetOpeningDate: LocalDate,
    ) {
        val source = Account(1, "원계정", AccountType.ASSET, BANK, sourceOpeningDate)
        val target = Account(2, "목적계정", AccountType.ASSET, BANK, targetOpeningDate)
        val request = TransferRequest(LocalDate.of(2026, 1, 31), BigDecimal("2500"), "계좌 이체", 1, 2)
        every { accountRepository.findById(1) } returns Optional.of(source)
        every { accountRepository.findById(2) } returns Optional.of(target)

        val exception = assertThrows(GrimaultException::class.java) { service.create(request) }

        assertEquals(INVALID_TRANSACTION_ACCOUNT, exception.errorCode)
        verify(exactly = 0) { transactionRepository.save(any()) }
    }
}
