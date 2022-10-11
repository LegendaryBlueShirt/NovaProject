package com.justnopoint.nova

import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath

expect fun getDosboxScancodeMap(): Map<Int, Int>
expect fun getDosboxStagingScancodeMap(): Map<Int, Int>
class DOSBoxConf {
    private val confPath = "omf.conf".toPath()
    private val mapperPath = "omfMap.map".toPath()

    fun writeConfFiles(novaConf: NovaConf) {
        val mapping = if(novaConf.stagingCompat) getDosboxStagingScancodeMap() else getDosboxScancodeMap()
        writeDosboxMapperFile(mapping, novaConf.p1Config, novaConf.p2Config)
        writeDosboxConfFile(novaConf)
    }

    fun getConfPath(): Path {
        return FileSystem.SYSTEM.canonicalize(confPath)
    }

    private fun writeDosboxMapperFile(mapping: Map<Int, Int>, p1Config: ControlMapping, p2Config: ControlMapping) {
        val boundScancodes = listOf(p1Config.up, p1Config.down, p1Config.left, p1Config.right, p1Config.punch, p1Config.kick,
            p2Config.up, p2Config.down, p2Config.left, p2Config.right, p2Config.punch, p2Config.kick)
            .filter { it.type == ControlType.KEY }.map { it.scancode }
        val availableKeys = getPossibleMappings().keys.filterNot { boundScancodes.contains(it) }
        val availableKeyQueue = ArrayDeque(availableKeys)
        val fs = FileSystem.SYSTEM
        fs.delete(path = mapperPath, mustExist = false)
        fs.write(file = mapperPath, mustCreate = true) {
            writeUtf8("key_esc \"key ${mapping[27]}\"\n")
            writeUtf8("key_1 \"key ${mapping[49]}\"\n")
            writeUtf8("key_2 \"key ${mapping[50]}\"\n")
            writeUtf8("key_3 \"key ${mapping[51]}\"\n")
            writeUtf8("key_4 \"key ${mapping[52]}\"\n")
            writeUtf8("key_5 \"key ${mapping[53]}\"\n")
            writeUtf8("key_6 \"key ${mapping[54]}\"\n")
            p1Config.apply {
                writeUtf8("key_up \"${up.dosboxMap(mapping, availableKeyQueue)}\"\n")
                writeUtf8("key_down \"${down.dosboxMap(mapping, availableKeyQueue)}\"\n")
                writeUtf8("key_left \"${left.dosboxMap(mapping, availableKeyQueue)}\"\n")
                writeUtf8("key_right \"${right.dosboxMap(mapping, availableKeyQueue)}\"\n")
                writeUtf8("key_enter \"${punch.dosboxMap(mapping, availableKeyQueue)}\"\n")
                writeUtf8("key_rshift \"${kick.dosboxMap(mapping, availableKeyQueue)}\"\n")
            }
            p2Config.apply {
                writeUtf8("key_w \"${up.dosboxMap(mapping, availableKeyQueue)}\"\n")
                writeUtf8("key_x \"${down.dosboxMap(mapping, availableKeyQueue)}\"\n")
                writeUtf8("key_a \"${left.dosboxMap(mapping, availableKeyQueue)}\"\n")
                writeUtf8("key_d \"${right.dosboxMap(mapping, availableKeyQueue)}\"\n")
                writeUtf8("key_tab \"${punch.dosboxMap(mapping, availableKeyQueue)}\"\n")
                writeUtf8("key_lctrl \"${kick.dosboxMap(mapping, availableKeyQueue)}\"\n")
            }
        }
    }

    private fun writeDosboxConfFile(novaConf: NovaConf) {
        val fs = FileSystem.SYSTEM
        val absoluteMapperPath = fs.canonicalize(mapperPath)
        fs.delete(path = confPath, mustExist = false)
        fs.write(file = confPath, mustCreate = true) {
            writeUtf8(
                "[sdl]\nwaitonerror=false\nmapperfile=${absoluteMapperPath}\npriority=normal,normal\n"
            )
            writeUtf8(
                "[dosbox]\nmachine=svga_s3\nvmemsize=8\nmemsize=16\n"
            )
            writeUtf8(
                "[cpu]\ncycles=fixed 14500\n"
            )
//            if(novaConf.joyEnabled) {
//                val joyType = if (novaConf.isUsingHat()) "fcs" else "auto" //DOSBox only supports hat mappings in fcs mode.
//                writeUtf8(
//                    "[joystick]\njoysticktype=$joyType\ntimed=true\n" //Trying out timed=true, doesn't seem to matter.
//                )
//            } else {
                writeUtf8(
                    "[joystick]\njoysticktype=none\n"
                )
            //}
            if(SoundCard.isGravisEnabled(novaConf.omfPath.toPath())) {
                writeUtf8("[gus]\ngus=true\n")
            }
        }
    }
}

//fun ButtonMap.dosboxMap(scancodeMap: Map<Int, Int>): String {
//    return when(type) {
//        ControlType.KEY -> {
//            "key ${scancodeMap[scancode]}"
//        }
//        ControlType.AXIS -> {
//            "stick_$controlId axis $axisId $direction"
//        }
//        ControlType.HAT -> {
//            "stick_$controlId hat $axisId $direction"
//        }
//        ControlType.BUTTON -> {
//            "stick_$controlId button $axisId"
//        }
//    }
//}

fun ButtonMap.dosboxMap(scancodeMap: Map<Int, Int>, availableCodes: ArrayDeque<Int>): String {
    return if(type == ControlType.KEY) {
        "key ${scancodeMap[scancode]}"
    } else {
        "key ${scancodeMap[availableCodes.removeFirst()]}"
    }
}
expect fun getPossibleMappings(): Map<Int, Any>