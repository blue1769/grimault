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

    @Column(name = "parent_name")
    val parentName: String,

    @Column(name = "sub_name")
    val subName: String,

    @Column(name = "is_active")
    val isActive: Boolean = true,
)
