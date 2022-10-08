package com.justnopoint.nova

import okio.Path
import okio.Path.Companion.toPath
import okio.buffer
import okio.use

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
        return getFileSystem().canonicalize(confPath)
    }

    private fun writeDosboxMapperFile(mapping: Map<Int, Int>, p1Config: ControlMapping, p2Config: ControlMapping) {
        getFileSystem().delete(path = mapperPath, mustExist = false)
        getFileSystem().write(file = mapperPath, mustCreate = true) {
            writeUtf8("key_esc \"key ${mapping[27]}\"\n")
            writeUtf8("key_1 \"key ${mapping[49]}\"\n")
            writeUtf8("key_2 \"key ${mapping[50]}\"\n")
            writeUtf8("key_3 \"key ${mapping[51]}\"\n")
            writeUtf8("key_4 \"key ${mapping[52]}\"\n")
            writeUtf8("key_5 \"key ${mapping[53]}\"\n")
            writeUtf8("key_6 \"key ${mapping[54]}\"\n")
            p1Config.apply {
                writeUtf8("key_up \"${up.dosboxMap(mapping)}\"\n")
                writeUtf8("key_down \"${down.dosboxMap(mapping)}\"\n")
                writeUtf8("key_left \"${left.dosboxMap(mapping)}\"\n")
                writeUtf8("key_right \"${right.dosboxMap(mapping)}\"\n")
                writeUtf8("key_enter \"${punch.dosboxMap(mapping)}\"\n")
                writeUtf8("key_rshift \"${kick.dosboxMap(mapping)}\"\n")
            }
            p2Config.apply {
                writeUtf8("key_w \"${up.dosboxMap(mapping)}\"\n")
                writeUtf8("key_x \"${down.dosboxMap(mapping)}\"\n")
                writeUtf8("key_a \"${left.dosboxMap(mapping)}\"\n")
                writeUtf8("key_d \"${right.dosboxMap(mapping)}\"\n")
                writeUtf8("key_tab \"${punch.dosboxMap(mapping)}\"\n")
                writeUtf8("key_lctrl \"${kick.dosboxMap(mapping)}\"\n")
            }
        }
    }

    private fun writeDosboxConfFile(novaConf: NovaConf) {
        val fs = getFileSystem()
        val absoluteMapperPath = fs.canonicalize(mapperPath)
        getFileSystem().delete(path = confPath, mustExist = false)
        getFileSystem().write(file = confPath, mustCreate = true) {
            writeUtf8(
                "[sdl]\nwaitonerror=false\nmapperfile=${absoluteMapperPath}\npriority=normal,normal\n"
            )
            writeUtf8(
                "[dosbox]\nmachine=svga_s3\nvmemsize=8\nmemsize=16\n"
            )
            writeUtf8(
                "[cpu]\ncycles=fixed 14500\n"
            )
            if(novaConf.joyEnabled) {
                val joyType = if (novaConf.isUsingHat()) "fcs" else "auto" //DOSBox only supports hat mappings in fcs mode.
                writeUtf8(
                    "[joystick]\njoysticktype=$joyType\ntimed=true\n" //Trying out timed=true, doesn't seem to matter.
                )
            } else {
                writeUtf8(
                    "[joystick]\njoysticktype=none\n"
                )
            }
            if(SoundCard.isGravisEnabled(novaConf.omfPath.toPath())) {
                writeUtf8("[gus]\ngus=true\n")
            }
        }
    }
}