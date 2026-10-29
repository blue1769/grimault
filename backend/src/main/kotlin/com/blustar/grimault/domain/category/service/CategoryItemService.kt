package com.blustar.grimault.domain.category.service

import com.blustar.grimault.domain.category.dto.CategoryItemResponse
import com.blustar.grimault.domain.category.repository.CategoryItemRepository
import com.blustar.grimault.global.error.ErrorCode
import com.blustar.grimault.global.error.GrimaultException
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service

@Service
class CategoryItemService(
    private val repository: CategoryItemRepository,
) {

    fun findById(id: Int): CategoryItemResponse {
        val item = repository.findByIdOrNull(id)
            ?: throw GrimaultException(ErrorCode.CATEGORY_ITEM_NOT_FOUND, "Category item not found: $id")

        return CategoryItemResponse.from(item)
    }

}