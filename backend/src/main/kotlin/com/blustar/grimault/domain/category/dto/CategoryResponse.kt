package com.blustar.grimault.domain.category.dto

import com.blustar.grimault.domain.category.entity.Category
import com.blustar.grimault.domain.category.entity.CategoryType
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "카테고리 그룹 항목 응답")
data class CategoryResponse(
    val id: Int,
    val type: CategoryType,
    val name: String,

    @get:JsonProperty("isActive")
    val isActive: Boolean,
    val sortOrder: Int,
) {

    companion object {
        fun from(category: Category): CategoryResponse {
            val verifiedId = requireNotNull(category.id) {
                "Category id must not be null"
            }

            return CategoryResponse(
                id = verifiedId,
                type = category.type,
                name = category.name,
                isActive = category.isActive,
                sortOrder = category.sortOrder,
            )
        }
    }

}
