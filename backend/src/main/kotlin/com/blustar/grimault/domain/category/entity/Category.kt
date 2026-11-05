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

    name: String,

    @Column(name = "is_active")
    val isActive: Boolean = true,

    sortOrder: Int,

    @OneToMany(mappedBy = "category", fetch = FetchType.LAZY)
    val items: List<CategoryItem> = emptyList(),
) {

    @Column(name = "name")
    var name: String = name
        protected set

    @Column(name = "sort_order")
    var sortOrder: Int = sortOrder
        protected set

    fun rename(newName: String) {
        name = newName
    }

}
