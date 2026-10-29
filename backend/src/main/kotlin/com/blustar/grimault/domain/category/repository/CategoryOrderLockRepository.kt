package com.blustar.grimault.domain.category.repository

import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import org.springframework.stereotype.Repository

@Repository
class CategoryOrderLockRepository(
    @PersistenceContext
    private val entityManager: EntityManager,
) {

    fun lock() {
        entityManager.createNativeQuery("SELECT pg_advisory_xact_lock(10001)").singleResult
    }

}