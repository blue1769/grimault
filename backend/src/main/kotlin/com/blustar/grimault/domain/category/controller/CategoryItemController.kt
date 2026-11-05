package com.blustar.grimault.domain.category.controller

import com.blustar.grimault.domain.category.dto.CategoryItemResponse
import com.blustar.grimault.domain.category.dto.ReorderCategoryEntryRequest
import com.blustar.grimault.domain.category.dto.UpdateCategoryItemRequest
import com.blustar.grimault.domain.category.service.CategoryItemService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.*

@Tag(name = "Category", description = "상세 카테고리 관리")
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

    @Operation(summary = "상세 카테고리 수정")
    @PatchMapping("/{id}")
    fun update(
        @PathVariable id: Int,
        @Valid @RequestBody body: UpdateCategoryItemRequest,
    ): CategoryItemResponse {
        return service.update(id, body)
    }

    @Operation(summary = "상세 카테고리 순번 변경")
    @PatchMapping("/{id}/order")
    fun reorder(
        @PathVariable id: Int,
        @Valid @RequestBody body: ReorderCategoryEntryRequest,
    ): CategoryItemResponse {
        return service.reorder(id, body)
    }

}