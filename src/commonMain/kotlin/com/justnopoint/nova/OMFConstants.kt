package com.justnopoint.nova

// Important Pointers
const val p1PilotPointer = 0x1F7978L
const val p2PilotPointer = 0x1F797CL

val p1struct = PilotStruct(0)
val p2struct = PilotStruct(0)

val p1har = HarStruct(0x215038L)
val p2har = HarStruct(0x220038L)

val video = VideoStruct(0x1FEB5CL)

// Pilot Struct
class PilotStruct(var ptrAddr: Long) {
    val name get() = ptrAddr+4
    val harId get() = ptrAddr+27
    val enchancementLevel get() = ptrAddr+166
    val hp get() = ptrAddr+264
    val hpMax get() = ptrAddr+266
}

// HAR Struct
class HarStruct(private val ptrAddr: Long) {
    val harId get() = ptrAddr+48
    val anim get() = ptrAddr+76
    val stun get() = ptrAddr+136
    val posX get() = ptrAddr+168
    val posY get() = ptrAddr+172
    val velY get() = ptrAddr+176
    val velX get() = ptrAddr+180
    val enemyPtr get() = ptrAddr+248
}

class VideoStruct(private val ptrAddr: Long) {
    val palette get() = ptrAddr+6687
}


const val videoCorner = 0x2460F0L
const val vsScreenValue = 2189656707u // 0x82838283

// Good numbers to have
const val p1StartXPosition = 28160u
const val p2StartXPosition = 53760u
const val groundYPosition = 48640u
const val cornerLeftP1 = 5120u
const val cornerLeftP2 = 12825u
const val cornerRightP1 = 69120u
const val cornerRightP2 = 76800u