package com.blustar.grimault.global.error

import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatus.BAD_REQUEST

enum class ErrorCode(
    val status: HttpStatus,
    val message: String,
) {

    INVALID_ACCOUNT_KIND(BAD_REQUEST, "Invalid account kind"),

}