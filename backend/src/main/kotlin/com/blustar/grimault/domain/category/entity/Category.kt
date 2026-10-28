package com.blustar.grimault.domain.category.entity

import jakarta.persistence.*

@Entity
@Table(name = "category")
class Category(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Int? = null,

    @Column(name = "type")
    @Enumerated(EnumType.STRING)
    val type: CategoryType,

    @Column(name = "name")
    val name: String,

    @Column(name = "is_active")
    val isActive: Boolean = true,

    @Column(name = "sort_order")
    val sortOrder: Int,

    @OneToMany(mappedBy = "category", fetch = FetchType.LAZY)
    val items: List<CategoryItem> = emptyList(),
)
