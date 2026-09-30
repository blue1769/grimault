package com.blustar.grimault.domain.account.dto

import com.blustar.grimault.domain.account.entity.AccountSubType
import com.blustar.grimault.domain.account.entity.AccountType

data class AccountResponse(
    val id: Int,
    val name: String,
    val type: AccountType,
    val subType: AccountSubType,
    val settlementDay: Short?,
    val isActive: Boolean,
)
