package com.blustar.grimault.domain.category.repository

import com.blustar.grimault.domain.category.entity.Category
import com.blustar.grimault.domain.category.entity.CategoryItem
import com.blustar.grimault.domain.category.entity.CategoryType
import com.blustar.grimault.domain.category.repository.specification.CategorySpecifications
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import jakarta.persistence.criteria.JoinType
import org.springframework.data.jpa.domain.Specification
import org.springframework.stereotype.Repository

@Repository
class CategoryTreeQueryRepository(
    @PersistenceContext
    private val entityManager: EntityManager,
) {

    fun findAll(
        type: CategoryType?,
        selectableOnly: Boolean
    ): List<Pair<Category, CategoryItem?>> {
        val criteriaBuilder = entityManager.criteriaBuilder
        val query = criteriaBuilder.createTupleQuery()
        val root = query.from(Category::class.java)

        val items = root.join<Category, CategoryItem>(
            "items",
            if (selectableOnly) JoinType.INNER else JoinType.LEFT
        )

        if (selectableOnly) {
            items.on(criteriaBuilder.isTrue(items.get("isActive")))
        }

        val specification = Specification
            .where(CategorySpecifications.equalType(type))
            .and(CategorySpecifications.filterSelectable(selectableOnly))

        val predicate = specification.toPredicate(root, query, criteriaBuilder)

        query.select(criteriaBuilder.tuple(root, items))
            .where(predicate)
            .orderBy(
                criteriaBuilder.asc(root.get<Int>("sortOrder")),
                criteriaBuilder.asc(root.get<Int>("id")),
                criteriaBuilder.asc(items.get<Int>("sortOrder")),
                criteriaBuilder.asc(items.get<Int>("id")),
            )

        return entityManager.createQuery(query)
            .resultList
            .map {
                Pair(
                    it.get(0, Category::class.java),
                    it.get(1, CategoryItem::class.java),
                )
            }
    }

}