package com.blustar.grimault.domain.category.dto

import com.blustar.grimault.domain.category.entity.Category
import com.blustar.grimault.domain.category.entity.CategoryItem
import com.blustar.grimault.domain.category.validation.ValidCreateCategoryItemRequest
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

@Schema(description = "상세 카테고리 생성 요청")
@ValidCreateCategoryItemRequest
data class CreateCategoryItemRequest(
    @field:NotBlank
    @field:Size(max = 50)
    val name: String,
) {

    fun toEntity(category: Category, sortOrder: Int): CategoryItem = CategoryItem(
        category = category,
        name = name,
        isActive = true,
        sortOrder = sortOrder,
        isFavorite = false,
    )

}
