package com.blustar.grimault.domain.category.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.Size

@Schema(description = "상세 카테고리 수정 요청")
data class UpdateCategoryItemRequest(
    @field:Size(max = 50)
    @field:Pattern(regexp = ".*\\S.*", message = "Name must not be blank")
    val name: String? = null,

    @field:Positive
    val categoryId: Int? = null,

    val isFavorite: Boolean? = null,
)
