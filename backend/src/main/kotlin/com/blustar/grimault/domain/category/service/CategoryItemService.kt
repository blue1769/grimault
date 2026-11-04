package com.blustar.grimault.domain.category.service

import com.blustar.grimault.domain.category.dto.CategoryItemResponse
import com.blustar.grimault.domain.category.dto.CreateCategoryItemRequest
import com.blustar.grimault.domain.category.dto.UpdateCategoryItemRequest
import com.blustar.grimault.domain.category.entity.Category
import com.blustar.grimault.domain.category.entity.CategoryItem
import com.blustar.grimault.domain.category.repository.CategoryItemRepository
import com.blustar.grimault.domain.category.repository.CategoryRepository
import com.blustar.grimault.global.error.ErrorCode
import com.blustar.grimault.global.error.GrimaultException
import jakarta.persistence.EntityManager
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class CategoryItemService(
    private val repository: CategoryItemRepository,
    private val groupRepository: CategoryRepository,
    private val entityManager: EntityManager,
) {

    fun findById(id: Int): CategoryItemResponse {
        return CategoryItemResponse.from(find(id))
    }

    @Transactional
    fun create(
        categoryId: Int,
        body: CreateCategoryItemRequest,
    ): CategoryItemResponse {
        val category = findGroup(categoryId)
        val nextSortOrder = getNextOrder(categoryId)
        val item = repository.save(body.toEntity(category, nextSortOrder))

        return CategoryItemResponse.from(item)
    }

    @Transactional
    fun update(
        id: Int,
        body: UpdateCategoryItemRequest,
    ): CategoryItemResponse {
        val item = find(id)
        val observedGroupId = requireNotNull(item.category.id)

        val groups = listOfNotNull(observedGroupId, body.categoryId)
            .distinct()
            .sorted()
            .map { findGroup(it) }
            .associateBy { requireNotNull(it.id) }

        entityManager.refresh(item)

        val currentGroupId = requireNotNull(item.category.id)

        if (currentGroupId != observedGroupId) {
            throw GrimaultException(
                ErrorCode.CATEGORY_ITEM_UPDATE_CONFLICT,
                "Category item moved concurrently: $id",
            )
        }

        body.categoryId?.let { targetGroupId ->
            if (targetGroupId == currentGroupId) {
                throw GrimaultException(
                    ErrorCode.INVALID_CATEGORY_ITEM_MOVE,
                    "Category item already belongs to category: ${body.categoryId}",
                )
            }

            moveGroup(
                item = item,
                originGroup = groups.getValue(currentGroupId),
                targetGroup = groups.getValue(targetGroupId),
            )
        }

        body.name?.let { item.rename(it) }
        body.isFavorite?.let { item.favorite(it) }

        return CategoryItemResponse.from(item)
    }

    private fun find(id: Int): CategoryItem =
        repository.findByIdOrNull(id)
            ?: throw GrimaultException(
                ErrorCode.CATEGORY_ITEM_NOT_FOUND,
                "Category item not found: $id",
            )

    private fun findGroup(groupId: Int): Category =
        groupRepository.findByIdForUpdate(groupId)
            ?: throw GrimaultException(
                ErrorCode.CATEGORY_NOT_FOUND,
                "Category not found: $groupId",
            )

    private fun moveGroup(
        item: CategoryItem,
        originGroup: Category,
        targetGroup: Category,
    ) {
        if (originGroup.type != targetGroup.type) {
            throw GrimaultException(
                ErrorCode.INVALID_CATEGORY_TYPE,
                "Category type mismatch: ${originGroup.type} -> ${targetGroup.type}",
            )
        }

        val targetGroupId = requireNotNull(targetGroup.id)
        val targetItemOrder = getNextOrder(targetGroupId)

        val originItems = originGroup.items

        val reordered = CategoryReorderer.compact(
            originItems.map { requireNotNull(it.id) to it.sortOrder },
            requireNotNull(item.id),
        )

        repository.deferOrderConstraint()

        originItems.forEach { sibling ->
            reordered[sibling.id]?.let { sibling.reorder(it) }
        }

        item.moveTo(targetGroup)
        item.reorder(targetItemOrder)
    }

    private fun getNextOrder(categoryId: Int): Int =
        (repository.findMaxSortOrder(categoryId) ?: 0) + 1

}
