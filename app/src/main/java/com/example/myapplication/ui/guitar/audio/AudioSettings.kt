package com.example.myapplication.ui.guitar.audio

data class AudioSettings(
    val decay: Float = 0.99f,
    val thickness: Float = 1.0f
) {
    init {
        require(decay in 0.990f..0.99995f) { "decay must be in [0.990, 0.99995]" }
        require(thickness in 0f..1f) { "thickness must be in [0, 1]" }
    }
}
