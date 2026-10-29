package com.blustar.grimault.domain.category.service

import com.blustar.grimault.domain.category.dto.CategoryResponse
import com.blustar.grimault.domain.category.dto.CategoryTreeItemResponse
import com.blustar.grimault.domain.category.dto.CategoryTreeResponse
import com.blustar.grimault.domain.category.entity.Category
import com.blustar.grimault.domain.category.entity.CategoryItem
import com.blustar.grimault.domain.category.entity.CategoryType
import com.blustar.grimault.domain.category.repository.CategoryRepository
import com.blustar.grimault.domain.category.repository.CategoryTreeQueryRepository
import com.blustar.grimault.global.error.ErrorCode
import com.blustar.grimault.global.error.GrimaultException
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class CategoryService(
    private val repository: CategoryRepository,
    private val queryRepository: CategoryTreeQueryRepository,
) {

    fun findAll(
        typeName: String? = null,
        selectableOnly: Boolean = false,
    ): List<CategoryTreeResponse> {
        val type = typeName?.let {
            CategoryType.fromNameOrNull(typeName)
                ?: throw GrimaultException(ErrorCode.INVALID_CATEGORY_TYPE, "Invalid category type: $typeName")
        }

        return queryRepository
            .findAll(type, selectableOnly)
            .toResponses(CategoryTreeItemResponse::from)
    }

    fun findById(id: Int): CategoryResponse {
        val category = repository.findByIdOrNull(id)
            ?: throw GrimaultException(ErrorCode.CATEGORY_NOT_FOUND, "Category not found: $id")

        return CategoryResponse.from(category)
    }

    private fun List<Pair<Category, CategoryItem?>>.toResponses(
        itemMapper: (CategoryItem) -> CategoryTreeItemResponse,
    ): List<CategoryTreeResponse> =
        groupBy { (category, _) -> category.id }
            .values
            .map { group ->
                CategoryTreeResponse.from(
                    group.first().first,
                    group.mapNotNull { (_, item) -> item?.let(itemMapper) },
                )
            }

}