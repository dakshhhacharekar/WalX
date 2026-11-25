package com.example

import com.example.data.DefaultAccountTemplates
import com.example.domain.CurrencyCode
import com.example.domain.FinanceMath
import com.example.domain.PasswordHasher
import com.example.domain.SafeMathEvaluator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WalXDomainUnitTest {

    @Test
    fun safeMathEvaluatorHandlesComplexExpressionsAndPrecision() {
        assertEquals(0.3, SafeMathEvaluator.evaluate("0.1 + 0.2").getOrThrow(), 0.0001)
        assertEquals(165.5, SafeMathEvaluator.evaluate("120 + 45.50").getOrThrow(), 0.0001)
        assertEquals(300.0, SafeMathEvaluator.evaluate("(120 + 30) * 2").getOrThrow(), 0.0001)
        assertEquals(25.33, SafeMathEvaluator.evaluate("76 / 3").getOrThrow(), 0.0001)
        assertTrue(SafeMathEvaluator.evaluate("100 / 0").isFailure)
        assertTrue(SafeMathEvaluator.evaluate("10 + (5 * 2").isFailure)
        assertTrue(SafeMathEvaluator.evaluate("10 + abc").isFailure)
    }

    @Test
    fun financeMathGuardsDivisionByZeroAndFormatsIndianCurrency() {
        assertEquals(0.0, FinanceMath.safePercent(100.0, 0.0), 0.0001)
        assertEquals(0.0, FinanceMath.roundCurrency(Double.NaN), 0.0001)
        assertEquals(0.0, FinanceMath.roundCurrency(Double.POSITIVE_INFINITY), 0.0001)
        assertEquals("₹1,24,500.50", FinanceMath.formatCurrency(124500.50, CurrencyCode.INR))
        assertEquals("$1,250.00", FinanceMath.formatCurrency(1250.0, CurrencyCode.USD))
        assertEquals("¥12,500", FinanceMath.formatCurrency(12500.0, CurrencyCode.JPY))
    }

    @Test
    fun passwordHasherUsesUniqueSaltsAndVerifiesCredentialsAndPin() {
        val hash1 = PasswordHasher.hash("StrongPass123")
        val hash2 = PasswordHasher.hash("StrongPass123")
        assertNotEquals(hash1.saltHex, hash2.saltHex)
        assertNotEquals(hash1.hashHex, hash2.hashHex)
        assertTrue(PasswordHasher.verify("StrongPass123", hash1.hashHex, hash1.saltHex))
        assertFalse(PasswordHasher.verify("WrongPass", hash1.hashHex, hash1.saltHex))

        val pinHash = PasswordHasher.hash("4829")
        assertTrue(PasswordHasher.verify("4829", pinHash.hashHex, pinHash.saltHex))
        assertFalse(PasswordHasher.verify("0000", pinHash.hashHex, pinHash.saltHex))

        assertEquals(0, PasswordHasher.evaluatePasswordStrength(""))
        assertEquals(1, PasswordHasher.evaluatePasswordStrength("abc"))
        assertEquals(4, PasswordHasher.evaluatePasswordStrength("StrongPass123!"))
    }

    @Test
    fun defaultAccountTemplatesScopeRecordsToUserId() {
        val catsA = DefaultAccountTemplates.createDefaultCategories("user_a")
        val catsB = DefaultAccountTemplates.createDefaultCategories("user_b")
        assertEquals(12, catsA.size)
        assertTrue(catsA.all { it.userId == "user_a" })
        assertTrue(catsB.all { it.userId == "user_b" })
        assertTrue(catsA.map { it.id }.intersect(catsB.map { it.id }.toSet()).isEmpty())

        val walletA = DefaultAccountTemplates.createStarterWallet("user_a", 15000.0)
        assertEquals(15000.0, walletA.balance, 0.001)
        assertEquals("user_a", walletA.userId)
    }
}
