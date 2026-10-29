package com.blustar.grimault.domain.category.controller

import com.blustar.grimault.domain.category.dto.CategoryItemResponse
import com.blustar.grimault.domain.category.service.CategoryItemService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Tag(name = "Category", description = "카테고리 소분류 관리")
@RestController
@RequestMapping("/category-items")
class CategoryItemController(
    private val service: CategoryItemService
) {

    @Operation(summary = "단일 소분류 조회")
    @GetMapping("/{id}")
    fun findById(@PathVariable id: Int): CategoryItemResponse {
        return service.findById(id)
    }

}