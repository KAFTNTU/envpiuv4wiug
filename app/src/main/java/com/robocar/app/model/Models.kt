package com.robocar.app.model

sealed class MoveRecord {
    data class Move(val m1: Int, val m2: Int, val m3: Int, val m4: Int) : MoveRecord()
    data class Wait(val sec: Double) : MoveRecord()
}

data class TuningSettings(
    val invertL: Boolean = false,
    val invertR: Boolean = false,
    val trim: Int = 0,
    val turnSens: Int = 100,
    val use4Motors: Boolean = false,
    val useSlip: Boolean = true
)
