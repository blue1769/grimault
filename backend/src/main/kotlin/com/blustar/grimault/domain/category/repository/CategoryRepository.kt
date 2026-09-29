package com.blustar.grimault.domain.category.repository

import com.blustar.grimault.domain.category.entity.Category
import org.springframework.data.jpa.repository.JpaRepository

interface CategoryRepository : JpaRepository<Category, Int>