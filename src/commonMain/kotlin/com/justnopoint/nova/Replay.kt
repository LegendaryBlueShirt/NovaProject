package com.justnopoint.nova

import okio.*
import kotlin.experimental.xor

class Replay(data: BufferedSource, val path: Path, val timestamp: Long) {
    companion object {
        fun loadReplay(file: Path): Replay {
            val fs = FileSystem.SYSTEM
            val metadata = fs.metadata(file)
            val timestamp = metadata.createdAtMillis?:metadata.lastModifiedAtMillis?:0L
            val replay = fs.read(file = file) {
                Replay(this, file, timestamp)
            }
            return replay
        }

        val Stages = listOf(
            "Stadium",
            "Danger Room",
            "Power Plant",
            "Fire Pit",
            "Desert"
        )

        val Hars = listOf(
            "Jaguar",
            "Shadow",
            "Thorn",
            "Pyros",
            "Electra",
            "Katana",
            "Shredder",
            "Flail",
            "Gargoyle",
            "Chronos",
            "Nova"
        )

        val Rounds = listOf(
            "Best of One",
            "Best 2 of 3",
            "Best 3 of 5",
            "Best 4 of 7"
        )
    }

    val stage
        get() = Stages[selectedStage]

    val p1Har
        get() = Hars[p1HarId]

    val p2Har
        get() = Hars[p2HarId]

    val lastTick
        get() = events.last().timeCode

    fun getRounds(): String {
        return Rounds[rounds]
    }

    val p1Name: String
    private val p1HarId: Int
    val p2Name: String
    private val p2HarId: Int

    private val p1Score: Int
    private val p2Score: Int
    private val throwRangePercent: Int
    private val hitPause: Int
    private val blockDamagePercent: Int
    private val vitalityPercent: Int
    private val jumpHeightPercent: Int
    private val p1Power: Int
    private val p2Power: Int
    private val selectedStage: Int
    private val defensiveThrows: Boolean
    val rehit: Boolean
    private val knockdowns: Int
    private val events: List<EventDescriptor>
    val hazards: Boolean
    val hyperMode: Boolean
    private val rounds: Int

    init {
        val p1data = data.readByteArray(428)
        for(n in 0 until 428) {
            val value = ((428+n) and 0xFF).toByte()
            p1data[n] = p1data[n] xor value
        }
        data.skip(167)
        val hasPortrait1 = data.readByte()
        val nameBytes = ByteArray(18)
        p1data.copyInto(destination = nameBytes, startIndex = 4, endIndex = 22)
        p1Name = nameBytes.decodeToString().takeWhile { it.isLetterOrDigit() }
        p1HarId = p1data[27].toInt()

        val p2data = data.readByteArray(428)
        for(n in 0 until 428) {
            val value = ((428+n) and 0xFF).toByte()
            p2data[n] = p2data[n] xor value
        }
        data.skip(167)
        val hasPortrait2 = data.readByte()
        p2data.copyInto(destination = nameBytes, startIndex = 4, endIndex = 22)
        p2Name = nameBytes.decodeToString().takeWhile { it.isLetterOrDigit() }
        p2HarId = p2data[27].toInt()

        p1Score = data.readIntLe()
        p2Score = data.readIntLe()
        data.skip(3)
        throwRangePercent = data.readShortLe().toUShort().toInt()
        hitPause = data.readShortLe().toUShort().toInt()
        blockDamagePercent = data.readShortLe().toUShort().toInt()
        vitalityPercent = data.readShortLe().toUShort().toInt()
        jumpHeightPercent = data.readShortLe().toUShort().toInt()
        data.skip(6)
        val stageFlagsKnockdown = data.readByte().toUByte().toInt()
        selectedStage = stageFlagsKnockdown shr 4
        defensiveThrows = stageFlagsKnockdown and 0x8 == 0x8
        rehit = stageFlagsKnockdown and 0x4 == 0x4
        knockdowns = stageFlagsKnockdown and 0x3
        val powerAndFlags = data.readShortLe().toUShort().toInt()
        rounds = (powerAndFlags shr 12) and 0x3
        hazards = (powerAndFlags shr 11) and 0x1 == 1
        p1Power = (powerAndFlags shr 6) and 0x7
        p2Power = (powerAndFlags shr 1) and 0x7
        hyperMode = data.readByte().toUByte().toInt() and 0x1 == 1
        data.skip(1)
        val currentEvents = mutableListOf<EventDescriptor>()
        while(!data.exhausted()) {
            val timeCode = data.readIntLe()
            val extraFlag = data.readByte().toUByte().toInt()
            val playerId = data.readByte().toUByte().toInt()
            val flags = data.readByte().toUByte().toInt()
            val extraData = if(extraFlag > 2) data.readByteArray(7) else ByteArray(0)
            currentEvents.add(EventDescriptor(timeCode, extraFlag, playerId, flags, extraData))
        }
        events = currentEvents
    }

    data class EventDescriptor(val timeCode: Int, val extraFlag: Int, val playerId: Int, val flags: Int, val extraData: ByteArray)
}