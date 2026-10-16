package com.blustar.grimault.domain.account.service

import com.blustar.grimault.domain.account.dto.AccountResponse
import com.blustar.grimault.domain.account.entity.AccountSubType
import com.blustar.grimault.domain.account.repository.AccountQueryRepository
import com.blustar.grimault.domain.account.repository.AccountRepository
import com.blustar.grimault.global.error.ErrorCode
import com.blustar.grimault.global.error.GrimaultException
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class AccountService(
    private val repository: AccountRepository,
    private val queryRepository: AccountQueryRepository,
) {

    fun findAll(
        isActive: Boolean? = null,
        kinds: List<String>? = null,
    ): List<AccountResponse> {
        return queryRepository
            .findAll(isActive, parseSubTypes(kinds))
            .map { AccountResponse.from(it) }
    }

    fun findById(id: Int): AccountResponse {
        val account = repository.findByIdOrNull(id)
            ?: throw GrimaultException(ErrorCode.ACCOUNT_NOT_FOUND, "Account not found: $id")

        return AccountResponse.from(account)
    }

    private fun parseSubTypes(kinds: List<String>?): List<AccountSubType> {
        val subTypes = kinds?.map {
            AccountSubType.fromNameOrNull(it.trim())
                ?: throw GrimaultException(ErrorCode.INVALID_ACCOUNT_KIND, "Invalid account kind: $it")
        }

        return subTypes
            ?: emptyList()
    }

}
