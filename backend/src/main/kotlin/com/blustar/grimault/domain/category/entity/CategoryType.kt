package com.blustar.grimault.domain.category.entity

enum class CategoryType {
    EXPENSE,
    INCOME,
    ;

    companion object {
        fun fromNameOrNull(name: String?): CategoryType? =
            entries.firstOrNull { it.name.equals(name, true) }
    }

}
