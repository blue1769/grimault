package com.blustar.grimault.domain.account.service

import com.blustar.grimault.domain.account.dto.AccountResponse
import com.blustar.grimault.domain.account.repository.AccountQueryRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class AccountService(
    private val queryRepository: AccountQueryRepository
) {

    fun findAll(
        isActive: Boolean? = null
    ): List<AccountResponse> {
        return queryRepository
            .findAll(isActive)
            .map { AccountResponse.from(it) }
    }

}
