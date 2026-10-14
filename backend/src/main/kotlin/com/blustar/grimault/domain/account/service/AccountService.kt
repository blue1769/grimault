package com.blustar.grimault.domain.account.service

import com.blustar.grimault.domain.account.dto.AccountResponse
import com.blustar.grimault.domain.account.repository.AccountRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class AccountService(
    private val repository: AccountRepository
) {

    fun findAll(
        isActive: Boolean? = null
    ): List<AccountResponse> {
        val accounts = if (isActive != null) {
            repository.findAllByIsActive(isActive)
        } else {
            repository.findAll()
        }

        return accounts
            .map { AccountResponse.from(it) }
    }

}
