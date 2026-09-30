package com.blustar.grimault.domain.account.dto

import com.blustar.grimault.domain.account.entity.Account
import com.blustar.grimault.domain.account.entity.AccountSubType
import com.blustar.grimault.domain.account.entity.AccountType

data class AccountResponse(
    val id: Int,
    val name: String,
    val type: AccountType,
    val subType: AccountSubType,
    val settlementDay: Short?,
    val isActive: Boolean,
) {
    companion object {
        fun from(account: Account): AccountResponse {
            val verifiedId = requireNotNull(account.id) {
                "Account id must not be null"
            }

            return AccountResponse(
                id = verifiedId,
                name = account.name,
                type = account.type,
                subType = account.subType,
                settlementDay = account.settlementDay,
                isActive = account.isActive
            )
        }
    }
}
