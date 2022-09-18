import okio.FileSystem
import okio.Path.Companion.toPath
import okio.use

fun main() {
    getNativeWindow()?.let {
        NovaProject().runLoop(it)
    }
}

expect fun getNativeWindow(): NovaWindow?
expect fun getFileSystem(): FileSystem

class NovaProject {
    enum class State {
        MENU, KEYCONFIG1, LAUNCH, OMF_CONFIG, NOVA_CONFIG
    }

    private var quit = false
    private lateinit var window: NovaWindow
    private var currentState = State.MENU
    private var omfConfig: OMFConf? = null
    private val novaConf = NovaConf("nova.cfg".toPath())
    private val dosboxConf = DOSBoxConf()
    private lateinit var currentConfig: ControlMapping

    fun runLoop(window: NovaWindow) {
        onStart(window)
        while (!quit) {
            window.processEvents(this)
            window.render()
        }
        onEnd()
    }

    fun quit() {
        quit = true
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
            VIRT_1 -> startKeyconfig(novaConf.p1Config)
            VIRT_2 -> startKeyconfig(novaConf.p2Config)
            VIRT_3 -> startNovaConfig()
            VIRT_4 -> startDosBox()
            VIRT_5 -> startOmfConfig()
            else -> { /* no-op */ }
        }
    }

    private fun novaConfigHandleScancode(scancode: Int) {
        when(scancode) {
            VIRT_1 -> {
                novaConf.dosboxPath = window.showFileChooser(novaConf.dosboxPath.ifEmpty { ".\\DOSBox.exe" }, "Select DOSBox.exe")
            }
            VIRT_2 -> {
                novaConf.omfPath = window.showFolderChooser(novaConf.omfPath.ifEmpty { "." }, "Select OMF2097 Location")
                loadOmfConfig()
            }
            VIRT_3 -> {
                novaConf.joyEnabled = !novaConf.joyEnabled
                window.setJoystickEnabled(novaConf.joyEnabled)
            }
            VIRT_4 -> {
                startMainMenu()
                return
            }
        }
        novaConf.printNovaConfigOptions(window)
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
            omfConfig.printGameOptions(window)
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
            dosboxConf.writeConfFiles(novaConf)
            val dosboxConfPath = dosboxConf.getConfPath()
            val separatorPosition = novaConf.dosboxPath.lastIndexOf('\\')
            val exe = novaConf.dosboxPath.substring(separatorPosition + 1)
            val args = listOf(
                exe,
                "-noconsole",
                "-conf \"$dosboxConfPath\"",
                "-c \"mount c ${novaConf.omfPath}\"",
                "-c \"c:\"",
                "-c \"file0001 two_player\"",
                "-c \"exit\""
            )
            val command = args.joinToString(" ")
            window.executeCommand(executable = novaConf.dosboxPath, command = command)
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
            it.printGameOptions(window)
        }
    }

    private fun startMainMenu() {
        currentState = State.MENU
        printMenuOptions()
    }

    private fun startNovaConfig() {
        currentState = State.NOVA_CONFIG
        novaConf.printNovaConfigOptions(window)
    }

    private fun writeOmfConfig() {
        val configPath = novaConf.omfPath.toPath().div("SETUP.CFG")
        getFileSystem().openReadWrite(configPath).use {
            omfConfig?.buildFile(it)
        }
    }

    private fun onStart(window: NovaWindow) {
        this.window = window
        window.setJoystickEnabled(novaConf.joyEnabled)
        loadOmfConfig()
        printMenuOptions()
    }

    private fun loadOmfConfig() {
        val configPath = novaConf.omfPath.toPath().div("SETUP.CFG")
        val fs = getFileSystem()
        if(fs.exists(configPath)) {
            fs.openReadOnly(configPath).use {
                omfConfig = OMFConf(it)
            }
        }
    }
    private fun getErrors(): List<String> {
        val errors = mutableListOf<String>()
        val boundInputs = novaConf.getBoundInputs()
        if(novaConf.isUsingHat()) {
            val devices = boundInputs.filter { it.type != ControlType.KEY }.map { it.controlId }.distinct()
            if(devices.size > 1) {
                errors.add("Hat not supported with two joysticks")
            }
        }
        if(!novaConf.joyEnabled) {
            val joyInputs = boundInputs.filter { it.type != ControlType.KEY }
            if(joyInputs.size > 1) {
                errors.add("Joysticks disabled, please remap buttons")
            }
        }
        if(boundInputs.distinct().size < boundInputs.size) {
            errors.add("Duplicate inputs detected")
        }
        val fs = getFileSystem()
        val pathToOmf = novaConf.omfPath.toPath()
        if(novaConf.omfPath.isBlank() || !fs.exists(pathToOmf)) {
            errors.add("OMF location not configured!")
        } else {
            val omffiles = fs.list(pathToOmf)
            val exec = omffiles.find { it.name.contains(other = "FILE0001.EXE", ignoreCase = true) }
            val cfg = omffiles.find { it.name.contains(other = "SETUP.CFG", ignoreCase = true) }
            if(exec == null || cfg == null) {
                errors.add("Missing files in OMF location!")
            }
        }
        val pathToDosbox = novaConf.dosboxPath.toPath()
        if(novaConf.dosboxPath.isBlank() || !fs.exists(pathToDosbox)) {
            errors.add("DOSBox location not configured!")
        } else if(!fs.metadata(pathToDosbox).isRegularFile) {
            errors.add("DOSBox location is not a file!")
        }
        return errors
    }

    private fun onEnd() {
        novaConf.save()
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
}

interface NovaWindow {
    fun processEvents(project: NovaProject)
    fun render()
    fun showText(textLine: String)
    fun clearText()
    fun executeCommand(executable: String, command: String)
    fun showFileChooser(start: String, prompt: String): String
    fun showFolderChooser(start: String, prompt: String): String
    fun setJoystickEnabled(joyEnabled: Boolean)
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