package com.justnopoint.nova

import com.justnopoint.nova.menu.MainMenu
import kotlinx.serialization.Serializable
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import kotlin.system.getTimeMillis

fun main() {
    getNativeWindow()?.let {
        writeLog("Starting Nova")
        NovaProject().runLoop(it)
    }
}

expect fun getNativeWindow(): NovaWindow?

expect fun showErrorPopup(title: String, message: String)

expect fun writeLog(message: String)

val debug = true
var trainingMode = false
class NovaProject {
    private var quit = false
    private lateinit var window: NovaWindow
    var omfConfig: OMFConf? = null
    val novaConf = NovaConf("novacfg.json".toPath())
    private val dosboxConf = DOSBoxConf()
    private val mainMenu = MainMenu(this)

    //UI Vars
    private var frameCount = 0
    private var bgTex = 0
    private var darkener = 0

    private var startFrameTime = 0L
    private val millisPerFrame = 1000.0 / 60

    private var gameRunning = false
    private var matchStarted = false
    private var vsScreen = false

    fun runLoop(window: NovaWindow) {
        onStart(window)
        startFrameTime = getTimeMillis()
        var currentFrameTime: Long
        var nextFrameTime: Long
        while (!quit) {
            try {
                window.processEvents(this)
            } catch (e: Exception) {
                showErrorPopup("Runtime Error", e.message?:"Unknown Error")
            }
            doDummyPlayback()?.let { (button, up, _) ->
                window.sendKeyEvent(button, up, false, true)
            }
            currentFrameTime = getTimeMillis()
            nextFrameTime = (startFrameTime + (frameCount*millisPerFrame).toLong())
            if(currentFrameTime > nextFrameTime) {
                //if(!gameRunning) {
                    renderFrame()
                //}
                frameCount++
                if(frameCount < 0) {
                    frameCount = 0
                }
                if(gameRunning) {
                    if (!matchStarted) {
                        if (isMatchStarted()) {
                            matchStarted = true
                            matchStarted()
                        }
                    } else {
                        if (!isMatchStarted()) {
                            matchStarted = false
                        } else {
                            duringMatch()
                        }
                    }
                    if (!vsScreen) {
                        if (isVsScreen()) {
                            vsScreenStarted()
                            vsScreen = true
                        }
                    } else {
                        if (!isVsScreen()) {
                            vsScreen = false
                        }
                    }
                }
            }
        }
        onEnd()
    }

    fun quit() {
        quit = true
    }

    fun handleInput(input: ButtonMap, release: Boolean = false) {
        if(!gameRunning) {
            if(!release) {
                mainMenu.handleInput(input)
            }
        } else {
            window.sendKeyEvent(input, release, dummyActive)
        }
    }

    fun startVs() {
        omfConfig?.let {
            writeOmfConfig(false)
        }
        startDosBox(saveReplays = novaConf.saveReplays, userconf = novaConf.userConf)
    }

    fun startNormal() {
        omfConfig?.let {
            writeOmfConfig(true)
        }
        startDosBox(mode = "advanced", saveReplays = novaConf.saveReplays, userconf = novaConf.userConf)
    }

    fun startTraining() {
        omfConfig?.let {
            writeOmfConfig(false)
        }
        trainingMode = true
        setTrainingInputs(novaConf.trainingConfig)
        startDosBox(saveReplays = false, userconf = false)
    }

    fun startSetup() {
        dosboxConf.writeConfFiles(novaConf)
        val dosboxConfPath = dosboxConf.getConfPath()
        val separatorPosition = novaConf.dosboxPath.lastIndexOf('\\')
        val exe = novaConf.dosboxPath.substring(separatorPosition + 1)
        val hasCustomConf = novaConf.confPath.isNotBlank() && FileSystem.SYSTEM.exists(novaConf.confPath.toPath())
        val args = listOfNotNull(
            exe,
            if(novaConf.userConf) "-userconf" else null,
            if(hasCustomConf) "-conf \"${novaConf.confPath}\"" else null,
            "-noconsole",
            "-noautoexec",
            "-conf \"$dosboxConfPath\"",
            "-c \"mount c ${novaConf.omfPath}\"",
            "-c \"c:\"",
            "-c \"setup\"",
            "-c \"exit\""
        )
        val command = args.joinToString(" ")
        window.executeCommand(executable = novaConf.dosboxPath, command = command)
        gameRunning = true
    }

