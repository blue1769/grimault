package com.blustar.grimault.domain.ledger.repository

import com.blustar.grimault.domain.ledger.entity.LedgerEntry
import org.springframework.data.jpa.repository.JpaRepository

interface LedgerEntryRepository : JpaRepository<LedgerEntry, Long>