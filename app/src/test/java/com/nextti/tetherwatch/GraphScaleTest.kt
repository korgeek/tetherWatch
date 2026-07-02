package com.nextti.tetherwatch

import org.junit.Assert.assertEquals
import org.junit.Test

class GraphScaleTest {
    @Test
    fun emptyInputReturnsEmptyFractions() {
        assertEquals(emptyList<Float>(), GraphScale.fractions(emptyList()))
    }

    @Test
    fun zeroInputKeepsVisibleBaseline() {
        assertEquals(listOf(0.08f, 0.08f), GraphScale.fractions(listOf(0.0, 0.0)))
    }

    @Test
    fun valuesScaleAgainstPeak() {
        assertEquals(listOf(0.25f, 0.5f, 1.0f), GraphScale.fractions(listOf(25.0, 50.0, 100.0)))
    }
}