    fun startReplay(path: Path) {
        omfConfig?.let {
            startDosBox(replayFile = path, userconf = novaConf.userConf)
        }
    }

    private fun startDosBox(replayFile: Path? = null, userconf: Boolean = false, saveReplays: Boolean = false, mode: String = "two_player") {
        dosboxConf.writeConfFiles(novaConf)
        val dosboxConfPath = dosboxConf.getConfPath()
        val separatorPosition = novaConf.dosboxPath.lastIndexOf('\\')
        val exe = novaConf.dosboxPath.substring(separatorPosition + 1)
        val omfParams = if(replayFile != null) {
            "play $replayFile"
        } else if(saveReplays) {
            "$mode rec save_rec"
        } else {
            mode
        }
        val hasCustomConf = novaConf.confPath.isNotBlank() && FileSystem.SYSTEM.exists(novaConf.confPath.toPath())
        val args = listOfNotNull(
            exe,
            if(userconf) "-userconf" else null,
            if(hasCustomConf) "-conf \"${novaConf.confPath}\"" else null,
            "-noconsole",
            "-noautoexec",
            "-conf \"$dosboxConfPath\"",
            "-c \"mount c ${novaConf.omfPath}\"",
            "-c \"c:\"",
            "-c \"file0001 $omfParams\"",
            "-c \"exit\""
        )
        val command = args.joinToString(" ")
        window.executeCommand(executable = novaConf.dosboxPath, command = command)
        gameRunning = true
        //This doesn't work for DOSBox staging! AAAAAAAAAAA
//        getFileSystem().openReadOnly("stdout.txt".toPath()).use {handle ->
//            handle.source().buffer().use {buffer ->
//                println(buffer.readUtf8Line())
//            }
//        }
    }

    fun vsScreenStarted() {
        p1struct.ptrAddr = readMemoryInt(p1PilotPointer).toLong()
        p2struct.ptrAddr = readMemoryInt(p2PilotPointer).toLong()

//        for(n in 0 until 11) {
//            writeMemoryByte(p1struct.enchancementLevel + n, 3u)
//            writeMemoryByte(p2struct.enchancementLevel + n, 3u)
//        }
//
//        writeMemoryString(p1struct.name, "Yo", 18)
//        writeMemoryString(p2struct.name, "Mama", 18)
    }

    fun matchStarted() {

    }

    fun duringMatch() {
//        val savedR = readMemoryByte(video.palette+3)
//        val savedG = readMemoryByte(video.palette+4)
//        val savedB = readMemoryByte(video.palette+5)
//        for(c in 2 until 48) {
//            val red = readMemoryByte(video.palette+c*3)
//            val green = readMemoryByte(video.palette+c*3+1)
//            val blue = readMemoryByte(video.palette+c*3+2)
//            writeMemoryByte(video.palette+(c-1)*3, red)
//            writeMemoryByte(video.palette+(c-1)*3+1, green)
//            writeMemoryByte(video.palette+(c-1)*3+2, blue)
//        }
//        writeMemoryByte(video.palette+47*3, savedR)
//        writeMemoryByte(video.palette+47*3+1, savedG)
//        writeMemoryByte(video.palette+47*3+2, savedB)
    }

    fun dosBoxFinished() {
        writeLog("DOSBox has terminated")
        trainingMode = false
        novaConf.checkErrors()
        gameRunning = false
        mainMenu.gameEnd()
        loadOmfConfig()
    }

    private fun writeOmfConfig(singlePlayer: Boolean) {
        val configPath = novaConf.omfPath.toPath().div("SETUP.CFG")
        try {
            FileSystem.SYSTEM.apply {
                if(exists(configPath)) {
                    write(file = configPath, mustCreate = false) {
                        omfConfig?.buildFile(this, singlePlayer)
                    }
                    writeLog("OMF configuration written")
                }
            }
        } catch (e: Exception) {
            showErrorPopup("Could not write OMF configuration", e.message?:"Unknown Error")
        }
    }

