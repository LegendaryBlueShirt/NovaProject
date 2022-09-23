import okio.Path
import okio.buffer
import okio.use

class NovaConf(val location: Path) {
    var dosboxPath = ""
    var omfPath = ""
    val p1Config = getDefaultP1Config()
    val p2Config = getDefaultP2Config()
    var joyEnabled = true
    var saveReplays = false

    init {
        val fs = getFileSystem()
        if(!fs.exists(location)) {
            save()
        }
        fs.openReadOnly(location).use { handle ->
            handle.source().buffer().use {
                dosboxPath = it.readUtf8Line()?:""
                omfPath = it.readUtf8Line()?:""
                p1Config.up = (it.readUtf8Line()?:"").toButtonMap()
                p1Config.down = (it.readUtf8Line()?:"").toButtonMap()
                p1Config.left = (it.readUtf8Line()?:"").toButtonMap()
                p1Config.right = (it.readUtf8Line()?:"").toButtonMap()
                p1Config.punch = (it.readUtf8Line()?:"").toButtonMap()
                p1Config.kick = (it.readUtf8Line()?:"").toButtonMap()
                p2Config.up = (it.readUtf8Line()?:"").toButtonMap()
                p2Config.down = (it.readUtf8Line()?:"").toButtonMap()
                p2Config.left = (it.readUtf8Line()?:"").toButtonMap()
                p2Config.right = (it.readUtf8Line()?:"").toButtonMap()
                p2Config.punch = (it.readUtf8Line()?:"").toButtonMap()
                p2Config.kick = (it.readUtf8Line()?:"").toButtonMap()
                joyEnabled = (it.readUtf8Line()?:"true").toBoolean()
                saveReplays = (it.readUtf8Line()?:"false").toBoolean()
            }
        }
    }

    fun save() {
        val fs = getFileSystem()
        fs.openReadWrite(location).use { handle ->
            handle.sink().buffer().use {
                it.writeUtf8("$dosboxPath\n")
                it.writeUtf8("$omfPath\n")
                getBoundInputs().forEach { binding ->
                    it.writeUtf8("${binding.toNovaConf()}\n")
                }
                it.writeUtf8("$joyEnabled\n")
                it.writeUtf8(saveReplays.toString())
            }
        }
    }

    fun getBoundInputs(): List<ButtonMap> {
        return listOf(
            p1Config.up,p1Config.down,p1Config.left,p1Config.right,p1Config.punch,p1Config.kick,
            p2Config.up,p2Config.down,p2Config.left,p2Config.right,p2Config.punch,p2Config.kick,
        )
    }

    fun isUsingHat(): Boolean {
        getBoundInputs().find { it.type == ControlType.HAT }?.let {
            return true
        }
        return false
    }

    fun printNovaConfigOptions(window: NovaWindow) {
        window.clearText()
        window.showText("=======   Nova Options   =======")
        window.showText("1. DOSBox location")
        window.showText("- $dosboxPath")
        window.showText("2. OMF location")
        window.showText("- $omfPath")
        window.showText("3. Joystick Support - ${if(joyEnabled) "On" else "Off"}")
        window.showText("4. Back")
    }

    private fun getDefaultP1Config(): ControlMapping {
        return ControlMapping(
            up = ButtonMap(type = ControlType.KEY, scancode = VIRT_UP, name = "Up"),
            down = ButtonMap(type = ControlType.KEY, scancode = VIRT_DOWN, name = "Down"),
            left = ButtonMap(type = ControlType.KEY, scancode = VIRT_LEFT, name = "Left"),
            right = ButtonMap(type = ControlType.KEY, scancode = VIRT_RIGHT, name = "Right"),
            punch = ButtonMap(type = ControlType.KEY, scancode = VIRT_RETURN, name = "Return"),
            kick = ButtonMap(type = ControlType.KEY, scancode = VIRT_RSHIFT, name = "Right Shift")
        )
    }

    private fun getDefaultP2Config(): ControlMapping {
        return ControlMapping(
            up = ButtonMap(type = ControlType.KEY, scancode = VIRT_W, name = "W"),
            down = ButtonMap(type = ControlType.KEY, scancode = VIRT_S, name = "S"),
            left = ButtonMap(type = ControlType.KEY, scancode = VIRT_A, name = "A"),
            right = ButtonMap(type = ControlType.KEY, scancode = VIRT_D, name = "D"),
            punch = ButtonMap(type = ControlType.KEY, scancode = VIRT_LSHIFT, name = "Left Shift"),
            kick = ButtonMap(type = ControlType.KEY, scancode = VIRT_LCTRL, name = "Left Ctrl")
        )
    }
}

fun ButtonMap.toNovaConf(): String {
    return when(type) {
        ControlType.KEY -> {
            "key:$scancode:$name"
        }
        ControlType.AXIS -> {
            "axis:$controlId:$axisId:$direction:$name"
        }
        ControlType.HAT -> {
            "hat:$controlId:$axisId:$direction:$name"
        }
        ControlType.BUTTON -> {
            "button:$controlId:$axisId:$name"
        }
    }
}