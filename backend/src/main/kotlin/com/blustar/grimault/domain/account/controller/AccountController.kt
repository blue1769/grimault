package com.blustar.grimault.domain.account.controller

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/accounts")
class AccountController {

    @GetMapping
    fun findAll() {
        TODO("call accountService.findAll()")
    }

}
