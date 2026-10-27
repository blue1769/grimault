package com.blustar.grimault.domain.category.repository

import com.blustar.grimault.domain.category.entity.CategoryItem
import org.springframework.data.jpa.repository.JpaRepository

interface CategoryItemRepository : JpaRepository<CategoryItem, Int>
