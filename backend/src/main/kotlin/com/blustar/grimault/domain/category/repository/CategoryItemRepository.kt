package com.blustar.grimault.domain.category.repository

import com.blustar.grimault.domain.category.entity.CategoryItem
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface CategoryItemRepository : JpaRepository<CategoryItem, Int> {

    @Query("""
        SELECT MAX(i.sortOrder)
        FROM CategoryItem i
        WHERE i.category.id = :categoryId
    """)
    fun findMaxSortOrder(@Param("categoryId") categoryId: Int): Int?

}