    @OptIn(ExperimentalUnsignedTypes::class)
    private fun onStart(window: NovaWindow) {
        this.window = window

        val background = loadPcx("NETARENA.PCX".toPath())?: error("Couldn't load background NETARENA.PCX")
        bgTex = window.loadTexture(background)
        val font1image = loadPcx("NETFONT1.PCX".toPath())?: error("Couldn't load font NETFONT1.PCX")
        val font2image = loadPcx("NETFONT2.PCX".toPath())?: error("Couldn't load font NETFONT2.PCX")

        val darkenerRaster = UByteArray(320*144*4)
        for(n in 0 until (320*144)) {
            darkenerRaster[n*4] = 0x0u
            darkenerRaster[n*4+1] = 0x0u
            darkenerRaster[n*4+2] = 0x0u
            darkenerRaster[n*4+3] = 0x80u
        }
        darkener = window.loadTextureFromRaster(darkenerRaster, 320, 144)

        /* Manipulation to get all font colors we need */
        background.paletteData.copyInto(font1image.paletteData)
        background.paletteData.copyInto(font2image.paletteData)
        MenuFonts.initialize(window, font1image, font2image)

        window.setJoystickEnabled(novaConf.joyEnabled)
        loadOmfConfig()
        novaConf.checkErrors()
    }

    private fun renderFrame() {
        window.startRender()

        window.showImage(bgTex,0,0)
        window.showImage(darkener, 0, 0)
        mainMenu.render(window, frameCount)

        novaConf.errors.forEachIndexed { index, error ->
            window.showText(error, MenuFonts.bigFont_red, 20, 376 - index*18)
        }

        window.endRender()
    }

    private fun loadOmfConfig() {
        val configPath = novaConf.omfPath.toPath().div(OMFConf.FILENAME)
        val fs = FileSystem.SYSTEM
        if (fs.exists(configPath)) {
            writeLog("OMF configuration loaded")
            omfConfig = fs.read(file = configPath, readerAction = ::OMFConf)
        } else {
            writeLog("OMF configuration not found, skipping")
        }
    }

    private fun onEnd() {
        window.destroy()
        novaConf.save()
        writeOmfConfig(true)
    }

    fun showFileChooser(start: String, prompt: String, filter: String, filterDesc: String): String {
        return window.showFileChooser(start, prompt, filter, filterDesc)
    }

    fun showFolderChooser(start: String, prompt: String): String {
        return window.showFolderChooser(start, prompt)
    }

    fun setJoystickEnabled(joyEnabled: Boolean) {
        window.setJoystickEnabled(joyEnabled)
    }
}

interface NovaWindow {
    fun processEvents(project: NovaProject)
    fun startRender()
    fun endRender()
    //fun showText(textLine: String)
    fun showText(textLine: String, font: Int, x: Int, y: Int, reverse: Boolean = false)
    fun executeCommand(executable: String, command: String)
    fun showFileChooser(start: String, prompt: String, filter: String, filterDesc: String): String
    fun showFolderChooser(start: String, prompt: String): String
    fun setJoystickEnabled(joyEnabled: Boolean)
    fun destroy()
    fun loadFont(fontMapping: OmfFont, textureHandle: Int): Int
    fun loadTexture(image: PCXImage): Int
    fun loadTexturePng(path: String): Int
    @OptIn(ExperimentalUnsignedTypes::class)
    fun loadTextureFromRaster(raster: UByteArray, width: Int, height: Int): Int
    fun showImage(textureHandle: Int, x: Int, y: Int)
    fun sendKeyEvent(mappedButton: ButtonMap, up: Boolean, useDummy: Boolean, recorded: Boolean = false)
}

enum class ControlType {
    KEY, AXIS, BUTTON, HAT
}

@Serializable
data class ButtonMap(
    val type: ControlType,
    val controlId: Int = -1,
    val scancode: Int = 0,
    val name: String? = null,
    val axisId: Int = -1,
    val direction: Int = 0
)

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

class ControlMapping(map: MutableMap<String, ButtonMap>) {
    var up by map
    var down by map
    var left by map
    var right by map
    var punch by map
    var kick by map
    var esc by map
}

class TrainingMapping(map: MutableMap<String, ButtonMap>) {
    var resetLeft by map
    var resetRight by map
    var resetCenter by map
    var record by map
    var playback by map
}

enum class OMFInput {
    UP, DOWN, LEFT, RIGHT, PUNCH, KICK
}