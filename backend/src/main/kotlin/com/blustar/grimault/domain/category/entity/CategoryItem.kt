package com.blustar.grimault.domain.category.entity

import jakarta.persistence.*

@Entity
@Table(name = "category_item")
class CategoryItem(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Int? = null,

    category: Category,

    name: String,

    @Column(name = "is_active")
    val isActive: Boolean = true,

    sortOrder: Int,

    isFavorite: Boolean = false,
) {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    var category: Category = category
        protected set

    @Column(name = "name")
    var name: String = name
        protected set

    @Column(name = "sort_order")
    var sortOrder: Int = sortOrder
        protected set

    @Column(name = "is_favorite")
    var isFavorite = isFavorite
        protected set

    fun moveTo(otherCategory: Category) {
        category = otherCategory
    }

    fun rename(newName: String) {
        name = newName
    }

    fun reorder(newOrder: Int) {
        sortOrder = newOrder
    }

    fun favorite(state: Boolean) {
        isFavorite = state
    }

}
