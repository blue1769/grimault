package com.blustar.grimault.domain.category.service

import com.blustar.grimault.domain.category.dto.CategoryResponse
import com.blustar.grimault.domain.category.entity.Category
import com.blustar.grimault.domain.category.entity.CategoryItem
import com.blustar.grimault.domain.category.entity.CategoryType
import com.blustar.grimault.domain.category.repository.CategoryOrderLockRepository
import com.blustar.grimault.domain.category.repository.CategoryRepository
import com.blustar.grimault.domain.category.repository.CategoryTreeQueryRepository
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verifySequence
import jakarta.persistence.EntityManager
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.Optional

class CategoryActivationTest {

    private val repository = mockk<CategoryRepository>()
    private val queryRepository = mockk<CategoryTreeQueryRepository>()
    private val orderLockRepository = mockk<CategoryOrderLockRepository>()
    private val entityManager = mockk<EntityManager>()
    private val service = CategoryService(repository, queryRepository, orderLockRepository, entityManager)

    @Test
    fun `비활성 대분류를 활성화한다`() {
        // Arrange
        val items = mutableListOf<CategoryItem>()
        val category = Category(
            id = 10,
            type = CategoryType.EXPENSE,
            name = "식비",
            isActive = false,
            sortOrder = 2,
            items = items,
        )
        val activeItem = CategoryItem(id = 11, category = category, name = "외식", isActive = true, sortOrder = 1)
        val inactiveItem = CategoryItem(id = 12, category = category, name = "간식", isActive = false, sortOrder = 2)
        items.addAll(listOf(activeItem, inactiveItem))
        every { orderLockRepository.lock() } just Runs
        every { repository.findById(10) } returns Optional.of(category)

        // Act
        val response = service.changeActiveStatus(id = 10, active = true)

        // Assert
        assertTrue(category.isActive)
        assertEquals(10, category.id)
        assertEquals("식비", category.name)
        assertEquals(CategoryType.EXPENSE, category.type)
        assertEquals(2, category.sortOrder)
        assertEquals(listOf(activeItem, inactiveItem), category.items)
        assertTrue(activeItem.isActive)
        assertFalse(inactiveItem.isActive)
        assertEquals(CategoryResponse(10, CategoryType.EXPENSE, "식비", true, 2), response)
        verifySequence {
            orderLockRepository.lock()
            repository.findById(10)
        }
    }

    @Test
    fun `활성 대분류를 비활성화한다`() {
        // Arrange
        val items = mutableListOf<CategoryItem>()
        val category = Category(
            id = 10,
            type = CategoryType.EXPENSE,
            name = "식비",
            isActive = true,
            sortOrder = 2,
            items = items,
        )
        val activeItem = CategoryItem(id = 11, category = category, name = "외식", isActive = true, sortOrder = 1)
        val inactiveItem = CategoryItem(id = 12, category = category, name = "간식", isActive = false, sortOrder = 2)
        items.addAll(listOf(activeItem, inactiveItem))
        every { orderLockRepository.lock() } just Runs
        every { repository.findById(10) } returns Optional.of(category)

        // Act
        val response = service.changeActiveStatus(id = 10, active = false)

        // Assert
        assertFalse(category.isActive)
        assertEquals(10, category.id)
        assertEquals("식비", category.name)
        assertEquals(CategoryType.EXPENSE, category.type)
        assertEquals(2, category.sortOrder)
        assertEquals(listOf(activeItem, inactiveItem), category.items)
        assertTrue(activeItem.isActive)
        assertFalse(inactiveItem.isActive)
        assertEquals(CategoryResponse(10, CategoryType.EXPENSE, "식비", false, 2), response)
        verifySequence {
            orderLockRepository.lock()
            repository.findById(10)
        }
    }

    @Test
    fun `이미 동일한 활성 상태로 변경 요청한다`() {
        // Arrange
        val category = Category(
            id = 10,
            type = CategoryType.EXPENSE,
            name = "식비",
            isActive = true,
            sortOrder = 2,
        )
        every { orderLockRepository.lock() } just Runs
        every { repository.findById(10) } returns Optional.of(category)

        // Act
        val firstResponse = service.changeActiveStatus(id = 10, active = true)
        val secondResponse = service.changeActiveStatus(id = 10, active = true)

        // Assert
        assertTrue(category.isActive)
        assertEquals(10, category.id)
        assertEquals("식비", category.name)
        assertEquals(CategoryType.EXPENSE, category.type)
        assertEquals(2, category.sortOrder)
        assertEquals(CategoryResponse(10, CategoryType.EXPENSE, "식비", true, 2), firstResponse)
        assertEquals(firstResponse, secondResponse)
        verifySequence {
            orderLockRepository.lock()
            repository.findById(10)
            orderLockRepository.lock()
            repository.findById(10)
        }
    }
}
