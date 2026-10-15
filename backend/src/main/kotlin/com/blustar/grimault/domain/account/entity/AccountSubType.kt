package com.blustar.grimault.domain.account.entity

enum class AccountSubType {
    BANK,
    CASH,
    PREPAID,
    CREDIT_CARD,
    LOAN,
    CAPITAL,
    ;

    companion object {
        fun fromName(name: String): AccountSubType? =
            entries.firstOrNull { it.name.equals(name, true) }
    }

}