package com.justnopoint.nova

import kotlin.system.getTimeMillis

fun getTrainingModeInput(code: Int) {
    when(code) {
        //VIRT_F1 -> resetLeftSide()
        //VIRT_F2 -> resetCenter()
        //VIRT_F3 -> resetRightSide()
    }
}

fun getTrainingModeButton(input: ButtonMap) {
    mapping?.apply {
        when (input) {
            resetLeft -> resetLeftSide()
            resetCenter -> resetCenter()
            resetRight -> resetRightSide()
            record -> toggleDummyRecording()
            playback -> recordingPlayback()
        }
    }
}

fun resetLeftSide() {
    resetCharacters(hp = true, stun = true, state = true)
    writeMemoryInt(p1har.posY, groundYPosition)
    writeMemoryInt(p2har.posY, groundYPosition)
    writeMemoryInt(p1har.posX, cornerLeftP1)
    writeMemoryInt(p2har.posX, cornerLeftP2)
}

fun resetCenter() {
    resetCharacters(hp = true, stun = true, state = true)
    writeMemoryInt(p1har.posY, groundYPosition)
    writeMemoryInt(p2har.posY, groundYPosition)
    writeMemoryInt(p1har.posX, p1StartXPosition)
    writeMemoryInt(p2har.posX, p2StartXPosition)
}

fun resetRightSide() {
    resetCharacters(hp = true, stun = true, state = true)
    writeMemoryInt(p1har.posY, groundYPosition)
    writeMemoryInt(p2har.posY, groundYPosition)
    writeMemoryInt(p1har.posX, cornerRightP1)
    writeMemoryInt(p2har.posX, cornerRightP2)
}

fun resetCharacters(hp: Boolean, stun: Boolean, state: Boolean) {
    p1struct.ptrAddr = readMemoryInt(p1PilotPointer).toLong()
    p2struct.ptrAddr = readMemoryInt(p2PilotPointer).toLong()

    if(hp) {
        var maxHp = readMemoryShort(p1struct.hpMax)
        writeMemoryShort(p1struct.hp, maxHp)
        maxHp = readMemoryShort(p2struct.hpMax)
        writeMemoryShort(p2struct.hp, maxHp)
    }
    if(stun) {
        writeMemoryInt(p1har.stun, 0u)
        writeMemoryInt(p2har.stun, 0u)
    }
    if(state) {
        writeMemoryByte(p1har.anim, 11u)
        writeMemoryByte(p2har.anim, 11u)
    }
}

fun isMatchStarted(): Boolean {
    return readMemoryInt(p1har.enemyPtr) != 0u
}

fun isVsScreen(): Boolean {
    return readMemoryInt(videoCorner) == vsScreenValue
}

var dummyActive = false
var recordingActive = false
var dummyPlaybackIndex = -1
var lastTimestamp = 0L
val recordedInputs = mutableListOf<RecordedInput>()
var mapping: TrainingMapping? = null

fun toggleDummyRecording() {
    dummyPlaybackIndex = -1
    if(recordingActive) {
        dummyActive = false
        recordingActive = false
    } else {
        dummyActive = true
        recordingActive = true
        lastTimestamp = getTimeMillis()
        recordedInputs.clear()
    }
}

fun recordingPlayback() {
    println("Playback, ${recordedInputs.size} recorded inputs")
    recordingActive = false
    dummyActive = false
    dummyPlaybackIndex = 0
    lastTimestamp = getTimeMillis()
}

fun doDummyPlayback(): RecordedInput? {
    if(dummyPlaybackIndex == -1) return null
    val timestamp = getTimeMillis() - lastTimestamp
    val move = recordedInputs[dummyPlaybackIndex]
    return if(timestamp >= move.delay) {
        dummyPlaybackIndex++
        if(dummyPlaybackIndex == recordedInputs.size) {
            dummyPlaybackIndex = -1
            println("Playback finished")
        }
        move
    } else {
        null
    }
}

fun setTrainingInputs(trainingMapping: TrainingMapping) {
    mapping = trainingMapping
}

fun recordInput(mappedButton: ButtonMap, up: Boolean) {
    val timestamp = getTimeMillis() - lastTimestamp
    recordedInputs.add(RecordedInput(mappedButton, up, timestamp))
}

data class RecordedInput(val mappedButton: ButtonMap, val up: Boolean, val delay: Long)

expect fun readMemoryByte(address: Long): UByte
expect fun readMemoryShort(address: Long): UShort
expect fun readMemoryInt(address: Long): UInt
expect fun writeMemoryInt(address: Long, value: UInt)
expect fun writeMemoryShort(address: Long, value: UShort)
expect fun writeMemoryByte(address: Long, value: UByte)
expect fun writeMemoryString(address: Long, value: String, limit: Int)