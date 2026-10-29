package com.blustar.grimault.domain.category.dto

import com.blustar.grimault.domain.category.entity.Category
import com.blustar.grimault.domain.category.entity.CategoryItem
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
    val items: List<CategoryTreeItemResponse>,
) {

    companion object {
        fun from(category: Category, items: List<CategoryTreeItemResponse>): CategoryTreeResponse {
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

@Schema(description = "카테고리 트리 아이템 응답")
data class CategoryTreeItemResponse(
    val id: Int,
    val name: String,

    @get:JsonProperty("isActive")
    val isActive: Boolean,
    val sortOrder: Int,
    val isFavorite: Boolean,
) {

    companion object {
        fun from(item: CategoryItem): CategoryTreeItemResponse {
            val verifiedId = requireNotNull(item.id) {
                "Category item id must not be null"
            }

            return CategoryTreeItemResponse(
                id = verifiedId,
                name = item.name,
                isActive = item.isActive,
                sortOrder = item.sortOrder,
                isFavorite = item.isFavorite,
            )
        }
    }

}
