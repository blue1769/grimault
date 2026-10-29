package com.blustar.grimault.domain.category.dto

import com.blustar.grimault.domain.category.entity.Category
import com.blustar.grimault.domain.category.entity.CategoryType
import com.blustar.grimault.domain.category.validation.ValidCreateCategoryRequest
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

@Schema(description = "카테고리 그룹 생성 요청")
@ValidCreateCategoryRequest
data class CreateCategoryRequest(
    @field:NotBlank
    @field:Size(max = 50)
    val name: String,

    @field:NotBlank
    val type: String,
) {

    fun toEntity(sortOrder: Int): Category = Category(
        type = requireNotNull(CategoryType.fromNameOrNull(type.trim())),
        name = name,
        isActive = true,
        sortOrder = sortOrder,
    )

}
