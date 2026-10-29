package com.blustar.grimault.domain.category.controller

import com.blustar.grimault.domain.category.dto.CategoryResponse
import com.blustar.grimault.domain.category.dto.CategoryTreeResponse
import com.blustar.grimault.domain.category.service.CategoryService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Tag(name = "category", description = "카테고리 관리")
@RestController
@RequestMapping("/categories")
class CategoryController(
    private val service: CategoryService
) {

    @Operation(summary = "전체 분류 계층 조회")
    @GetMapping
    fun findAll(
        @RequestParam(required = false) type: String?,
        @RequestParam(required = false, defaultValue = "false") selectableOnly: Boolean,
    ): List<CategoryTreeResponse> {
        return service.findAll(type, selectableOnly)
    }

    @Operation(summary = "단일 분류 조회")
    @GetMapping("/{id}")
    fun findById(@PathVariable id: Int): CategoryResponse {
        return service.findById(id)
    }

}