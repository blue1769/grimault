package com.blustar.grimault.global.error

import java.time.OffsetDateTime

data class ErrorResponse(
    val status: Int,
    val code: String,
    val message: String,
    val timestamp: OffsetDateTime = OffsetDateTime.now()
)
