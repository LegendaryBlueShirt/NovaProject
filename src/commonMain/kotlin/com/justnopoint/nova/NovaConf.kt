package com.justnopoint.nova

import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath

class NovaConf(val location: Path) {
    var dosboxPath = ""
    var omfPath = ""
    var confPath = ""
    val p1Config = getDefaultP1Config()
    val p2Config = getDefaultP2Config()
    var joyEnabled = true
    var saveReplays = false
    var userConf = false
    var stagingCompat = false
    var attract = false

    var errors: List<String> = emptyList()

    fun checkErrors() {
        val currentErrors = mutableListOf<String>()
        val boundInputs = getBoundInputs()
//        if(isUsingHat()) {
//            val devices = boundInputs.filter { it.type != ControlType.KEY }.map { it.controlId }.distinct()
//            if(devices.size > 1) {
//                currentErrors.add("Hat not supported with two joysticks")
//            }
//        }
        if(!joyEnabled) {
            val joyInputs = boundInputs.filter { it.type != ControlType.KEY }
            if(joyInputs.size > 1) {
                currentErrors.add("Joysticks disabled, please remap buttons")
            }
        }
        if(boundInputs.distinct().size < boundInputs.size) {
            currentErrors.add("Duplicate inputs detected")
        }
        val fs = FileSystem.SYSTEM
        val pathToOmf = omfPath.toPath()
        if(omfPath.isBlank() || !fs.exists(pathToOmf)) {
            currentErrors.add("OMF location not configured!")
        } else {
            val omffiles = fs.list(pathToOmf)
            val exec = omffiles.find { it.name.contains(other = "FILE0001.EXE", ignoreCase = true) }
            val cfg = omffiles.find { it.name.contains(other = OMFConf.FILENAME, ignoreCase = true) }
            if(exec == null || cfg == null) {
                currentErrors.add("Missing files in OMF location!")
            }
        }
        val pathToDosbox = dosboxPath.toPath()
        if(dosboxPath.isBlank() || !fs.exists(pathToDosbox)) {
            currentErrors.add("DOSBox location not configured!")
        } else if(!fs.metadata(pathToDosbox).isRegularFile) {
            currentErrors.add("DOSBox location is not a file!")
        }

        if(confPath.isNotBlank() && !fs.exists(confPath.toPath())) {
            currentErrors.add("Custom configuration file is missing and will be ignored!")
        }

        errors = currentErrors
    }

    init {
        val fs = FileSystem.SYSTEM
        if(!fs.exists(location)) {
            save()
        }
        fs.read(location) {
            dosboxPath = readUtf8Line()?:""
            omfPath = readUtf8Line()?:""
            p1Config.up = (readUtf8Line()?:"").toButtonMap()
            p1Config.down = (readUtf8Line()?:"").toButtonMap()
            p1Config.left = (readUtf8Line()?:"").toButtonMap()
            p1Config.right = (readUtf8Line()?:"").toButtonMap()
            p1Config.punch = (readUtf8Line()?:"").toButtonMap()
            p1Config.kick = (readUtf8Line()?:"").toButtonMap()
            p2Config.up = (readUtf8Line()?:"").toButtonMap()
            p2Config.down = (readUtf8Line()?:"").toButtonMap()
            p2Config.left = (readUtf8Line()?:"").toButtonMap()
            p2Config.right = (readUtf8Line()?:"").toButtonMap()
            p2Config.punch = (readUtf8Line()?:"").toButtonMap()
            p2Config.kick = (readUtf8Line()?:"").toButtonMap()
            joyEnabled = (readUtf8Line()?:"true").toBoolean()
            saveReplays = (readUtf8Line()?:"false").toBoolean()
            userConf = (readUtf8Line()?:"false").toBoolean()
            attract = (readUtf8Line()?:"false").toBoolean()
            confPath = readUtf8Line()?:""
        }
    }

    fun save() {
        FileSystem.SYSTEM.write(file = location, mustCreate = false) {
            writeUtf8("$dosboxPath\n")
            writeUtf8("$omfPath\n")
            getBoundInputs().forEach { binding ->
                writeUtf8("${binding.toNovaConf()}\n")
            }
            writeUtf8("$joyEnabled\n")
            writeUtf8("$saveReplays\n")
            writeUtf8("$userConf\n")
            writeUtf8("$attract\n")
            writeUtf8("$confPath\n")
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