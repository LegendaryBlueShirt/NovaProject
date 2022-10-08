package com.justnopoint.nova

import com.justnopoint.nova.menu.MainMenu
import okio.FileSystem
import okio.Path
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
    private var quit = false
    private lateinit var window: NovaWindow
    var omfConfig: OMFConf? = null
    val novaConf = NovaConf("nova.cfg".toPath())
    private val dosboxConf = DOSBoxConf()
    private val mainMenu = MainMenu(this)

    //UI Vars
    private var frameCount = 0
    private var bgTex = 0
    private var darkener = 0

    fun runLoop(window: NovaWindow) {
        onStart(window)
        while (!quit) {
            window.processEvents(this)
            renderFrame()
            frameCount++
        }
        onEnd()
    }

    fun quit() {
        quit = true
    }

    fun handleInput(input: ButtonMap) {
        mainMenu.handleInput(input)
    }

    fun startVs() {
        omfConfig?.let {
            writeOmfConfig(false)
            startDosBox(saveReplays = novaConf.saveReplays, userconf = novaConf.userConf)
        }
    }

    fun startNormal() {
        omfConfig?.let {
            writeOmfConfig(true)
            startDosBox(mode = "advanced", saveReplays = novaConf.saveReplays, userconf = novaConf.userConf)
        }
    }

    fun startTraining() {
        omfConfig?.let {
            window.enableTraining()
            writeOmfConfig(false)
            startDosBox(saveReplays = false, userconf = false)
        }
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
        val args = listOfNotNull(
            exe,
            if(userconf) "-userconf" else null,
            "-noconsole",
            "-conf \"$dosboxConfPath\"",
            "-c \"mount c ${novaConf.omfPath}\"",
            "-c \"c:\"",
            "-c \"file0001 $omfParams\"",
            "-c \"exit\""
        )
        val command = args.joinToString(" ")
        window.executeCommand(executable = novaConf.dosboxPath, command = command)
        //This doesn't work for DOSBox staging! AAAAAAAAAAA
//        getFileSystem().openReadOnly("stdout.txt".toPath()).use {handle ->
//            handle.source().buffer().use {buffer ->
//                println(buffer.readUtf8Line())
//            }
//        }
    }

    fun dosBoxFinished() {
        mainMenu.reset()
    }

    private fun writeOmfConfig(singlePlayer: Boolean) {
        val configPath = novaConf.omfPath.toPath().div("SETUP.CFG")
        getFileSystem().openReadWrite(configPath, mustCreate = false, mustExist = false).use {
            omfConfig?.buildFile(it, singlePlayer)
        }
    }

    private fun onStart(window: NovaWindow) {
        this.window = window
        val background = loadPcx("NETARENA.PCX".toPath())?: error("Couldn't load background NETARENA.PCX")
        val font1image = loadPcx("NETFONT1.PCX".toPath())?: error("Couldn't load font NETFONT1.PCX")
        val font2image = loadPcx("NETFONT2.PCX".toPath())?: error("Couldn't load font NETFONT2.PCX")
        bgTex = window.loadTexture(background)

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

    fun loadOmfConfig() {
        val configPath = novaConf.omfPath.toPath().div(OMFConf.FILENAME)
        val fs = getFileSystem()
        if (fs.exists(configPath)) {
            fs.openReadOnly(configPath).use {
                omfConfig = OMFConf(it)
            }
        }
    }

    private fun onEnd() {
        window.destroy()
        novaConf.save()
    }

    fun showFileChooser(start: String, prompt: String): String {
        return window.showFileChooser(start, prompt)
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
    fun showFileChooser(start: String, prompt: String): String
    fun showFolderChooser(start: String, prompt: String): String
    fun setJoystickEnabled(joyEnabled: Boolean)
    fun destroy()
    fun enableTraining()
    fun loadFont(fontMapping: OmfFont, textureHandle: Int): Int
    fun loadTexture(image: PCXImage): Int
    fun loadTexturePng(path: String): Int
    fun loadTextureFromRaster(raster: UByteArray, width: Int, height: Int): Int
    fun showImage(textureHandle: Int, x: Int, y: Int)
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

fun ButtonMap.dosboxMap(scancodeMap: Map<Int, Int>): String {
    return when(type) {
        ControlType.KEY -> {
            "key ${scancodeMap[scancode]}"
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