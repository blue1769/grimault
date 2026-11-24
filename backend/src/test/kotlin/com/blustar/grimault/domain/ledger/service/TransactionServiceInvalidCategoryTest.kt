package com.blustar.grimault.domain.ledger.service

import com.blustar.grimault.domain.account.entity.Account
import com.blustar.grimault.domain.account.entity.AccountSubtype.BANK
import com.blustar.grimault.domain.account.entity.AccountType.ASSET
import com.blustar.grimault.domain.account.repository.AccountRepository
import com.blustar.grimault.domain.category.entity.Category
import com.blustar.grimault.domain.category.entity.CategoryItem
import com.blustar.grimault.domain.category.entity.CategoryType
import com.blustar.grimault.domain.category.entity.CategoryType.EXPENSE
import com.blustar.grimault.domain.category.repository.CategoryItemRepository
import com.blustar.grimault.domain.ledger.dto.IncomeReceiptRequest
import com.blustar.grimault.domain.ledger.dto.RefundRequest
import com.blustar.grimault.domain.ledger.dto.SpendingRequest
import com.blustar.grimault.domain.ledger.entity.TransactionType
import com.blustar.grimault.domain.ledger.repository.TransactionRepository
import com.blustar.grimault.global.error.ErrorCode.INVALID_TRANSACTION_CATEGORY
import com.blustar.grimault.global.error.GrimaultException
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import java.math.BigDecimal
import java.time.LocalDate
import java.util.Optional

class TransactionServiceInvalidCategoryTest {
    private val accountRepository = mockk<AccountRepository>()
    private val categoryItemRepository = mockk<CategoryItemRepository>()
    private val transactionRepository = mockk<TransactionRepository>()
    private val service = TransactionService(accountRepository, categoryItemRepository, transactionRepository)

    @ParameterizedTest(name = "{0}")
    @CsvSource(
        "존재하지 않는 소분류, false, , ",
        "비활성 소분류, true, true, false",
        "비활성 대분류, true, false, true",
    )
    @Suppress("UNUSED_PARAMETER")
    fun `존재하지 않거나 선택 불가능한 손익 소분류의 참조를 거부한다`(
        scenario: String,
        itemExists: Boolean,
        categoryActive: Boolean?,
        itemActive: Boolean?,
    ) {
        val date = LocalDate.of(2026, 1, 2)
        val account = Account(1, "은행 계정", ASSET, BANK, date.minusDays(1))
        val request = SpendingRequest(date, BigDecimal("2500"), "점심", 1, 2, "식당")
        val item = if (itemExists) {
            val category = Category(3, EXPENSE, "식비", isActive = requireNotNull(categoryActive), sortOrder = 1)
            CategoryItem(2, category, "외식", isActive = requireNotNull(itemActive), sortOrder = 1)
        } else {
            null
        }
        every { accountRepository.findById(1) } returns Optional.of(account)
        every { categoryItemRepository.findById(2) } returns Optional.ofNullable(item)

        val exception = assertThrows(GrimaultException::class.java) { service.create(request) }

        assertEquals(INVALID_TRANSACTION_CATEGORY, exception.errorCode)
        verify(exactly = 0) { transactionRepository.save(any()) }
    }

    @ParameterizedTest(name = "{0}에서 {1} 사용")
    @CsvSource("SPENDING, INCOME", "INCOME_RECEIPT, EXPENSE", "REFUND, INCOME")
    fun `거래 유형과 일치하지 않는 손익 분류의 사용을 거부한다`(
        transactionType: TransactionType,
        categoryType: CategoryType,
    ) {
        val date = LocalDate.of(2026, 1, 2)
        val amount = BigDecimal("2500")
        val account = Account(1, "은행 계정", ASSET, BANK, date.minusDays(1))
        val category = Category(3, categoryType, "대분류", sortOrder = 1)
        val item = CategoryItem(2, category, "소분류", sortOrder = 1)
        val request = when (transactionType) {
            TransactionType.SPENDING -> SpendingRequest(date, amount, "지출", 1, 2, "식당")
            TransactionType.INCOME_RECEIPT -> IncomeReceiptRequest(date, amount, "수입", 1, 2)
            TransactionType.REFUND -> RefundRequest(date, amount, "환불", 1, 2)
            TransactionType.TRANSFER, TransactionType.OPENING_BALANCE -> error("손익 분류를 사용하지 않는 거래 유형: $transactionType")
        }
        every { accountRepository.findById(1) } returns Optional.of(account)
        every { categoryItemRepository.findById(2) } returns Optional.of(item)

        val exception = assertThrows(GrimaultException::class.java) { service.create(request) }

        assertEquals(INVALID_TRANSACTION_CATEGORY, exception.errorCode)
        verify(exactly = 0) { transactionRepository.save(any()) }
    }
}
