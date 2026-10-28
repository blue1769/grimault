package com.blustar.grimault.domain.category.dto

import com.blustar.grimault.domain.category.entity.CategoryItem
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "상세 카테고리 항목 응답")
data class CategoryItemResponse(
    val id: Int,
    val name: String,

    @get:JsonProperty("isActive")
    val isActive: Boolean,
    val sortOrder: Int,
    val isFavorite: Boolean,
) {

    companion object {
        fun from(item: CategoryItem): CategoryItemResponse {
            val verifiedId = requireNotNull(item.id) {
                "Category item id must not be null"
            }

            return CategoryItemResponse(
                id = verifiedId,
                name = item.name,
                isActive = item.isActive,
                sortOrder = item.sortOrder,
                isFavorite = item.isFavorite,
            )
        }
    }

}
