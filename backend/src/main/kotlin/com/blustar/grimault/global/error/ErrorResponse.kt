package com.blustar.grimault.global.error

import org.springframework.http.HttpStatus
import java.time.OffsetDateTime

data class ErrorResponse(
    val status: Int,
    val code: String,
    val message: String,
    val timestamp: OffsetDateTime = OffsetDateTime.now(),
) {

    companion object {
        fun of(status: HttpStatus, message: String?): ErrorResponse {
            return ErrorResponse(
                status = status.value(),
                code = status.name,
                message = message ?: status.reasonPhrase
            )
        }
    }

}
