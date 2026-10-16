package com.blustar.grimault.domain.account.entity

enum class AccountType {
    ASSET,
    LIABILITY,
    EQUITY,
    ;

    companion object {
        fun fromNameOrNull(name: String): AccountType? =
            entries.firstOrNull { it.name.equals(name, true) }
    }

}