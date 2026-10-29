package com.blustar.grimault.global.error

import org.hibernate.exception.ConstraintViolationException
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.MethodArgumentNotValidException
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

    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleHttpMessageNotReadableException(error: HttpMessageNotReadableException): ResponseEntity<ErrorResponse> {
        val status = HttpStatus.BAD_REQUEST
        val body = ErrorResponse.of(status, "Invalid request body")

        return ResponseEntity(body, status)
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleMethodArgumentNotValidException(error: MethodArgumentNotValidException): ResponseEntity<ErrorResponse> {
        val status = HttpStatus.BAD_REQUEST

        val message = error.bindingResult.allErrors
            .firstOrNull()
            ?.defaultMessage
            ?: "Invalid request body"

        val body = ErrorResponse.of(status, message)

        return ResponseEntity(body, status)
    }

    @ExceptionHandler(DataIntegrityViolationException::class)
    fun handleDataIntegrityViolationException(error: DataIntegrityViolationException): ResponseEntity<ErrorResponse> {
        val constraintViolation = generateSequence(error as Throwable?) { it.cause }
            .filterIsInstance<ConstraintViolationException>()
            .firstOrNull()

        val constraintName = constraintViolation?.constraintName

        val (status, message) = when (constraintName) {
            "account_name_key" -> HttpStatus.CONFLICT to "Account name already exists"
            "category_name_key" -> HttpStatus.CONFLICT to "Category name already exists"
            "uq_category_item_name" -> HttpStatus.CONFLICT to "Category item name already exists in this category"
            else -> HttpStatus.INTERNAL_SERVER_ERROR to "Data integrity violation (constraint: ${constraintName ?: "unknown"})"
        }

        val body = ErrorResponse.of(status, message)

        return ResponseEntity(body, status)
    }

    @ExceptionHandler(GrimaultException::class)
    fun handleGrimaultException(error: GrimaultException): ResponseEntity<ErrorResponse> {
        val errorCode = error.errorCode
        val body = ErrorResponse.of(errorCode, error.message)

        return ResponseEntity(body, errorCode.status)
    }

    @ExceptionHandler(NotImplementedError::class)
    fun handleNotImplementError(error: NotImplementedError): ResponseEntity<ErrorResponse> {
        val status = HttpStatus.NOT_IMPLEMENTED
        val body = ErrorResponse.of(status, error.message)

        return ResponseEntity(body, status)
    }

}
