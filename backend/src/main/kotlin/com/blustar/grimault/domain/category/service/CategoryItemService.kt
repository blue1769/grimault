package com.blustar.grimault.domain.category.service

import com.blustar.grimault.domain.category.dto.CategoryItemResponse
import com.blustar.grimault.domain.category.dto.CreateCategoryItemRequest
import com.blustar.grimault.domain.category.repository.CategoryItemRepository
import com.blustar.grimault.domain.category.repository.CategoryRepository
import com.blustar.grimault.global.error.ErrorCode
import com.blustar.grimault.global.error.GrimaultException
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class CategoryItemService(
    private val repository: CategoryItemRepository,
    private val groupRepository: CategoryRepository,
) {

    fun findById(id: Int): CategoryItemResponse {
        val item = repository.findByIdOrNull(id)
            ?: throw GrimaultException(ErrorCode.CATEGORY_ITEM_NOT_FOUND, "Category item not found: $id")

        return CategoryItemResponse.from(item)
    }

    @Transactional
    fun create(
        categoryId: Int,
        body: CreateCategoryItemRequest,
    ): CategoryItemResponse {
        val category = groupRepository.findByIdForUpdate(categoryId)
            ?: throw GrimaultException(ErrorCode.CATEGORY_NOT_FOUND, "Category not found: $categoryId")

        val maxSortOrder = repository.findMaxSortOrder(categoryId) ?: 0
        val nextSortOrder = maxSortOrder + 1

        val item = repository.save(body.toEntity(category, nextSortOrder))

        return CategoryItemResponse.from(item)
    }

}