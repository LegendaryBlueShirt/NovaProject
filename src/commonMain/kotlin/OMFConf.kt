@file:OptIn(ExperimentalUnsignedTypes::class, ExperimentalUnsignedTypes::class)

import okio.FileHandle
import okio.buffer
import okio.use

class OMFConf(handle: FileHandle) {
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
        fun isValid(handle: FileHandle): Boolean {
            return true //TODO
        }
    }

    init {
        handle.source().buffer().use { buffer ->
            handle.reposition(buffer, 0)

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
        }
    }

    fun buildFile(handle: FileHandle) {
        handle.sink().buffer().use {
            handle.reposition(it, 0)
            it.writeByte(speed)
            it.write(controlSettings)
            it.writeShortLe(p1ControlType)
            it.writeShortLe(7)
            it.writeShortLe(8)
            it.write(unknowns)
            it.writeShortLe(difficulty)
            it.writeShortLe(throwRange)
            it.writeShortLe(hitPause)
            it.writeShortLe(blockDamage)
            it.writeShortLe(vitality)
            it.writeShortLe(jumpHeight)
            it.write(flags.toByteArray())
            it.writeByte(sound)
            it.writeByte(music)
            it.write(unkFooter)
        }
    }

    fun printGameOptions(window: NovaWindow) {
        val p1PowerString = "${1 / (2.25 - .25*p1power)}".take(5)
        val p2PowerString = "${1 / (2.25 - .25*p2power)}".take(5)
        window.clearText()
        window.showText("=======  Game Options   =======")
        window.showText("1. Hyper Mode - ${if (hyperMode) "On" else "Off"}")
        window.showText("2. Rehit Mode - ${if (rehitMode) "On" else "Off"}")
        window.showText("3. Hazards    - ${if (hazards) "On" else "Off"}")
        window.showText("4. Speed      - ${speed*10}%")
        window.showText("5. P1 Power   - ${p1PowerString}x")
        window.showText("6. P2 Power   - ${p2PowerString}x")
        window.showText("7. Back")
    }

    fun printAVOptions(window: NovaWindow) {
        window.clearText()
        window.showText("===== AudioVisual Options =====")
        window.showText("1. Hyper Mode - ${if (hyperMode) "On" else "Off"}")
        window.showText("2. Rehit Mode - ${if (rehitMode) "On" else "Off"}")
        window.showText("3. Hazards    - ${if (hazards) "On" else "Off"}")
        window.showText("4. Speed      - ${speed*10}%")
        window.showText("5. Sound   - ${(0 until sound).joinToString(separator = "") { "*" }}")
        window.showText("6. Music   - ${(0 until music).joinToString(separator = "") { "*" }}")
        window.showText("7. Back")
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