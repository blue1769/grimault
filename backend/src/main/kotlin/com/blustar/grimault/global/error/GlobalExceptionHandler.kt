package com.blustar.grimault.global.error

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(NotImplementedError::class)
    fun handleNotImplementError(error: NotImplementedError): ResponseEntity<ErrorResponse> {
        val status = HttpStatus.NOT_IMPLEMENTED

        val body = ErrorResponse(
            status = status.value(),
            code = status.name,
            message = error.message ?: "Not yet implemented"
        )

        return ResponseEntity(body, status)
    }

}
