import okio.FileSystem
import okio.Path.Companion.toPath
import okio.buffer
import okio.use

fun main() {
    val instance = getNativeWindow()
    instance?.runLoop(NovaProject())
}

expect fun getNativeWindow(): NovaWindow?
expect fun getFileSystem(): FileSystem

class NovaProject {
    enum class State {
        MENU, KEYCONFIG1, LAUNCH, OMF_CONFIG, NOVA_CONFIG
    }

    private var dosboxPath = ""
    private var omfPath = ""
    private val mapperPath = "omfMap.map".toPath()
    private val confPath = "omf.conf".toPath()
    private lateinit var window: NovaWindow
    private var currentState = State.MENU
    private var omfConfig: OMFConf? = null
    private val p1Config = getDefaultP1Config()
    private val p2Config = getDefaultP2Config()
    private lateinit var currentConfig: ControlMapping

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

    fun handleInput(input: ButtonMap) {
        when(currentState) {
            State.MENU -> menuHandleScancode(input.scancode)
            State.KEYCONFIG1 -> keyconfigHandleInput(input)
            State.OMF_CONFIG -> omfConfigHandleScancode(input.scancode)
            State.NOVA_CONFIG -> novaConfigHandleScancode(input.scancode)
            else -> { /* No-op */ }
        }
    }

    private fun menuHandleScancode(scancode: Int) {
        when(scancode) {
            VIRT_1 -> startKeyconfig(p1Config)
            VIRT_2 -> startKeyconfig(p2Config)
            VIRT_3 -> startNovaConfig()
            VIRT_4 -> startDosBox()
            VIRT_5 -> startOmfConfig()
            else -> { /* no-op */ }
        }
    }

    private fun novaConfigHandleScancode(scancode: Int) {
        when(scancode) {
            VIRT_1 -> {
                dosboxPath = window.showFileChooser(dosboxPath.ifEmpty { ".\\DOSBox.exe" }, "Select DOSBox.exe")
            }
            VIRT_2 -> {
                omfPath = window.showFolderChooser(omfPath.ifEmpty { "." }, "Select OMF2097 Location")
                loadOmfConfig()
            }
            VIRT_3 -> { /* Joystick Toggle */ }
            VIRT_4 -> {
                startMainMenu()
                return
            }
        }
        printNovaConfigOptions()
    }

    private fun omfConfigHandleScancode(scancode: Int) {
        omfConfig?.let { omfConfig ->
            when (scancode) {
                VIRT_1 -> omfConfig.hyperMode = !omfConfig.hyperMode
                VIRT_2 -> omfConfig.rehitMode = !omfConfig.rehitMode
                VIRT_3 -> omfConfig.hazards = !omfConfig.hazards
                VIRT_4 -> omfConfig.speed = (omfConfig.speed + 1) % 11
                VIRT_5 -> omfConfig.p1power = (omfConfig.p1power + 1)
                VIRT_6 -> omfConfig.p2power = (omfConfig.p2power + 1)
                VIRT_7 -> {
                    startMainMenu()
                    return
                }
            }
            omfConfig.print(window)
        }
    }

    private var allconfig = false
    private var waitingForKey = false
    private var configKey = 0
    private fun keyconfigHandleInput(input: ButtonMap) {
        if(waitingForKey) {
            waitingForKey = false
            when(configKey) {
                VIRT_1 -> currentConfig.up = input
                VIRT_2 -> currentConfig.down = input
                VIRT_3 -> currentConfig.left = input
                VIRT_4 -> currentConfig.right = input
                VIRT_5 -> currentConfig.punch = input
                VIRT_6 -> currentConfig.kick = input
            }
            if(allconfig) {
                configKey++
                if(configKey == VIRT_7) {
                    allconfig = false
                } else {
                    waitingForKey = true
                }
            }
        } else {
            when (input.scancode) {
                VIRT_1,VIRT_2,VIRT_3,VIRT_4,VIRT_5,VIRT_6 -> {
                    waitingForKey = true
                    configKey = input.scancode
                    val map = ButtonMap(type = ControlType.KEY, scancode = 0, name = "?")
                    when(input.scancode) {
                        VIRT_1 -> currentConfig.up = map
                        VIRT_2 -> currentConfig.down = map
                        VIRT_3 -> currentConfig.left = map
                        VIRT_4 -> currentConfig.right = map
                        VIRT_5 -> currentConfig.punch = map
                        VIRT_6 -> currentConfig.kick = map
                    }
                }
                VIRT_7 -> {
                    allconfig = true
                    waitingForKey = true
                    configKey = VIRT_1
                    val map = ButtonMap(type = ControlType.KEY, scancode = 0, name = "?")
                    currentConfig.up = map
                    currentConfig.down = map
                    currentConfig.left = map
                    currentConfig.right = map
                    currentConfig.punch = map
                    currentConfig.kick = map
                }
                VIRT_8 -> {
                    startMainMenu()
                    return
                }
            }
        }
        currentConfig.print(window)
    }

