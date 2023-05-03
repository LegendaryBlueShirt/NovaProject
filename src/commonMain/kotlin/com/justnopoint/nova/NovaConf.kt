package com.justnopoint.nova

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.okio.decodeFromBufferedSource
import kotlinx.serialization.json.okio.encodeToBufferedSink
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath

class NovaConf(private val location: Path) {
    private val configuration: NovaConfFile
    private val json = Json {
        prettyPrint = true
        explicitNulls = false
    }

    init {
        var readConfiguration = NovaConfFile()
        FileSystem.SYSTEM.apply {
            if(exists(location)) {
                try {
                    read(location) {
                        val defaultConfiguration = readConfiguration
                        readConfiguration = json.decodeFromBufferedSource(this)
                        defaultConfiguration.trainingConfig.forEach { (key, value) ->
                            readConfiguration.trainingConfig.getOrPut(key) { value }
                        }
                        defaultConfiguration.p1Config.forEach { (key, value) ->
                            readConfiguration.p1Config.getOrPut(key) { value }
                        }
                        defaultConfiguration.p2Config.forEach { (key, value) ->
                            readConfiguration.p2Config.getOrPut(key) { value }
                        }
                    }
                } catch (e: Exception) {
                    showErrorPopup("Failed to load Settings", e.message ?: "Unknown Error")
                }
            }
        }
        configuration = readConfiguration
        save()
    }

    var dosboxPath: String by configuration::dosBoxPath
    var omfPath: String by configuration::omfPath
    var confPath: String by configuration::confPath
    val p1Config: ControlMapping get() = ControlMapping(configuration.p1Config)
    val p2Config: ControlMapping get() = ControlMapping(configuration.p2Config)
    val trainingConfig: TrainingMapping get() = TrainingMapping(configuration.trainingConfig)
    var joyEnabled: Boolean by configuration::joyEnabled
    var saveReplays: Boolean by configuration::saveReplays
    var userConf: Boolean by configuration::userConf
    var stagingCompat: Boolean by configuration::stagingCompat
    var attract: Boolean by configuration::attract

    var errors: List<String> = emptyList()

    fun checkErrors() {
        val currentErrors = mutableListOf<String>()
        val boundInputs = getBoundInputs()
        if (!joyEnabled) {
            val joyInputs = boundInputs.filter { it.type != ControlType.KEY }
            if (joyInputs.size > 1) {
                currentErrors.add("Joysticks disabled, please remap buttons")
            }
        }
        if (boundInputs.distinct().size < boundInputs.size) {
            currentErrors.add("Duplicate inputs detected")
        }
        val missingFiles = validateOmfSetup()
        when(missingFiles.size) {
            0 -> { /* No-op */ }
            4 -> {
                currentErrors.add("OMF location not configured!")
            }
            else -> {
                if(missingFiles.contains(SoundCard.FILENAME)) {
                    currentErrors.add("Please run OMF setup!")
                } else if(missingFiles.contains(OMFConf.FILENAME)) {
                    currentErrors.add("OMF settings not found, run the game once.")
                } else {
                    currentErrors.add("Missing files in OMF location!")
                }
            }
        }

        validateDosbox()?.let(currentErrors::add)

        if (confPath.isNotBlank() && !FileSystem.SYSTEM.exists(confPath.toPath())) {
            currentErrors.add("Custom configuration file is missing and will be ignored!")
        }

        errors = currentErrors
    }

    fun validateDosbox(): String? {
        val fs = FileSystem.SYSTEM
        val pathToDosbox = dosboxPath.toPath()
        if (dosboxPath.isBlank() || !fs.exists(pathToDosbox)) {
            return "DOSBox location not configured!"
        } else if (!fs.metadata(pathToDosbox).isRegularFile) {
            return "DOSBox location is not a file!"
        }
        return null
    }

    fun validateOmfSetup(): List<String> {
        var missing = listOf(
            "FILE0001.EXE",
            OMFConf.SETUP,
            OMFConf.FILENAME,
            SoundCard.FILENAME
        )
        val fs = FileSystem.SYSTEM
        val pathToOmf = omfPath.toPath()
        if (omfPath.isNotBlank() && fs.exists(pathToOmf)) {
            val omffiles = fs.list(pathToOmf)
            missing = missing.filter { fileToCheck ->
                val foundPath = omffiles.find { it.name.contains(other = fileToCheck, ignoreCase = true) }
                foundPath == null
            }
        }
        return missing
    }

    fun save() {
        FileSystem.SYSTEM.delete(path = location, mustExist = false)
        FileSystem.SYSTEM.write(file = location, mustCreate = false) {
            json.encodeToBufferedSink(configuration, this)
        }
        writeLog("Nova configuration saved")
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
    val p1Config: MutableMap<String, ButtonMap> = getDefaultP1Config(),
    val p2Config: MutableMap<String, ButtonMap> = getDefaultP2Config(),
    val trainingConfig: MutableMap<String, ButtonMap> = getDefaultTrainingConfig(),
    var joyEnabled: Boolean = true,
    var saveReplays: Boolean = false,
    var userConf: Boolean = false,
    var stagingCompat: Boolean = false,
    var attract: Boolean = false
)

private fun getDefaultP1Config(): MutableMap<String, ButtonMap> {
    return mutableMapOf(
        "up" to ButtonMap(type = ControlType.KEY, scancode = VIRT_UP, name = "Up"),
        "down" to ButtonMap(type = ControlType.KEY, scancode = VIRT_DOWN, name = "Down"),
        "left" to ButtonMap(type = ControlType.KEY, scancode = VIRT_LEFT, name = "Left"),
        "right" to ButtonMap(type = ControlType.KEY, scancode = VIRT_RIGHT, name = "Right"),
        "punch" to ButtonMap(type = ControlType.KEY, scancode = VIRT_RETURN, name = "Return"),
        "kick" to ButtonMap(type = ControlType.KEY, scancode = VIRT_RSHIFT, name = "Right Shift"),
        "esc" to ButtonMap(type = ControlType.KEY, scancode = VIRT_ESC, name = "Escape")
    )
}

private fun getDefaultP2Config(): MutableMap<String, ButtonMap> {
    return mutableMapOf(
        "up" to ButtonMap(type = ControlType.KEY, scancode = VIRT_W, name = "W"),
        "down" to ButtonMap(type = ControlType.KEY, scancode = VIRT_S, name = "S"),
        "left" to ButtonMap(type = ControlType.KEY, scancode = VIRT_A, name = "A"),
        "right" to ButtonMap(type = ControlType.KEY, scancode = VIRT_D, name = "D"),
        "punch" to ButtonMap(type = ControlType.KEY, scancode = VIRT_LSHIFT, name = "Left Shift"),
        "kick" to ButtonMap(type = ControlType.KEY, scancode = VIRT_LCTRL, name = "Left Ctrl"),
        "esc" to ButtonMap(type = ControlType.KEY, scancode = VIRT_ESC, name = "Escape")
    )
}

private fun getDefaultTrainingConfig(): MutableMap<String, ButtonMap> {
    return mutableMapOf(
        "resetLeft" to ButtonMap(type = ControlType.KEY, scancode = VIRT_F1, name = "F1"),
        "resetCenter" to ButtonMap(type = ControlType.KEY, scancode = VIRT_F2, name = "F2"),
        "resetRight" to ButtonMap(type = ControlType.KEY, scancode = VIRT_F3, name = "F3"),
        "record" to ButtonMap(type = ControlType.KEY, scancode = VIRT_F4, name = "F4"),
        "playback" to ButtonMap(type = ControlType.KEY, scancode = VIRT_F5, name = "F5")
    )
}