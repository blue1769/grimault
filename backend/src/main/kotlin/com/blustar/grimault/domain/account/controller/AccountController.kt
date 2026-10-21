package com.blustar.grimault.domain.account.controller

import com.blustar.grimault.domain.account.dto.AccountResponse
import com.blustar.grimault.domain.account.dto.CreateAccountRequest
import com.blustar.grimault.domain.account.dto.UpdateAccountRequest
import com.blustar.grimault.domain.account.service.AccountService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*

@Tag(name = "Account", description = "금융 계정 관리")
@RestController
@RequestMapping("/accounts")
class AccountController(
    private val service: AccountService
) {

    @Operation(summary = "전체 금융 계정 목록 조회")
    @GetMapping
    fun findAll(
        @Parameter(description = "활성 상태: 미지정 시 전체 조회") @RequestParam(required = false) isActive: Boolean?,
        @Parameter(description = "계정 속성: 미지정 시 전체 조회") @RequestParam(required = false) kinds: List<String>?,
    ): List<AccountResponse> {
        return service.findAll(isActive, kinds)
    }

    @Operation(summary = "단일 금융 계정 조회")
    @GetMapping("/{id}")
    fun findById(@PathVariable id: Int): AccountResponse {
        return service.findById(id)
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(
        @Valid @RequestBody body: CreateAccountRequest,
    ): AccountResponse {
        return service.create(body)
    }

    @PutMapping("/{id}")
    fun update(
        @PathVariable id: Int,
        @Valid @RequestBody body: UpdateAccountRequest,
    ): AccountResponse {
        return service.update(id, body)
    }

}
