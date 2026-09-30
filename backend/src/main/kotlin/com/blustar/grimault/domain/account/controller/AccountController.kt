package com.blustar.grimault.domain.account.controller

import com.blustar.grimault.domain.account.dto.AccountResponse
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/accounts")
class AccountController {

    @GetMapping
    fun findAll(): List<AccountResponse> {
        TODO("call accountService.findAll()")
    }

}
