package com.blustar.grimault.domain.account.repository.specification

import com.blustar.grimault.domain.account.entity.Account
import com.blustar.grimault.domain.account.entity.AccountSubtype
import org.springframework.data.jpa.domain.Specification

object AccountSpecifications {

    fun isActive(isActive: Boolean?): Specification<Account> =
        Specification { root, _, criteriaBuilder ->
            if (isActive == null) {
                criteriaBuilder.conjunction()
            } else {
                criteriaBuilder.equal(
                    root.get<Boolean>("isActive"),
                    isActive
                )
            }
        }

    fun subtypesIn(subtypes: List<AccountSubtype>): Specification<Account> =
        Specification { root, _, criteriaBuilder ->
            if (subtypes.isEmpty()) {
                criteriaBuilder.conjunction()
            } else {
                root.get<AccountSubtype>("subtype").`in`(subtypes)
            }
        }

}