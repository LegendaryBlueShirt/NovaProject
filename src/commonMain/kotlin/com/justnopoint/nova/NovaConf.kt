package com.justnopoint.nova

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.okio.decodeFromBufferedSource
import kotlinx.serialization.json.okio.encodeToBufferedSink
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath

class NovaConf(private val location: Path) {
    private var configuration = NovaConfFile(p1Config = getDefaultP1Config(), p2Config = getDefaultP2Config())

    var dosboxPath: String by configuration::dosBoxPath
    var omfPath: String by configuration::omfPath
    var confPath: String by configuration::confPath
    val p1Config: ControlMapping by configuration::p1Config
    val p2Config: ControlMapping by configuration::p2Config
    var joyEnabled: Boolean by configuration::joyEnabled
    var saveReplays: Boolean by configuration::saveReplays
    var userConf: Boolean by configuration::userConf
    var stagingCompat: Boolean by configuration::stagingCompat
    var attract: Boolean by configuration::attract

    var errors: List<String> = emptyList()

    fun checkErrors() {
        val currentErrors = mutableListOf<String>()
        val boundInputs = getBoundInputs()
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
        FileSystem.SYSTEM.apply {
            if(!exists(location)) {
                save()
            }
            try {
                read(location) {
                    configuration = Json.decodeFromBufferedSource(this)
                }
            } catch (e: Exception) {
                showErrorPopup("Failed to load Settings", e.message?:"Unknown Error")
                save()
            }
        }
    }

    fun save() {
        FileSystem.SYSTEM.write(file = location, mustCreate = false) {
            Json.encodeToBufferedSink(configuration, this)
        }
    }

    fun getBoundInputs(includeEsc: Boolean = false): List<ButtonMap> {
        return if(includeEsc) {
            listOf(
                p1Config.esc, p2Config.esc,
                p1Config.up, p1Config.down, p1Config.left, p1Config.right, p1Config.punch, p1Config.kick,
                p2Config.up, p2Config.down, p2Config.left, p2Config.right, p2Config.punch, p2Config.kick
            )
        } else {
            listOf(
                p1Config.up, p1Config.down, p1Config.left, p1Config.right, p1Config.punch, p1Config.kick,
                p2Config.up, p2Config.down, p2Config.left, p2Config.right, p2Config.punch, p2Config.kick
            )
        }
    }

    private fun getDefaultP1Config(): ControlMapping {
        return ControlMapping(
            up = ButtonMap(type = ControlType.KEY, scancode = VIRT_UP, name = "Up"),
            down = ButtonMap(type = ControlType.KEY, scancode = VIRT_DOWN, name = "Down"),
            left = ButtonMap(type = ControlType.KEY, scancode = VIRT_LEFT, name = "Left"),
            right = ButtonMap(type = ControlType.KEY, scancode = VIRT_RIGHT, name = "Right"),
            punch = ButtonMap(type = ControlType.KEY, scancode = VIRT_RETURN, name = "Return"),
            kick = ButtonMap(type = ControlType.KEY, scancode = VIRT_RSHIFT, name = "Right Shift"),
            esc = ButtonMap(type = ControlType.KEY, scancode = VIRT_ESC, name = "Escape")
        )
    }

    private fun getDefaultP2Config(): ControlMapping {
        return ControlMapping(
            up = ButtonMap(type = ControlType.KEY, scancode = VIRT_W, name = "W"),
            down = ButtonMap(type = ControlType.KEY, scancode = VIRT_S, name = "S"),
            left = ButtonMap(type = ControlType.KEY, scancode = VIRT_A, name = "A"),
            right = ButtonMap(type = ControlType.KEY, scancode = VIRT_D, name = "D"),
            punch = ButtonMap(type = ControlType.KEY, scancode = VIRT_LSHIFT, name = "Left Shift"),
            kick = ButtonMap(type = ControlType.KEY, scancode = VIRT_LCTRL, name = "Left Ctrl"),
            esc = ButtonMap(type = ControlType.KEY, scancode = VIRT_ESC, name = "Escape")
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

@Serializable
data class NovaConfFile(
    var dosBoxPath: String = "",
    var omfPath: String = "",
    var confPath: String = "",
    val p1Config: ControlMapping,
    val p2Config: ControlMapping,
    var joyEnabled: Boolean = true,
    var saveReplays: Boolean = false,
    var userConf: Boolean = false,
    var stagingCompat: Boolean = false,
    var attract: Boolean = false
)