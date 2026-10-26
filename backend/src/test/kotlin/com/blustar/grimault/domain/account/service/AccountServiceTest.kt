package com.blustar.grimault.domain.account.service

import com.blustar.grimault.domain.account.dto.UpdateAccountRequest
import com.blustar.grimault.domain.account.entity.Account
import com.blustar.grimault.domain.account.entity.AccountSubtype
import com.blustar.grimault.domain.account.entity.AccountType
import com.blustar.grimault.domain.account.entity.CreditCardBillingPeriod
import com.blustar.grimault.domain.account.entity.CreditCardIssuer
import com.blustar.grimault.domain.account.entity.CreditCardMetadata
import com.blustar.grimault.domain.account.repository.AccountQueryRepository
import com.blustar.grimault.domain.account.repository.AccountRepository
import com.blustar.grimault.global.error.ErrorCode
import com.blustar.grimault.global.error.GrimaultException
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.Optional

class AccountServiceTest {

    private val repository = mockk<AccountRepository>()
    private val queryRepository = mockk<AccountQueryRepository>()
    private val service = AccountService(repository, queryRepository)

    @Test
    fun `유효하지 않은 계정 종류는 조회할 수 없다`() {
        // Arrange
        val kinds = listOf("INVALID_KIND")

        // Act
        val exception = assertThrows(GrimaultException::class.java) {
            service.findAll(kinds = kinds)
        }

        // Assert
        assertEquals(ErrorCode.INVALID_ACCOUNT_KIND, exception.errorCode)
        verify(exactly = 0) { queryRepository.findAll(any(), any()) }
    }

    @Test
    fun `비신용카드 계정의 신용카드 전용 정보 수정을 거부한다`() {
        // Arrange
        val account = Account(
            id = 1,
            name = "테스트 은행 계정",
            type = AccountType.ASSET,
            subtype = AccountSubtype.BANK,
            isActive = true,
        )
        val createdAt = account.createdAt
        val request = UpdateAccountRequest(settlementDay = 15)
        every { repository.findById(1) } returns Optional.of(account)

        // Act
        val exception = assertThrows(GrimaultException::class.java) {
            service.update(id = 1, body = request)
        }

        // Assert
        assertEquals(ErrorCode.INVALID_ACCOUNT_UPDATE, exception.errorCode)
        assertEquals(1, account.id)
        assertEquals("테스트 은행 계정", account.name)
        assertEquals(AccountType.ASSET, account.type)
        assertEquals(AccountSubtype.BANK, account.subtype)
        assertTrue(account.isActive)
        assertNull(account.creditCardMetadata)
        assertEquals(createdAt, account.createdAt)
    }

    @Test
    fun `비활성 신용카드의 상세정보 수정을 거부한다`() {
        // Arrange
        val metadata = CreditCardMetadata(
            issuer = CreditCardIssuer.SHINHAN,
            settlementDay = 15,
            billingPeriod = CreditCardBillingPeriod(
                startMonthOffset = -1,
                startDay = 1,
                endMonthOffset = 0,
                endDay = 15,
            ),
        )
        val account = Account(
            id = 1,
            name = "테스트 신용카드",
            type = AccountType.LIABILITY,
            subtype = AccountSubtype.CREDIT_CARD,
            isActive = false,
            creditCardMetadata = metadata,
        )
        val request = UpdateAccountRequest(settlementDay = 20)
        every { repository.findById(1) } returns Optional.of(account)

        // Act
        val exception = assertThrows(GrimaultException::class.java) {
            service.update(id = 1, body = request)
        }

        // Assert
        assertEquals(ErrorCode.INACTIVE_ACCOUNT_UPDATE, exception.errorCode)
        assertEquals("테스트 신용카드", account.name)
        assertFalse(account.isActive)
        assertEquals(metadata, account.creditCardMetadata)
    }

    @Test
    fun `활성 계정을 비활성화할 수 있다`() {
        // Arrange
        val account = Account(
            id = 1,
            name = "은행 계정",
            type = AccountType.ASSET,
            subtype = AccountSubtype.BANK,
            isActive = true,
        )
        every { repository.findById(1) } returns Optional.of(account)

        // Act
        val response = service.changeActiveStatus(id = 1, active = false)

        // Assert
        assertFalse(account.isActive)
        assertEquals(1, response.id)
        assertFalse(response.isActive)
    }
}