    private fun startDosBox() {
        omfConfig?.let {
            window.clearText()
            window.showText("=======  DOSBox Running  =======")
            currentState = State.LAUNCH
            writeOmfConfig()
            writeDosboxMapperFile()
            writeDosboxConfFile()
            val dosboxConfPath = getFileSystem().canonicalize(confPath)
            val separatorPosition = dosboxPath.lastIndexOf('\\')
            val exe = dosboxPath.substring(separatorPosition + 1)
            val args = listOf(
                exe,
                "-noconsole",
                "-conf \"$dosboxConfPath\"",
                "-c \"mount c $omfPath\"",
                "-c \"c:\"",
                "-c \"file0001 two_player\"",
                "-c \"exit\""
            )
            val command = args.joinToString(" ")
            window.executeCommand(executable = dosboxPath, command = command)
        }
    }

    fun dosBoxFinished() {
        startMainMenu()
    }

    private fun startKeyconfig(playerConfig: ControlMapping) {
        currentState = State.KEYCONFIG1
        currentConfig = playerConfig
        currentConfig.print(window)
    }

    private fun startOmfConfig() {
        omfConfig?.let {
            currentState = State.OMF_CONFIG
            it.print(window)
        }
    }

    private fun startMainMenu() {
        currentState = State.MENU
        printMenuOptions()
    }

    private fun startNovaConfig() {
        currentState = State.NOVA_CONFIG
        printNovaConfigOptions()
    }

    private fun writeOmfConfig() {
        val configPath = omfPath.toPath().div("SETUP.CFG")
        getFileSystem().openReadWrite(configPath).use {
            omfConfig?.buildFile(it)
        }
    }

    fun onStart(window: NovaWindow) {
        this.window = window
        loadNovaSettings()
        loadOmfConfig()
        printMenuOptions()
    }

    private fun loadOmfConfig() {
        val configPath = omfPath.toPath().div("SETUP.CFG")
        val fs = getFileSystem()
        if(fs.exists(configPath)) {
            fs.openReadOnly(configPath).use {
                omfConfig = OMFConf(it)
            }
        }
    }

    private fun getBoundInputs(): List<ButtonMap> {
        return listOf(
            p1Config.up,p1Config.down,p1Config.left,p1Config.right,p1Config.punch,p1Config.kick,
            p2Config.up,p2Config.down,p2Config.left,p2Config.right,p2Config.punch,p2Config.kick,
        )
    }

    private fun isUsingHat(): Boolean {
        getBoundInputs().find { it.type == ControlType.HAT }?.let {
            return true
        }
        return false
    }

    private fun getErrors(): List<String> {
        val errors = mutableListOf<String>()
        val boundInputs = getBoundInputs()
        if(isUsingHat()) {
            val devices = boundInputs.filter { it.type != ControlType.KEY }.map { it.controlId }.distinct()
            if(devices.size > 1) {
                errors.add("Hat not supported with two joysticks")
            }
        }
        if(boundInputs.distinct().size < boundInputs.size) {
            errors.add("Duplicate inputs detected")
        }
        val fs = getFileSystem()
        val pathToOmf = omfPath.toPath()
        if(omfPath.isBlank() || !fs.exists(pathToOmf)) {
            errors.add("OMF location not configured!")
        } else {
            val omffiles = fs.list(pathToOmf)
            val exec = omffiles.find { it.name.contains(other = "FILE0001.EXE", ignoreCase = true) }
            val cfg = omffiles.find { it.name.contains(other = "SETUP.CFG", ignoreCase = true) }
            if(exec == null || cfg == null) {
                errors.add("Missing files in OMF location!")
            }
        }
        val pathToDosbox = dosboxPath.toPath()
        if(dosboxPath.isBlank() || !fs.exists(pathToDosbox)) {
            errors.add("DOSBox location not configured!")
        } else if(!fs.metadata(pathToDosbox).isRegularFile) {
            errors.add("DOSBox location is not a file!")
        }
        return errors
    }

    fun onEnd() {
        saveNovaSettings()
    }

    private fun printNovaConfigOptions() {
        window.clearText()
        window.showText("=======   Nova Options   =======")
        window.showText("1. DOSBox location")
        window.showText("- $dosboxPath")
        window.showText("2. OMF location")
        window.showText("- $omfPath")
        window.showText("3. Joystick Support - On")
        window.showText("4. Back")
    }

