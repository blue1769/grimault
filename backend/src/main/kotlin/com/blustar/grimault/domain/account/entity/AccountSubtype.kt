package com.blustar.grimault.domain.account.entity

enum class AccountSubtype {
    BANK,
    CASH,
    PREPAID,
    CREDIT_CARD,
    LOAN,
    CAPITAL,
    ;

    companion object {
        fun fromNameOrNull(name: String): AccountSubtype? =
            entries.firstOrNull { it.name.equals(name, true) }
    }

}