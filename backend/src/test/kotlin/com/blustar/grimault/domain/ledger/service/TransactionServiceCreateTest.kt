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
import com.blustar.grimault.domain.ledger.entity.LedgerEntryType
import com.blustar.grimault.domain.ledger.entity.LedgerEntryType.*
import com.blustar.grimault.domain.ledger.entity.Transaction
import com.blustar.grimault.domain.ledger.repository.TransactionRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.math.BigDecimal
import java.time.LocalDate
import java.util.Optional

class TransactionServiceCreateTest {
    private val accountRepository = mockk<AccountRepository>()
    private val categoryItemRepository = mockk<CategoryItemRepository>()
    private val transactionRepository = mockk<TransactionRepository>()
    private val service = TransactionService(accountRepository, categoryItemRepository, transactionRepository)

    @ParameterizedTest(name = "{0}")
    @MethodSource("transactionCases")
    fun `거래 유형과 참여 계정에 따라 올바른 분개를 생성하고 응답을 반환한다`(
        caseName: String,
        request: CreateTransactionRequest,
        positiveType: LedgerEntryType,
        negativeType: LedgerEntryType,
        positiveAccount: Account?,
        negativeAccount: Account?,
        item: CategoryItem?,
        merchant: String?,
        paymentMethod: String?,
        isWaste: Boolean,
    ) {
        listOfNotNull(positiveAccount, negativeAccount).forEach { account ->
            every { accountRepository.findById(requireNotNull(account.id)) } returns Optional.of(account)
        }
        item?.let {
            every { categoryItemRepository.findById(requireNotNull(it.id)) } returns Optional.of(it)
        }
        val saved = slot<Transaction>()
        every { transactionRepository.save(capture(saved)) } answers {
            // Simulate the generated ID on the same entity referenced by its entries.
            firstArg<Transaction>().also { transaction ->
                Transaction::class.java.getDeclaredField("id").apply { isAccessible = true }
                    .set(transaction, 101L)
            }
        }

        val response = service.create(request)

        verify(exactly = 1) { transactionRepository.save(any()) }
        val transaction = saved.captured
        assertEquals(request.type, transaction.type, caseName)
        assertEquals(request.date, transaction.transactionDate)
        assertEquals(request.description, transaction.description)
        assertEquals(merchant, transaction.merchant)
        assertEquals(paymentMethod, transaction.paymentMethod)
        assertEquals(request.tags, transaction.tags)
        assertEquals(isWaste, transaction.isWaste)

        assertEquals(2, transaction.entries.size)
        val positive = transaction.entries.single { it.amount.signum() > 0 }
        val negative = transaction.entries.single { it.amount.signum() < 0 }
        assertSame(transaction, positive.transaction)
        assertSame(transaction, negative.transaction)
        assertEquals(positiveType, positive.entryType)
        assertEquals(negativeType, negative.entryType)
        assertSame(positiveAccount, positive.account)
        assertSame(negativeAccount, negative.account)
        assertSame(item.takeIf { positiveAccount == null }, positive.categoryItem)
        assertSame(item.takeIf { negativeAccount == null }, negative.categoryItem)
        assertEquals(request.amount, positive.amount)
        assertEquals(request.amount.negate(), negative.amount)
        assertEquals(0, transaction.entries.sumOf { it.amount }.signum())

        val source = negativeAccount
        val target = positiveAccount
        val transferMetadata = if (request is TransferRequest) {
            TransferMetadata(requireNotNull(source?.id), requireNotNull(source).name,
                requireNotNull(target?.id), requireNotNull(target).name)
        } else {
            null
        }
        val account = (positiveAccount ?: negativeAccount).takeUnless { request is TransferRequest }
        assertEquals(
            TransactionResponse(
                id = 101L,
                type = request.type,
                date = request.date,
                amount = request.amount,
                merchant = merchant,
                description = request.description,
                accountId = account?.id,
                accountName = account?.name,
                categoryItemId = item?.id,
                categoryLabel = item?.let { "${it.category.name}>${it.name}" },
                transferMetadata = transferMetadata,
                paymentMethod = paymentMethod,
                tags = request.tags,
                isWaste = isWaste,
            ),
            response,
        )
    }

    companion object {
        @JvmStatic
        fun transactionCases(): List<Arguments> {
            val openingDate = LocalDate.of(2026, 1, 1)
            val date = openingDate.plusDays(1)
            val amount = BigDecimal("2500")
            val bank = Account(1, "은행", AccountType.ASSET, BANK, openingDate)
            val cash = Account(2, "현금", AccountType.ASSET, CASH, openingDate)
            val card = Account(3, "신용카드", AccountType.LIABILITY, CREDIT_CARD, openingDate)
            val loan = Account(4, "대출", AccountType.LIABILITY, LOAN, openingDate)
            val expense = CategoryItem(
                5, Category(7, CategoryType.EXPENSE, "생활비", sortOrder = 1), "식비", sortOrder = 1,
            )
            val income = CategoryItem(
                6, Category(8, CategoryType.INCOME, "근로소득", sortOrder = 1), "급여", sortOrder = 1,
            )

            return listOf(
                Arguments.of(
                    "SPENDING / BANK",
                    SpendingRequest(
                        date, BigDecimal("2500.10"), "장보기", 1, 5, "마트",
                        paymentMethod = "체크카드", tags = "장보기", isWaste = true,
                    ),
                    EXPENSE, ASSET, null, bank, expense, "마트", "체크카드", true,
                ),
                Arguments.of(
                    "SPENDING / CREDIT_CARD",
                    SpendingRequest(date, amount, "점심", 3, 5, "식당"),
                    EXPENSE, LIABILITY, null, card, expense, "식당", null, false,
                ),
                Arguments.of(
                    "INCOME_RECEIPT / BANK",
                    IncomeReceiptRequest(date, amount, "급여 입금", 1, 6),
                    ASSET, REVENUE, bank, null, income, null, null, false,
                ),
                Arguments.of(
                    "TRANSFER / BANK → CASH",
                    TransferRequest(date, amount, "현금 인출", 1, 2),
                    ASSET, ASSET, cash, bank, null, null, null, false,
                ),
                Arguments.of(
                    "TRANSFER / BANK → CREDIT_CARD",
                    TransferRequest(date, amount, "카드 대금 납부", 1, 3),
                    LIABILITY, ASSET, card, bank, null, null, null, false,
                ),
                Arguments.of(
                    "TRANSFER / LOAN → BANK",
                    TransferRequest(date, amount, "대출금 입금", 4, 1),
                    ASSET, LIABILITY, bank, loan, null, null, null, false,
                ),
                Arguments.of(
                    "REFUND / BANK",
                    RefundRequest(date, amount, "장보기 환불", 1, 5, merchant = "마트"),
                    ASSET, EXPENSE, bank, null, expense, "마트", null, false,
                ),
                Arguments.of(
                    "REFUND / CREDIT_CARD",
                    RefundRequest(date, amount, "결제 취소", 3, 5, merchant = null),
                    LIABILITY, EXPENSE, card, null, expense, null, null, false,
                ),
            )
        }
    }
}
