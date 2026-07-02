package com.nextti.tetherwatch

object GraphScale {
    fun fractions(values: List<Double>, minVisible: Float = 0.08f): List<Float> {
        if (values.isEmpty()) return emptyList()
        val peak = values.maxOrNull()?.takeIf { it > 0.0 } ?: return values.map { minVisible }
        return values.map { ((it / peak).toFloat()).coerceIn(minVisible, 1f) }
    }
}
