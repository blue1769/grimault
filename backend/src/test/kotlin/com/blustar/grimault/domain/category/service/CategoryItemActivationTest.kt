package com.blustar.grimault.domain.category.service

import com.blustar.grimault.domain.category.dto.CategoryItemResponse
import com.blustar.grimault.domain.category.entity.Category
import com.blustar.grimault.domain.category.entity.CategoryItem
import com.blustar.grimault.domain.category.entity.CategoryType
import com.blustar.grimault.domain.category.repository.CategoryItemRepository
import com.blustar.grimault.domain.category.repository.CategoryRepository
import com.blustar.grimault.global.error.ErrorCode
import com.blustar.grimault.global.error.GrimaultException
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verifySequence
import jakarta.persistence.EntityManager
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.Optional

class CategoryItemActivationTest {

    private val repository = mockk<CategoryItemRepository>()
    private val groupRepository = mockk<CategoryRepository>()
    private val entityManager = mockk<EntityManager>()
    private val service = CategoryItemService(repository, groupRepository, entityManager)

    @Test
    fun `비활성 소분류를 활성화한다`() {
        // Arrange
        val items = mutableListOf<CategoryItem>()
        val category = Category(
            id = 1, type = CategoryType.EXPENSE, name = "식비",
            isActive = true, sortOrder = 3, items = items,
        )
        val item = CategoryItem(
            id = 10, category = category, name = "외식",
            isActive = false, sortOrder = 2, isFavorite = true,
        )
        items.add(item)
        every { repository.findById(10) } returns Optional.of(item)
        every { groupRepository.findByIdForUpdate(1) } returns category
        every { entityManager.refresh(item) } just Runs

        // Act
        val response = service.changeActiveStatus(id = 10, active = true)

        // Assert
        assertTrue(item.isActive)
        assertEquals(10, item.id)
        assertEquals("외식", item.name)
        assertSame(category, item.category)
        assertEquals(2, item.sortOrder)
        assertTrue(item.isFavorite)
        assertEquals(1, category.id)
        assertEquals(CategoryType.EXPENSE, category.type)
        assertEquals("식비", category.name)
        assertTrue(category.isActive)
        assertEquals(3, category.sortOrder)
        assertSame(items, category.items)
        assertEquals(listOf(item), category.items)
        assertEquals(CategoryItemResponse(10, 1, "외식", true, 2, true), response)
        verifySequence {
            repository.findById(10)
            groupRepository.findByIdForUpdate(1)
            entityManager.refresh(item)
        }
    }

    @Test
    fun `활성 소분류를 비활성화한다`() {
        // Arrange
        val items = mutableListOf<CategoryItem>()
        val category = Category(
            id = 1, type = CategoryType.EXPENSE, name = "식비",
            isActive = true, sortOrder = 3, items = items,
        )
        val item = CategoryItem(
            id = 10, category = category, name = "외식",
            isActive = true, sortOrder = 2, isFavorite = true,
        )
        items.add(item)
        every { repository.findById(10) } returns Optional.of(item)
        every { groupRepository.findByIdForUpdate(1) } returns category
        every { entityManager.refresh(item) } just Runs

        // Act
        val response = service.changeActiveStatus(id = 10, active = false)

        // Assert
        assertFalse(item.isActive)
        assertEquals(10, item.id)
        assertEquals("외식", item.name)
        assertSame(category, item.category)
        assertEquals(2, item.sortOrder)
        assertTrue(item.isFavorite)
        assertEquals(1, category.id)
        assertEquals(CategoryType.EXPENSE, category.type)
        assertEquals("식비", category.name)
        assertTrue(category.isActive)
        assertEquals(3, category.sortOrder)
        assertSame(items, category.items)
        assertEquals(listOf(item), category.items)
        assertEquals(CategoryItemResponse(10, 1, "외식", false, 2, true), response)
        verifySequence {
            repository.findById(10)
            groupRepository.findByIdForUpdate(1)
            entityManager.refresh(item)
        }
    }

    @Test
    fun `이미 동일한 활성 상태로 변경 요청한다`() {
        // Arrange
        val items = mutableListOf<CategoryItem>()
        val category = Category(
            id = 1, type = CategoryType.EXPENSE, name = "식비",
            isActive = true, sortOrder = 3, items = items,
        )
        val item = CategoryItem(
            id = 10, category = category, name = "외식",
            isActive = true, sortOrder = 2, isFavorite = true,
        )
        items.add(item)
        every { repository.findById(10) } returns Optional.of(item)
        every { groupRepository.findByIdForUpdate(1) } returns category
        every { entityManager.refresh(item) } just Runs

        // Act
        val response = service.changeActiveStatus(id = 10, active = true)
        val secondResponse = service.changeActiveStatus(id = 10, active = true)

        // Assert
        assertTrue(item.isActive)
        assertEquals(10, item.id)
        assertEquals("외식", item.name)
        assertSame(category, item.category)
        assertEquals(2, item.sortOrder)
        assertTrue(item.isFavorite)
        assertEquals(1, category.id)
        assertEquals(CategoryType.EXPENSE, category.type)
        assertEquals("식비", category.name)
        assertTrue(category.isActive)
        assertEquals(3, category.sortOrder)
        assertSame(items, category.items)
        assertEquals(listOf(item), category.items)
        assertEquals(CategoryItemResponse(10, 1, "외식", true, 2, true), response)
        assertEquals(response, secondResponse)
        verifySequence {
            repository.findById(10)
            groupRepository.findByIdForUpdate(1)
            entityManager.refresh(item)
            repository.findById(10)
            groupRepository.findByIdForUpdate(1)
            entityManager.refresh(item)
        }
    }

    @Test
    fun `잠금 획득 중 소속 대분류가 변경되면 충돌로 처리한다`() {
        // Arrange
        val origin = Category(id = 1, type = CategoryType.EXPENSE, name = "식비", sortOrder = 1)
        val target = Category(id = 2, type = CategoryType.EXPENSE, name = "생활비", sortOrder = 2)
        val item = CategoryItem(
            id = 10, category = origin, name = "외식", isActive = false, sortOrder = 2,
        )
        every { repository.findById(10) } returns Optional.of(item)
        every { groupRepository.findByIdForUpdate(1) } returns origin
        // 실제 DB 경합이 아닌 refresh 이후의 소속 변경 상태를 모형화한다.
        every { entityManager.refresh(item) } answers { item.moveTo(target) }

        // Act
        val exception = assertThrows(GrimaultException::class.java) {
            service.changeActiveStatus(id = 10, active = true)
        }

        // Assert
        assertEquals(ErrorCode.CATEGORY_ITEM_UPDATE_CONFLICT, exception.errorCode)
        assertSame(target, item.category)
        assertFalse(item.isActive)
        verifySequence {
            repository.findById(10)
            groupRepository.findByIdForUpdate(1)
            entityManager.refresh(item)
        }
    }
}
