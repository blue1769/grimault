package com.blustar.grimault.domain.account.dto

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate

class CreateAccountRequestTest {

    @Test
    fun `원장 시작일을 생략하면 계정 등록일을 기본값으로 사용한다`() {
        // Arrange
        val request = CreateAccountRequest(
            name = "테스트 은행 계정",
            type = "ASSET",
            subtype = "BANK",
        )

        // Act
        val before = LocalDate.now()
        val account = request.toEntity()
        val after = LocalDate.now()

        // Assert
        assertTrue(
            !account.openingDate.isBefore(before) && !account.openingDate.isAfter(after),
            "원장 시작일 ${account.openingDate}은 호출 직전 $before 과 직후 $after 사이여야 한다",
        )
    }
}
