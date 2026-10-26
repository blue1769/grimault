package com.blustar.grimault.domain.account.entity

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

class AccountTest {

    @Test
    fun `결제일만 수정하면 기존 청구 기간을 유지한다`() {
        // Arrange
        val originalBillingPeriod = CreditCardBillingPeriod(
            startMonthOffset = -1,
            startDay = 1,
            endMonthOffset = 0,
            endDay = 15,
        )
        val account = Account(
            name = "신한카드",
            type = AccountType.LIABILITY,
            subtype = AccountSubtype.CREDIT_CARD,
            creditCardMetadata = CreditCardMetadata(
                issuer = CreditCardIssuer.SHINHAN,
                billingPeriod = originalBillingPeriod,
                settlementDay = 15,
            ),
        )

        // Act
        account.updateCreditCardDetails(settlementDay = 20, billingPeriod = null)

        // Assert
        val metadata = account.creditCardMetadata
        assertNotNull(metadata)
        assertEquals(20, metadata?.settlementDay)
        assertEquals(originalBillingPeriod, metadata?.billingPeriod)
        assertEquals(CreditCardIssuer.SHINHAN, metadata?.issuer)
    }

    @Test
    fun `청구 기간만 수정하면 기존 결제일을 유지한다`() {
        // Arrange
        val account = Account(
            name = "신한카드",
            type = AccountType.LIABILITY,
            subtype = AccountSubtype.CREDIT_CARD,
            creditCardMetadata = CreditCardMetadata(
                issuer = CreditCardIssuer.SHINHAN,
                billingPeriod = CreditCardBillingPeriod(
                    startMonthOffset = -1,
                    startDay = 1,
                    endMonthOffset = 0,
                    endDay = 15,
                ),
                settlementDay = 15,
            ),
        )
        val updatedBillingPeriod = CreditCardBillingPeriod(
            startMonthOffset = -1,
            startDay = 5,
            endMonthOffset = 0,
            endDay = 20,
        )

        // Act
        account.updateCreditCardDetails(settlementDay = null, billingPeriod = updatedBillingPeriod)

        // Assert
        val metadata = account.creditCardMetadata
        assertNotNull(metadata)
        assertEquals(updatedBillingPeriod, metadata?.billingPeriod)
        assertEquals(15, metadata?.settlementDay)
        assertEquals(CreditCardIssuer.SHINHAN, metadata?.issuer)
    }
}
