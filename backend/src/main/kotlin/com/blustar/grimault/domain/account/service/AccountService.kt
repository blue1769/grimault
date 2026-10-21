package com.blustar.grimault.domain.account.service

import com.blustar.grimault.domain.account.dto.AccountResponse
import com.blustar.grimault.domain.account.dto.CreateAccountRequest
import com.blustar.grimault.domain.account.dto.UpdateAccountRequest
import com.blustar.grimault.domain.account.entity.AccountSubtype
import com.blustar.grimault.domain.account.entity.CreditCardBillingPeriod
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
            .findAll(isActive, parseSubtypes(kinds))
            .map { AccountResponse.from(it) }
    }

    fun findById(id: Int): AccountResponse {
        val account = repository.findByIdOrNull(id)
            ?: throw GrimaultException(ErrorCode.ACCOUNT_NOT_FOUND, "Account not found: $id")

        return AccountResponse.from(account)
    }

    @Transactional
    fun create(body: CreateAccountRequest): AccountResponse {
        val account = repository.save(body.toEntity())

        return AccountResponse.from(account)
    }

    @Transactional
    fun update(id: Int, body: UpdateAccountRequest): AccountResponse {
        val account = repository.findByIdOrNull(id)
            ?: throw GrimaultException(ErrorCode.ACCOUNT_NOT_FOUND, "Account not found: $id")

        val hasCreditCardMetadataUpdates = (body.hasCompleteBillingPeriod || body.settlementDay != null)

        if (account.subtype != AccountSubtype.CREDIT_CARD && hasCreditCardMetadataUpdates) {
            throw GrimaultException(
                ErrorCode.INVALID_ACCOUNT_UPDATE,
                "Credit card metadata cannot be updated for this account"
            )
        }

        if (!account.isActive && hasCreditCardMetadataUpdates) {
            throw GrimaultException(
                ErrorCode.INACTIVE_ACCOUNT_UPDATE,
                "Credit card details cannot be updated for an inactive account"
            )
        }

        body.name?.let { account.rename(it) }

        if (hasCreditCardMetadataUpdates) {
            val billingPeriod = if (body.hasCompleteBillingPeriod) {
                CreditCardBillingPeriod(
                    startMonthOffset = requireNotNull(body.startMonthOffset),
                    startDay = requireNotNull(body.startDay),
                    endMonthOffset = requireNotNull(body.endMonthOffset),
                    endDay = requireNotNull(body.endDay),
                )
            } else {
                null
            }

            account.updateCreditCardDetails(
                settlementDay = body.settlementDay,
                billingPeriod = billingPeriod,
            )
        }

        return AccountResponse.from(account)
    }

    @Transactional
    fun changeActiveStatus(id: Int, active: Boolean): AccountResponse {
        val account = repository.findByIdOrNull(id)
            ?: throw GrimaultException(ErrorCode.ACCOUNT_NOT_FOUND, "Account not found: $id")

        if (active) account.activate() else account.deactivate()

        return AccountResponse.from(account)
    }

    private fun parseSubtypes(kinds: List<String>?): List<AccountSubtype> {
        val subtypes = kinds?.map {
            AccountSubtype.fromNameOrNull(it.trim())
                ?: throw GrimaultException(ErrorCode.INVALID_ACCOUNT_KIND, "Invalid account kind: $it")
        }

        return subtypes
            ?: emptyList()
    }

}
