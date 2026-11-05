package com.blustar.grimault.domain.category.service

import com.blustar.grimault.domain.category.dto.CategoryItemResponse
import com.blustar.grimault.domain.category.dto.CreateCategoryItemRequest
import com.blustar.grimault.domain.category.dto.ReorderCategoryEntryRequest
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
        val category = findGroupWithLock(categoryId)
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
            .map { findGroupWithLock(it) }
            .associateBy { requireNotNull(it.id) }

        entityManager.refresh(item)

        val currentGroupId = requireNotNull(item.category.id)

        if (currentGroupId != observedGroupId) {
            throw GrimaultException(
                ErrorCode.CATEGORY_ITEM_UPDATE_CONFLICT,
                "Category item moved concurrently: $id",
            )
        }

        val groupMove = body.categoryId?.let { targetGroupId ->
            if (targetGroupId == currentGroupId) {
                throw GrimaultException(
                    ErrorCode.INVALID_CATEGORY_ITEM_MOVE,
                    "Category item already belongs to category: ${body.categoryId}",
                )
            }

            prepareGroupMove(item, groups.getValue(targetGroupId))
        }

        if (groupMove != null) {
            repository.deferOrderConstraint()
            item.moveTo(groupMove.targetGroup)
            item.reorder(groupMove.targetItemOrder)
        }

        body.name?.let { item.rename(it) }
        body.isFavorite?.let { item.favorite(it) }

        if (groupMove != null) {
            entityManager.flush()
            repository.compact(groupMove.originGroupId, groupMove.originItemOrder)
            entityManager.clear()
        }

        return CategoryItemResponse.from(item)
    }

    @Transactional
    fun reorder(id: Int, body: ReorderCategoryEntryRequest): CategoryItemResponse {
        val item = find(id)
        val observedGroupId = requireNotNull(item.category.id)

        findGroupWithLock(observedGroupId)

        entityManager.refresh(item)

        val currentGroupId = requireNotNull(item.category.id)

        if (currentGroupId != observedGroupId) {
            throw GrimaultException(
                ErrorCode.CATEGORY_ITEM_UPDATE_CONFLICT,
                "Category item moved concurrently: $id",
            )
        }

        val target = body.sortOrder
        val maxOrder = getMaxOrder(observedGroupId)

        if (target > maxOrder) {
            throw GrimaultException(
                ErrorCode.INVALID_CATEGORY_ORDER,
                "Target sort order exceeds maximum allowed value: $target (max: $maxOrder)",
            )
        }

        val current = item.sortOrder

        if (current == target) {
            return CategoryItemResponse.from(item)
        }

        repository.deferOrderConstraint()

        entityManager.flush()

        when {
            current < target -> repository.reorderForward(id, currentGroupId, current, target)
            else -> repository.reorderBackward(id, currentGroupId, current, target)
        }

        entityManager.clear()

        return CategoryItemResponse.from(find(id))
    }

    @Transactional
    fun changeActiveStatus(id: Int, active: Boolean): CategoryItemResponse {
        val item = find(id)
        val observedGroupId = requireNotNull(item.category.id)

        findGroupWithLock(observedGroupId)

        entityManager.refresh(item)

        val currentGroupId = requireNotNull(item.category.id)

        if (currentGroupId != observedGroupId) {
            throw GrimaultException(
                ErrorCode.CATEGORY_ITEM_UPDATE_CONFLICT,
                "Category item moved concurrently: $id",
            )
        }

        if (active) item.activate() else item.deactivate()

        return CategoryItemResponse.from(item)
    }

    private fun find(id: Int): CategoryItem =
        repository.findByIdOrNull(id)
            ?: throw GrimaultException(
                ErrorCode.CATEGORY_ITEM_NOT_FOUND,
                "Category item not found: $id",
            )

    private fun findGroupWithLock(groupId: Int): Category =
        groupRepository.findByIdForUpdate(groupId)
            ?: throw GrimaultException(
                ErrorCode.CATEGORY_NOT_FOUND,
                "Category not found: $groupId",
            )

    private fun getMaxOrder(categoryId: Int): Int =
        repository.findMaxSortOrder(categoryId) ?: 0

    private fun getNextOrder(categoryId: Int): Int =
        getMaxOrder(categoryId) + 1

    private fun prepareGroupMove(item: CategoryItem, targetGroup: Category): GroupMove {
        val originGroup = requireNotNull(item.category)

        if (originGroup.type != targetGroup.type) {
            throw GrimaultException(
                ErrorCode.INVALID_CATEGORY_TYPE,
                "Category type mismatch: ${originGroup.type} -> ${targetGroup.type}",
            )
        }

        return GroupMove(
            originGroupId = requireNotNull(originGroup.id),
            originItemOrder = item.sortOrder,
            targetGroup = targetGroup,
            targetItemOrder = getNextOrder(requireNotNull(targetGroup.id)),
        )
    }

    private data class GroupMove(
        val originGroupId: Int,
        val originItemOrder: Int,
        val targetGroup: Category,
        val targetItemOrder: Int,
    )

}
