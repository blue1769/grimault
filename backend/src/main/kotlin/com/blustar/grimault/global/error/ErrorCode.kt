package com.blustar.grimault.global.error

import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatus.BAD_REQUEST
import org.springframework.http.HttpStatus.NOT_FOUND

enum class ErrorCode(
    val status: HttpStatus,
    val message: String,
) {

    ACCOUNT_NOT_FOUND(NOT_FOUND, "Account not found"),
    INACTIVE_ACCOUNT_UPDATE(BAD_REQUEST, "Inactive account update"),
    INVALID_ACCOUNT_KIND(BAD_REQUEST, "Invalid account kind"),
    INVALID_ACCOUNT_UPDATE(BAD_REQUEST, "Invalid account update"),

}