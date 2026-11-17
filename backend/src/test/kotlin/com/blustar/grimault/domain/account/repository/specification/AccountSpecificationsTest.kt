package com.blustar.grimault.domain.account.repository.specification

import com.blustar.grimault.domain.account.entity.Account
import com.blustar.grimault.domain.account.entity.AccountSubtype
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import jakarta.persistence.criteria.CriteriaBuilder
import jakarta.persistence.criteria.CriteriaQuery
import jakarta.persistence.criteria.Path
import jakarta.persistence.criteria.Predicate
import jakarta.persistence.criteria.Root
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test

class AccountSpecificationsTest {

    @Test
    fun `계정 목록 조회 조건에서 CAPITAL 소분류를 제외한다`() {
        // Arrange
        val root = mockk<Root<Account>>()
        val query = mockk<CriteriaQuery<Account>>()
        val criteriaBuilder = mockk<CriteriaBuilder>()
        val subtypePath = mockk<Path<AccountSubtype>>()
        val predicate = mockk<Predicate>()
        every { root.get<AccountSubtype>("subtype") } returns subtypePath
        every { subtypePath.notEqualTo(AccountSubtype.CAPITAL) } returns predicate
        val specification = AccountSpecifications.excludeCapital()

        // Act
        val result = specification.toPredicate(root, query, criteriaBuilder)

        // Assert
        assertSame(predicate, result)
        verify(exactly = 1) { root.get<AccountSubtype>("subtype") }
        verify(exactly = 1) { subtypePath.notEqualTo(AccountSubtype.CAPITAL) }
    }
}
