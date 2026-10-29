package com.blustar.grimault.domain.category.dto

import com.blustar.grimault.domain.category.entity.Category
import com.blustar.grimault.domain.category.entity.CategoryType
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "카테고리 트리 응답")
data class CategoryTreeResponse(
    val id: Int,
    val type: CategoryType,
    val name: String,

    @get:JsonProperty("isActive")
    val isActive: Boolean,
    val sortOrder: Int,
    val items: List<CategoryItemResponse>,
) {

    companion object {
        fun from(category: Category, items: List<CategoryItemResponse>): CategoryTreeResponse {
            val verifiedId = requireNotNull(category.id) {
                "Category id must not be null"
            }

            return CategoryTreeResponse(
                id = verifiedId,
                type = category.type,
                name = category.name,
                isActive = category.isActive,
                sortOrder = category.sortOrder,
                items = items
            )
        }
    }

}
