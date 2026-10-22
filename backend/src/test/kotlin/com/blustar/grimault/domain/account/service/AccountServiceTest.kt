package com.blustar.grimault.domain.account.service

import com.blustar.grimault.domain.account.repository.AccountQueryRepository
import com.blustar.grimault.domain.account.repository.AccountRepository
import com.blustar.grimault.global.error.ErrorCode
import com.blustar.grimault.global.error.GrimaultException
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

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
}