    private fun printMenuOptions() {
        window.clearText()
        window.showText("======= Choose an Option =======")
        window.showText("1. Set Player 1 Keys")
        window.showText("2. Set Player 2 Keys")
        window.showText("3. Nova Project Settings")
        window.showText("4. Launch DosBox")
        window.showText("5. Change OMF Settings")
        window.showText("")
        getErrors().forEach {
            window.showText(it)
        }
    }

    private fun writeDosboxConfFile() {
        val fs = getFileSystem()
        val absoluteMapperPath = fs.canonicalize(mapperPath)
        fs.openReadWrite(confPath).use { handle ->
            handle.sink().buffer().use {
                it.writeUtf8(
                    "[sdl]\nwaitonerror=false\nmapperfile=${absoluteMapperPath}\n"
                )
                it.writeUtf8(
                    "[cpu]\ncycles=fixed 14500\n"
                )
                val joyType = if(isUsingHat()) "fcs" else "auto"
                it.writeUtf8(
                    "[joystick]\njoysticktype=$joyType\n"
                )
            }
        }
    }

    private fun writeDosboxMapperFile() {
        val fs = getFileSystem()
        fs.openReadWrite(mapperPath).use { handle ->
            handle.sink().buffer().use {
                it.writeUtf8("key_esc \"key 27\"\n")
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

    private fun saveNovaSettings() {
        val fs = getFileSystem()
        fs.openReadWrite("nova.cfg".toPath()).use { handle ->
            handle.sink().buffer().use {
                it.writeUtf8("$dosboxPath\n")
                it.writeUtf8("$omfPath\n")
                getBoundInputs().forEach { binding ->
                    it.writeUtf8("${binding.toNovaConf()}\n")
                }
            }
        }
    }

    private fun loadNovaSettings() {
        val location = "nova.cfg".toPath()
        val fs = getFileSystem()
        if(!fs.exists(location)) {
            saveNovaSettings()
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
            }
        }
    }
}

interface NovaWindow {
    fun runLoop(project: NovaProject)
    fun showText(textLine: String)
    fun clearText()
    fun executeCommand(executable: String, command: String)
    fun showFileChooser(start: String, prompt: String): String
    fun showFolderChooser(start: String, prompt: String): String
}

enum class ControlType {
    KEY, AXIS, BUTTON, HAT
}

data class ButtonMap(
    val type: ControlType,
    val controlId: Int = -1,
    val scancode: Int = 0,
    val name: String? = null,
    val axisId: Int = -1,
    val direction: Int = 0
)

fun ButtonMap.dosboxMap(): String {
    return when(type) {
        ControlType.KEY -> {
            "key $scancode"
        }
        ControlType.AXIS -> {
            "stick_$controlId axis $axisId $direction"
        }
        ControlType.HAT -> {
            "stick_$controlId hat $axisId $direction"
        }
        ControlType.BUTTON -> {
            "stick_$controlId button $axisId"
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

fun String.toButtonMap(): ButtonMap {
    val nodes = split(":")
    if (nodes.isEmpty()) {
        return ButtonMap(type = ControlType.KEY, scancode = 0, name = "?")
    }
    return when(nodes[0]) {
        "key" -> ButtonMap(type = ControlType.KEY, scancode = nodes[1].toInt(), name = nodes[2])
        "axis" -> ButtonMap(
            type = ControlType.AXIS,
            controlId = nodes[1].toInt(),
            name = nodes[4],
            axisId = nodes[2].toInt(),
            direction = nodes[3].toInt())
        "hat" -> ButtonMap(
            type = ControlType.HAT,
            controlId = nodes[1].toInt(),
            name = nodes[4],
            axisId = nodes[2].toInt(),
            direction = nodes[3].toInt())
        "button" -> ButtonMap(
            type = ControlType.BUTTON,
            controlId = nodes[1].toInt(),
            name = nodes[3],
            axisId = nodes[2].toInt())
        else -> ButtonMap(type = ControlType.KEY, scancode = 0, name = "?")
    }
}

data class ControlMapping(
    var up: ButtonMap,
    var down: ButtonMap,
    var left: ButtonMap,
    var right: ButtonMap,
    var punch: ButtonMap,
    var kick: ButtonMap
)

fun ControlMapping.print(window: NovaWindow) {
    window.clearText()
    window.showText("======= Controller Config =======")
    window.showText("1. UP    - ${up.name}")
    window.showText("2. DOWN  - ${down.name}")
    window.showText("3. LEFT  - ${left.name}")
    window.showText("4. RIGHT - ${right.name}")
    window.showText("5. PUNCH - ${punch.name}")
    window.showText("6. KICK  - ${kick.name}")
    window.showText("")
    window.showText("7. Set All")
    window.showText("8. Back")
}