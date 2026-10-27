package com.blustar.grimault.domain.category.entity

import jakarta.persistence.*

@Entity
@Table(name = "category_item")
class CategoryItem(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Int? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    val category: Category,

    @Column(name = "name")
    val name: String,

    @Column(name = "is_active")
    val isActive: Boolean = true,

    @Column(name = "sort_order")
    val sortOrder: Int,

    @Column(name = "is_favorite")
    val isFavorite: Boolean = false,
)
