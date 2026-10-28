package com.blustar.grimault.domain.category.service

import com.blustar.grimault.domain.category.dto.CategoryItemResponse
import com.blustar.grimault.domain.category.dto.CategoryResponse
import com.blustar.grimault.domain.category.entity.Category
import com.blustar.grimault.domain.category.entity.CategoryItem
import com.blustar.grimault.domain.category.entity.CategoryType
import com.blustar.grimault.domain.category.repository.CategoryTreeQueryRepository
import com.blustar.grimault.global.error.ErrorCode
import com.blustar.grimault.global.error.GrimaultException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class CategoryService(
    private val queryRepository: CategoryTreeQueryRepository,
) {

    fun findAll(
        typeName: String? = null,
        selectableOnly: Boolean = false,
    ): List<CategoryResponse> {
        val type = typeName?.let {
            CategoryType.fromNameOrNull(typeName)
                ?: throw GrimaultException(ErrorCode.INVALID_CATEGORY_TYPE, "Invalid category type: $typeName")
        }

        return queryRepository
            .findAll(type, selectableOnly)
            .toResponses(CategoryItemResponse::from)
    }

    private fun List<Pair<Category, CategoryItem?>>.toResponses(
        itemMapper: (CategoryItem) -> CategoryItemResponse,
    ): List<CategoryResponse> =
        groupBy { (category, _) -> category.id }
            .values
            .map { group ->
                CategoryResponse.from(
                    group.first().first,
                    group.mapNotNull { (_, item) -> item?.let(itemMapper) },
                )
            }

}