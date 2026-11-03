package com.blustar.grimault.domain.category.service

import com.blustar.grimault.global.error.ErrorCode
import com.blustar.grimault.global.error.GrimaultException

internal object CategoryItemReorderer {

    fun compact(pairs: List<Pair<Int, Int>>, removedItemId: Int): Map<Int, Int> {
        val items = pairs.map { Item(it.first, it.second) }

        return compactItems(items, removedItemId)
    }

    private fun compactItems(sortedItems: List<Item>, removedItemId: Int): Map<Int, Int> {
        val removalTarget = sortedItems.firstOrNull { it.id == removedItemId }

        val removedOrder = removalTarget?.sortOrder
            ?: throw GrimaultException(
                ErrorCode.INVALID_CATEGORY_ITEM_ORDER,
                "Item not found in ordering list: $removedItemId"
            )

        val compacted = sortedItems
            .mapNotNull { item ->
                when {
                    item.id == removedItemId -> null
                    item.sortOrder > removedOrder -> item.copy(sortOrder = item.sortOrder - 1)
                    else -> item
                }
            }

        return compacted.associate { it.id to it.sortOrder }
    }

    private data class Item(
        val id: Int,
        val sortOrder: Int,
    )
}
