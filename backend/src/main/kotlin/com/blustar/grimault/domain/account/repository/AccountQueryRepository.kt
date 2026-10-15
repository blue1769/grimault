package com.blustar.grimault.domain.account.repository

import com.blustar.grimault.domain.account.entity.Account
import com.blustar.grimault.domain.account.entity.AccountSubType
import com.blustar.grimault.domain.account.repository.specification.AccountSpecifications
import org.springframework.data.jpa.domain.Specification
import org.springframework.stereotype.Repository

@Repository
class AccountQueryRepository(
    private val repository: AccountRepository
) {

    fun findAll(
        isActive: Boolean?,
        subTypes: List<AccountSubType>,
    ): List<Account> {
        val specification = Specification.allOf(
            AccountSpecifications.isActive(isActive),
            AccountSpecifications.subTypesIn(subTypes),
        )

        return repository.findAll(specification)
    }

}