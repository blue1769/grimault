package com.blustar.grimault.domain.account.repository

import com.blustar.grimault.domain.account.entity.Account
import org.springframework.data.jpa.repository.JpaRepository

interface AccountRepository : JpaRepository<Account, Int>
