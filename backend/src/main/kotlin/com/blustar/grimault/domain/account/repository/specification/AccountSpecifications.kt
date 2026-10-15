package com.blustar.grimault.domain.account.repository.specification

import com.blustar.grimault.domain.account.entity.Account
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

}