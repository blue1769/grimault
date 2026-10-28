package com.blustar.grimault.domain.category.repository.specification

import com.blustar.grimault.domain.category.entity.Category
import com.blustar.grimault.domain.category.entity.CategoryType
import org.springframework.data.jpa.domain.Specification

object CategorySpecifications {

    fun equalType(type: CategoryType?): Specification<Category> =
        Specification { root, _, criteriaBuilder ->
            if (type == null) {
                criteriaBuilder.conjunction()
            } else {
                criteriaBuilder.equal(root.get<Category>("type"), type)
            }
        }

    fun filterSelectable(selectableOnly: Boolean): Specification<Category> =
        Specification { root, _, criteriaBuilder ->
            if (selectableOnly) {
                criteriaBuilder.isTrue(root.get("isActive"))
            } else {
                criteriaBuilder.conjunction()
            }
        }

}