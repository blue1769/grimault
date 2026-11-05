package com.blustar.grimault.domain.category.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive

@Schema(description = "카테고리 순번 관리")
data class ReorderCategoryEntryRequest(
    @field:NotNull
    @field:Positive
    val sortOrder: Int,
)
