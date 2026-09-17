package com.example.sensy

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TimeCalculatorTest {

    private val calculator = TimeCalculator()

    @Test
    fun testNoLimit() {
        val startTime = 1000L
        val currentTime = 5000L
        val duration = -1

        val remaining = calculator.calculateRemainingTimeMillis(startTime, duration, currentTime)
        assertEquals(-1L, remaining)
        assertFalse(calculator.isExpired(startTime, duration, currentTime))
    }

    @Test
    fun testZeroDuration() {
        val startTime = 1000L
        val currentTime = 1000L
        val duration = 0

        val remaining = calculator.calculateRemainingTimeMillis(startTime, duration, currentTime)
        assertEquals(0L, remaining)
        assertTrue(calculator.isExpired(startTime, duration, currentTime))
    }

    @Test
    fun testValidDurationNotExpired() {
        val startTime = 1000L
        val durationMinutes = 5 // 5 * 60 * 1000 = 300,000 ms
        val currentTime = startTime + 100_000L // 100 seconds passed

        val remaining = calculator.calculateRemainingTimeMillis(startTime, durationMinutes, currentTime)
        assertEquals(200_000L, remaining)
        assertFalse(calculator.isExpired(startTime, durationMinutes, currentTime))
    }

    @Test
    fun testValidDurationExpired() {
        val startTime = 1000L
        val durationMinutes = 5 // 5 * 60 * 1000 = 300,000 ms
        val currentTime = startTime + 400_000L // 400 seconds passed, which is > 300s

        val remaining = calculator.calculateRemainingTimeMillis(startTime, durationMinutes, currentTime)
        assertEquals(0L, remaining)
        assertTrue(calculator.isExpired(startTime, durationMinutes, currentTime))
    }

    @Test
    fun testValidDurationExactlyAtExpiration() {
        val startTime = 1000L
        val durationMinutes = 5 // 300,000 ms
        val currentTime = startTime + 300_000L

        val remaining = calculator.calculateRemainingTimeMillis(startTime, durationMinutes, currentTime)
        assertEquals(0L, remaining)
        assertTrue(calculator.isExpired(startTime, durationMinutes, currentTime))
    }
}
