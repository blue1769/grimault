package com.blustar.grimault.domain.category.controller

import com.blustar.grimault.domain.category.dto.*
import com.blustar.grimault.domain.category.service.CategoryItemService
import com.blustar.grimault.domain.category.service.CategoryService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.servlet.support.ServletUriComponentsBuilder

@Tag(name = "category", description = "카테고리 관리")
@RestController
@RequestMapping("/categories")
class CategoryController(
    private val service: CategoryService,
    private val itemService: CategoryItemService,
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

    @PostMapping
    fun create(
        @Valid @RequestBody body: CreateCategoryRequest,
    ): ResponseEntity<CategoryResponse> {
        val category = service.create(body)

        val location = ServletUriComponentsBuilder
            .fromCurrentRequest()
            .path("/{id}")
            .buildAndExpand(category.id)
            .toUri()

        return ResponseEntity.created(location).body(category)
    }

    @PostMapping("/{id}/items")
    fun createNewItem(
        @PathVariable id: Int,
        @Valid @RequestBody body: CreateCategoryItemRequest,
    ): ResponseEntity<CategoryItemResponse> {
        val item = itemService.create(id, body)

        val location = ServletUriComponentsBuilder
            .fromCurrentContextPath()
            .path("/category-items/{id}")
            .buildAndExpand(item.id)
            .toUri()

        return ResponseEntity.created(location).body(item)
    }

}