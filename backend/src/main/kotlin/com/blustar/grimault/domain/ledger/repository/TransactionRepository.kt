package com.blustar.grimault.domain.ledger.repository

import com.blustar.grimault.domain.ledger.entity.Transaction
import org.springframework.data.jpa.repository.JpaRepository

interface TransactionRepository : JpaRepository<Transaction, Long>