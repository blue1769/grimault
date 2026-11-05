package com.blustar.grimault.domain.category.repository

import com.blustar.grimault.domain.category.entity.CategoryItem
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.NativeQuery
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface CategoryItemRepository : JpaRepository<CategoryItem, Int> {

    @Query("""
        SELECT MAX(i.sortOrder)
        FROM CategoryItem i
        WHERE i.category.id = :categoryId
    """)
    fun findMaxSortOrder(@Param("categoryId") categoryId: Int): Int?

    @Modifying
    @Query("""
        UPDATE CategoryItem i
        SET i.sortOrder = i.sortOrder - 1
        WHERE i.category.id = :categoryId
        AND i.sortOrder > :removedOrder
    """)
    fun compact(
        @Param("categoryId") categoryId: Int,
        @Param("removedOrder") removedOrder: Int,
    ): Int

    @Modifying
    @Query("""
        UPDATE CategoryItem i
        SET i.sortOrder = CASE
            WHEN i.id = :id THEN :target
            ELSE i.sortOrder - 1
        END
        WHERE i.category.id = :categoryId
        AND (i.id = :id OR i.sortOrder BETWEEN :current + 1 AND :target)
    """)
    fun reorderForward(
        @Param("id") id: Int,
        @Param("categoryId") categoryId: Int,
        @Param("current") current: Int,
        @Param("target") target: Int,
    ): Int

    @Modifying
    @Query("""
        UPDATE CategoryItem i
        SET i.sortOrder = CASE
            WHEN i.id = :id THEN :target
            ELSE i.sortOrder + 1
        END
        WHERE i.category.id = :categoryId
        AND (i.id = :id OR i.sortOrder BETWEEN :target AND :current - 1)
    """)
    fun reorderBackward(
        @Param("id") id: Int,
        @Param("categoryId") categoryId: Int,
        @Param("current") current: Int,
        @Param("target") target: Int,
    ): Int

    @Modifying
    @NativeQuery("""
        SET CONSTRAINTS uq_category_item_sort_order DEFERRED
    """)
    fun deferOrderConstraint()

}
