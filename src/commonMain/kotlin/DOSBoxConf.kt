import okio.Path
import okio.Path.Companion.toPath
import okio.buffer
import okio.use

class DOSBoxConf {
    private val confPath = "omf.conf".toPath()
    private val mapperPath = "omfMap.map".toPath()

    fun writeConfFiles(novaConf: NovaConf) {
        writeDosboxMapperFile(novaConf.p1Config, novaConf.p2Config)
        writeDosboxConfFile(novaConf)
    }

    fun getConfPath(): Path {
        return getFileSystem().canonicalize(confPath)
    }

    private fun writeDosboxMapperFile(p1Config: ControlMapping, p2Config: ControlMapping) {
        val fs = getFileSystem()
        fs.openReadWrite(mapperPath).use { handle ->
            handle.sink().buffer().use {
                it.writeUtf8("key_esc \"key 27\"\n")
                it.writeUtf8("key_1 \"key 49\"\n")
                it.writeUtf8("key_2 \"key 50\"\n")
                it.writeUtf8("key_3 \"key 51\"\n")
                it.writeUtf8("key_4 \"key 52\"\n")
                it.writeUtf8("key_5 \"key 53\"\n")
                it.writeUtf8("key_6 \"key 54\"\n")
                p1Config.apply {
                    it.writeUtf8("key_up \"${up.dosboxMap()}\"\n")
                    it.writeUtf8("key_down \"${down.dosboxMap()}\"\n")
                    it.writeUtf8("key_left \"${left.dosboxMap()}\"\n")
                    it.writeUtf8("key_right \"${right.dosboxMap()}\"\n")
                    it.writeUtf8("key_enter \"${punch.dosboxMap()}\"\n")
                    it.writeUtf8("key_rshift \"${kick.dosboxMap()}\"\n")
                }
                p2Config.apply {
                    it.writeUtf8("key_w \"${up.dosboxMap()}\"\n")
                    it.writeUtf8("key_x \"${down.dosboxMap()}\"\n")
                    it.writeUtf8("key_a \"${left.dosboxMap()}\"\n")
                    it.writeUtf8("key_d \"${right.dosboxMap()}\"\n")
                    it.writeUtf8("key_tab \"${punch.dosboxMap()}\"\n")
                    it.writeUtf8("key_lctrl \"${kick.dosboxMap()}\"\n")
                }
            }
        }
    }

    private fun writeDosboxConfFile(novaConf: NovaConf) {
        val fs = getFileSystem()
        val absoluteMapperPath = fs.canonicalize(mapperPath)
        fs.openReadWrite(confPath).use { handle ->
            handle.sink().buffer().use {
                it.writeUtf8(
                    "[sdl]\nwaitonerror=false\nmapperfile=${absoluteMapperPath}\npriority=normal,normal\n"
                )
                it.writeUtf8(
                    "[cpu]\ncycles=fixed 14500\n"
                )
                if(novaConf.joyEnabled) {
                    val joyType = if (novaConf.isUsingHat()) "fcs" else "auto" //DOSBox only supports hat mappings in fcs mode.
                    it.writeUtf8(
                        "[joystick]\njoysticktype=$joyType\ntimed=true\n" //Trying out timed=true, doesn't seem to matter.
                    )
                } else {
                    it.writeUtf8(
                        "[joystick]\njoysticktype=none\n"
                    )
                }
            }
        }
    }
}