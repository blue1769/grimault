package com.blustar.grimault.domain.category.service

import com.blustar.grimault.domain.category.dto.*
import com.blustar.grimault.domain.category.entity.Category
import com.blustar.grimault.domain.category.entity.CategoryItem
import com.blustar.grimault.domain.category.entity.CategoryType
import com.blustar.grimault.domain.category.repository.CategoryOrderLockRepository
import com.blustar.grimault.domain.category.repository.CategoryRepository
import com.blustar.grimault.domain.category.repository.CategoryTreeQueryRepository
import com.blustar.grimault.global.error.ErrorCode
import com.blustar.grimault.global.error.GrimaultException
import jakarta.persistence.EntityManager
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class CategoryService(
    private val repository: CategoryRepository,
    private val queryRepository: CategoryTreeQueryRepository,
    private val orderLockRepository: CategoryOrderLockRepository,
    private val entityManager: EntityManager,
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
        val category = find(id)

        return CategoryResponse.from(category)
    }

    @Transactional
    fun create(body: CreateCategoryRequest): CategoryResponse {
        orderLockRepository.lock()

        val nextSortOrder = getMaxOrder() + 1
        val category = repository.save(body.toEntity(nextSortOrder))

        return CategoryResponse.from(category)
    }

    @Transactional
    fun update(id: Int, body: UpdateCategoryRequest): CategoryResponse {
        orderLockRepository.lock()

        val category = find(id)

        category.rename(body.name)

        return CategoryResponse.from(category)
    }

    @Transactional
    fun reorder(id: Int, body: ReorderCategoryEntryRequest): CategoryResponse {
        orderLockRepository.lock()

        val target = body.sortOrder
        val maxOrder = getMaxOrder()

        if (target > maxOrder) {
            throw GrimaultException(
                ErrorCode.INVALID_CATEGORY_ORDER,
                "Target sort order exceeds maximum allowed value: $target (max: $maxOrder)",
            )
        }

        val category = find(id)
        val current = category.sortOrder

        if (current == target) {
            return CategoryResponse.from(category)
        }

        repository.deferOrderConstraint()

        entityManager.flush()

        when {
            current < target -> repository.reorderForward(id, current, target)
            else -> repository.reorderBackward(id, current, target)
        }

        entityManager.clear()

        return CategoryResponse.from(find(id))
    }

    @Transactional
    fun changeActiveStatus(id: Int, active: Boolean): CategoryResponse {
        orderLockRepository.lock()

        val category = find(id)

        if (active) category.activate() else category.deactivate()

        return CategoryResponse.from(category)
    }

    private fun find(id: Int): Category =
        repository.findByIdOrNull(id)
            ?: throw GrimaultException(
                ErrorCode.CATEGORY_NOT_FOUND,
                "Category not found: $id",
            )

    private fun getMaxOrder(): Int =
        repository.findMaxSortOrder() ?: 0

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