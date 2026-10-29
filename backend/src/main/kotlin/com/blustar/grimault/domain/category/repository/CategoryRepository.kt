package com.blustar.grimault.domain.category.repository

import com.blustar.grimault.domain.category.entity.Category
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface CategoryRepository : JpaRepository<Category, Int> {

    @Query("SELECT MAX(c.sortOrder) FROM Category c")
    fun findMaxSortOrder(): Int?

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Category c WHERE c.id = :id")
    fun findByIdForUpdate(@Param("id") id: Int): Category?

}