package com.blustar.grimault.domain.account.controller

import com.blustar.grimault.domain.account.dto.AccountResponse
import com.blustar.grimault.domain.account.service.AccountService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/accounts")
class AccountController(
    private val service: AccountService
) {

    @GetMapping
    fun findAll(): List<AccountResponse> {
        return service.findAll()
    }

}
