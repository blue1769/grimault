package com.blustar.grimault.domain.account.service

import com.blustar.grimault.domain.account.dto.AccountResponse
import com.blustar.grimault.domain.account.entity.AccountSubType
import com.blustar.grimault.domain.account.repository.AccountQueryRepository
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException

@Service
@Transactional(readOnly = true)
class AccountService(
    private val queryRepository: AccountQueryRepository
) {

    fun findAll(
        isActive: Boolean? = null,
        kinds: List<String>? = null,
    ): List<AccountResponse> {
        return queryRepository
            .findAll(isActive, parseSubTypes(kinds))
            .map { AccountResponse.from(it) }
    }

    private fun parseSubTypes(kinds: List<String>?): List<AccountSubType> {
        return kinds
            ?.map { kind ->
                AccountSubType.fromName(kind.trim())
                    ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid account kind: $kind")
            }
            ?: emptyList()
    }

}
