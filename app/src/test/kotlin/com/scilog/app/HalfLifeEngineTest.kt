package com.scilog.app

import com.scilog.app.core.math.HalfLifeEngine
import org.junit.Assert.*
import org.junit.Test

class HalfLifeEngineTest {

    @Test
    fun `remainingLevel returns full dose at time zero`() {
        val result = HalfLifeEngine.remainingLevel(1.0, 168.0, 0.0)
        assertEquals(1.0, result, 0.0001)
    }

    @Test
    fun `remainingLevel returns half dose at one half-life`() {
        val result = HalfLifeEngine.remainingLevel(1.0, 168.0, 168.0)
        assertEquals(0.5, result, 0.001)
    }

    @Test
    fun `remainingLevel returns zero for negative elapsed time`() {
        val result = HalfLifeEngine.remainingLevel(2.0, 168.0, -10.0)
        assertEquals(0.0, result, 0.0)
    }

    @Test
    fun `cumulativeLevel sums contributions from multiple doses`() {
        val now = System.currentTimeMillis()
        val weekAgo = now - 7 * 24 * 3600_000L
        val shots = listOf(weekAgo to 0.25, now to 0.25)
        val level = HalfLifeEngine.cumulativeLevel(shots, now, HalfLifeEngine.SEMAGLUTIDE_HALF_LIFE_HOURS)
        // First shot contributed 0.5 * 0.25 = 0.125, second shot full 0.25 → ~0.375
        assertTrue(level > 0.3 && level < 0.4)
    }

    @Test
    fun `generateDecayCurve produces expected number of points`() {
        val now = System.currentTimeMillis()
        val startMs = now - 24 * 3600_000L
        val endMs = now + 24 * 3600_000L
        val shots = listOf(now to 0.5)
        val curve = HalfLifeEngine.generateDecayCurve(shots, 168.0, startMs, endMs, intervalHours = 4)
        // 48h / 4h = 13 points (inclusive)
        assertEquals(13, curve.size)
    }

    @Test
    fun `tirzepatide decays faster than semaglutide at same dose`() {
        val sema = HalfLifeEngine.remainingLevel(1.0, HalfLifeEngine.SEMAGLUTIDE_HALF_LIFE_HOURS, 120.0)
        val tirz = HalfLifeEngine.remainingLevel(1.0, HalfLifeEngine.TIRZEPATIDE_HALF_LIFE_HOURS, 120.0)
        assertTrue(tirz < sema)
    }
}
