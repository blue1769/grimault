package com.blustar.grimault.global.error

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException

@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentTypeMismatchException::class)
    fun handleMethodArgumentTypeMismatchException(error: MethodArgumentTypeMismatchException): ResponseEntity<ErrorResponse> {
        val status = HttpStatus.BAD_REQUEST
        val body = ErrorResponse.of(status, "Invalid value for parameter: ${error.name}")

        return ResponseEntity(body, status)
    }

    @ExceptionHandler(NotImplementedError::class)
    fun handleNotImplementError(error: NotImplementedError): ResponseEntity<ErrorResponse> {
        val status = HttpStatus.NOT_IMPLEMENTED
        val body = ErrorResponse.of(status, error.message)

        return ResponseEntity(body, status)
    }

    @ExceptionHandler(GrimaultException::class)
    fun handleGrimaultException(error: GrimaultException): ResponseEntity<ErrorResponse> {
        val errorCode = error.errorCode
        val body = ErrorResponse.of(errorCode, error.message)

        return ResponseEntity(body, errorCode.status)
    }

}
