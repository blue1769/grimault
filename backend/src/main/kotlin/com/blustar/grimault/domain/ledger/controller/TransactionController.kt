package com.blustar.grimault.domain.ledger.controller

import com.blustar.grimault.domain.ledger.dto.CreateTransactionRequest
import com.blustar.grimault.domain.ledger.dto.TransactionResponse
import com.blustar.grimault.domain.ledger.service.TransactionService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.support.ServletUriComponentsBuilder

@Tag(name = "Transaction", description = "금융 거래 관리")
@RestController
@RequestMapping("/transactions")
class TransactionController(
    private val service: TransactionService,
) {

    @Operation(summary = "신규 금융 거래 작성")
    @PostMapping
    fun create(
        @Valid @RequestBody body: CreateTransactionRequest,
    ): ResponseEntity<TransactionResponse> {
        val transaction = service.create(body)

        val location = ServletUriComponentsBuilder
            .fromCurrentRequest()
            .path("/{id}")
            .buildAndExpand(transaction.id)
            .toUri()

        return ResponseEntity
            .created(location)
            .body(transaction)
    }

}
