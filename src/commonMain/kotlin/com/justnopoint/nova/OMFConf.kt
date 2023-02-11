@file:OptIn(ExperimentalUnsignedTypes::class, ExperimentalUnsignedTypes::class)

package com.justnopoint.nova

import okio.*

class OMFConf(buffer: BufferedSource) {
    var speed = 8
    var difficulty = 0
    var throwRange = 100
    var hitPause = 4
    var blockDamage = 0
    var vitality = 100
    var jumpHeight = 100
    val flags: OmfFlags = UByteArray(8)
    var sound = 10
    var music = 10

    private val controlSettings = ByteArray(211)
    private var p1ControlType: Int = -1
    private val unknowns = ByteArray(18)
    private val unkFooter = ByteArray(38)

    companion object {
        const val FILENAME = "SETUP.CFG"
        fun isValid(handle: FileHandle): Boolean {
            return true //TODO
        }
    }

    init {
        try {
            speed = buffer.readByte().toUByte().toInt()
            buffer.read(controlSettings)
            p1ControlType = buffer.readShortLe().toInt()
            buffer.skip(4)
            buffer.read(unknowns)
            difficulty = buffer.readShortLe().toInt()
            throwRange = buffer.readShortLe().toInt()
            hitPause = buffer.readShortLe().toInt()
            blockDamage = buffer.readShortLe().toInt()
            vitality = buffer.readShortLe().toInt()
            jumpHeight = buffer.readShortLe().toInt()
            buffer.readByteArray(8).toUByteArray().copyInto(flags)
            sound = buffer.readByte().toUByte().toInt()
            music = buffer.readByte().toUByte().toInt()
            buffer.read(unkFooter)
        } catch (e: Exception) {
            showErrorPopup("Error loading OMF configuration", e.message?:"Unknown Error")
        }
    }

    fun buildFile(buffer: BufferedSink, isSinglePlayer: Boolean = false) {
        buffer.writeByte(speed)
        buffer.write(controlSettings)
        if(isSinglePlayer) {
            buffer.writeShortLe(8)
            buffer.writeShortLe(7)
            buffer.writeShortLe(7)
        } else {
            buffer.writeShortLe(8)
            buffer.writeShortLe(7)
            buffer.writeShortLe(8)
        }
        buffer.write(unknowns)
        buffer.writeShortLe(difficulty)
        buffer.writeShortLe(throwRange)
        buffer.writeShortLe(hitPause)
        buffer.writeShortLe(blockDamage)
        buffer.writeShortLe(vitality)
        buffer.writeShortLe(jumpHeight)
        buffer.write(flags.toByteArray())
        buffer.writeByte(sound)
        buffer.writeByte(music)
        buffer.write(unkFooter)
    }

    var p1power: Int
        get() {
            return flags[0].toInt() shr 5
        }
        set(power) {
            val rest = flags[0].toInt() and 0x1F
            val newPower = (power and 0x7) shl 5
            flags[0] = (rest or newPower).toUByte()
        }

    var p2power: Int
        get() {
            return (flags[1].toInt() shr 2) and 0x7
        }
        set(power) {
            val rest = flags[1].toInt() and 0xE3
            val newPower = (power and 0x7) shl 2
            flags[1] = (rest or newPower).toUByte()
        }

    var hyperMode: Boolean
        get() {
            return (flags[2].toInt() and 0x20) != 0
        }
        set(value) {
            var rest = flags[2].toInt() and 0xDF
            if(value) {
                rest = rest or 0x20
            }
            flags[2] = rest.toUByte()
        }

    var rehitMode: Boolean
        get() {
            return (flags[0].toInt() and 0x04) != 0
        }
        set(value) {
            var rest = flags[0].toInt() and 0xFB
            if(value) {
                rest = rest or 0x04
            }
            flags[0] = rest.toUByte()
        }

    var hazards: Boolean
        get() {
            return (flags[2].toInt() and 0x10) != 0
        }
        set(value) {
            var rest = flags[2].toInt() and 0xEF
            if(value) {
                rest = rest or 0x10
            }
            flags[2] = rest.toUByte()
        }

    var animations: Boolean
        get() {
            return (flags[2].toInt() and 0x80) != 0
        }
        set(value) {
            var rest = flags[2].toInt() and 0x7F
            if(value) {
                rest = rest or 0x80
            }
            flags[2] = rest.toUByte()
        }

    var screenShakes: Boolean
        get() {
            return (flags[2].toInt() and 0x40) != 0
        }
        set(value) {
            var rest = flags[2].toInt() and 0xBF
            if(value) {
                rest = rest or 0x40
            }
            flags[2] = rest.toUByte()
        }

    //var shadows: Int

}

typealias OmfFlags = UByteArray

fun OMFConf.isDefensiveThrows(): Boolean {
    return (flags[0].toInt() and 0x08) != 0
}

fun OMFConf.setDefensiveThrows(value: Boolean) {
    var rest = flags[0].toInt() and 0xF7
    if(value) {
        rest = rest or 0x08
    }
    flags[0] = rest.toUByte()
}

fun OMFConf.getKnockdownMode(): Int {
    return flags[0].toInt() and 0x03
}

fun OMFConf.getRoundCount(): Int {
    return (flags[4].toInt() shr 1) and 0x03
}