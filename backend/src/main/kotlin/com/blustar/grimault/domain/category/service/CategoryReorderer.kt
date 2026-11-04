package com.blustar.grimault.domain.category.service

import com.blustar.grimault.global.error.ErrorCode
import com.blustar.grimault.global.error.GrimaultException

internal object CategoryReorderer {

    fun compact(pairs: List<Pair<Int, Int>>, removedEntityId: Int): Map<Int, Int> {
        val entries = pairs.map { Entry(it.first, it.second) }

        return compactEntries(entries, removedEntityId)
    }

    private fun compactEntries(entries: List<Entry>, removedEntityId: Int): Map<Int, Int> {
        val removalTarget = entries.firstOrNull { it.entityId == removedEntityId }

        val removedOrder = removalTarget?.sortOrder
            ?: throw GrimaultException(
                ErrorCode.INVALID_CATEGORY_ORDER,
                "Entry not found in ordering list: $removedEntityId"
            )

        val compacted = entries
            .mapNotNull { entry ->
                when {
                    entry.entityId == removedEntityId -> null
                    entry.sortOrder > removedOrder -> entry.copy(sortOrder = entry.sortOrder - 1)
                    else -> entry
                }
            }

        return compacted.associate { it.entityId to it.sortOrder }
    }

    private data class Entry(
        val entityId: Int,
        val sortOrder: Int,
    )
}
