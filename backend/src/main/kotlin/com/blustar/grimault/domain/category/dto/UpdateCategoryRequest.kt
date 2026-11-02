package com.blustar.grimault.domain.category.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

@Schema(description = "카테고리 그룹 수정 요청")
data class UpdateCategoryRequest(
    @field:NotBlank
    @field:Size(max = 50)
    val name: String,
)
