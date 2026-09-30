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
    fun findAll(): List<AccountResponse> {
        return repository
            .findAll()
            .map {
                val verifiedId = requireNotNull(it.id) {
                    "Account id must not be null"
                }

                AccountResponse(
                    id = verifiedId,
                    name = it.name,
                    type = it.type,
                    subType = it.subType,
                    settlementDay = it.settlementDay,
                    isActive = it.isActive
                )
            }
    }
}
