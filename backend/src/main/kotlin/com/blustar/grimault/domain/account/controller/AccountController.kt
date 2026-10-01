package com.blustar.grimault.domain.account.controller

import com.blustar.grimault.domain.account.dto.AccountResponse
import com.blustar.grimault.domain.account.service.AccountService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Tag(name = "Account", description = "금융 계정 관리")
@RestController
@RequestMapping("/accounts")
class AccountController(
    private val service: AccountService
) {

    @Operation(summary = "전체 금융 계정 목록 조회")
    @GetMapping
    fun findAll(): List<AccountResponse> {
        return service.findAll()
    }

}
