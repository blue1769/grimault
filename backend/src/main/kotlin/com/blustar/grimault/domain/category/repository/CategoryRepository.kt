package com.blustar.grimault.domain.category.repository

import com.blustar.grimault.domain.category.entity.Category
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.*
import org.springframework.data.repository.query.Param

interface CategoryRepository : JpaRepository<Category, Int> {

    @Query("SELECT MAX(c.sortOrder) FROM Category c")
    fun findMaxSortOrder(): Int?

    @Modifying
    @Query("""
        UPDATE Category c
        SET sortOrder = CASE
            WHEN id = :id THEN :target
            ELSE sortOrder - 1
        END
        WHERE id = :id
        OR sortOrder BETWEEN :current + 1 AND :target
    """)
    fun reorderForward(
        @Param("id") id: Int,
        @Param("current") current: Int,
        @Param("target") target: Int,
    ): Int

    @Modifying
    @Query("""
        UPDATE Category c
        SET sortOrder = CASE
            WHEN id = :id THEN :target
            ELSE sortOrder + 1
        END
        WHERE id = :id
        OR sortOrder BETWEEN :target AND :current - 1
    """)
    fun reorderBackward(
        @Param("id") id: Int,
        @Param("current") current: Int,
        @Param("target") target: Int,
    ): Int

    @Modifying
    @NativeQuery("""
        SET CONSTRAINTS uq_category_sort_order DEFERRED 
    """)
    fun deferOrderConstraint()

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Category c WHERE c.id = :id")
    fun findByIdForUpdate(@Param("id") id: Int): Category?

}