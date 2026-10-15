package com.blustar.grimault.domain.account.repository

import com.blustar.grimault.domain.account.entity.Account
import com.blustar.grimault.domain.account.repository.specification.AccountSpecifications
import org.springframework.stereotype.Repository

@Repository
class AccountQueryRepository(
    private val repository: AccountRepository
) {

    fun findAll(
        isActive: Boolean?
    ): List<Account> {
        val specification = AccountSpecifications
            .isActive(isActive)

        return repository.findAll(specification)
    }

}