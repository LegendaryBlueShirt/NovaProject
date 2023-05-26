package com.justnopoint.nova

import SDL.*
import cnames.structs.SDL_Renderer
import cnames.structs.SDL_Texture
import cnames.structs.SDL_Window
import kotlinx.cinterop.*

abstract class NovaWindowSDL: NovaWindow {
    //* UI Pointers *//
    private var window: CPointer<SDL_Window>? = null
    private var renderer: CPointer<SDL_Renderer>? = null
    private val dstRect: SDL_Rect = MemScope().alloc()
    private val textures = mutableListOf<CPointer<SDL_Texture>>()
    private val fontRenderers = mutableListOf<OmfFontRenderer>()
    private val texW = MemScope().alloc<IntVar>()
    private val texH = MemScope().alloc<IntVar>()

    //* Joystick Management *//
    private var joyEnabled = false
    private val joysticks = mutableListOf<SdlInput>()

    open fun init(): Boolean {
        writeLog("Initializing SDL")
        if(SDL_Init(SDL_INIT_GAMECONTROLLER or SDL_INIT_VIDEO or SDL_INIT_JOYSTICK) != 0) {
            val error = SDL_GetError()
            showErrorPopup("SDL could not initialize!", "SDL_Error: $error")
            error("SDL could not initialize! SDL_Error: $error")
        }

        SDL_JoystickEventState(SDL_ENABLE)
        SDL_SetHint(SDL_HINT_JOYSTICK_ALLOW_BACKGROUND_EVENTS,"1")
        writeLog("Initializing controller mappings")
        try {
            SDL_GameControllerAddMappingsFromRW(SDL_RWFromFile("gamecontrollerdb.txt", "rb"), 1)
        } catch (e: Exception) {
            showErrorPopup("Couldn't load gamecontrollerdb.txt", e.message?:"Unknown Error")
            error("Couldn't load gamecontrollerdb.txt  ${e.message?:"Unknown Error"}")
        }

        window = SDL_CreateWindow("Nova Project", SDL_WINDOWPOS_UNDEFINED.toInt(), SDL_WINDOWPOS_UNDEFINED.toInt(), 640, 400, 0)
        renderer = SDL_CreateRenderer(window, -1, SDL_RENDERER_ACCELERATED)

        return renderer != null
    }

    private val event = MemScope().alloc<SDL_Event>()

    override fun processEvents(project: NovaProject) {
        while (SDL_PollEvent(event.ptr) != 0) {
            when(event.type) {
                SDL_QUIT -> project.quit()
                SDL_KEYDOWN -> {
                    val name = SDL_GetKeyName(event.key.keysym.sym)?.toKString()
                    val code = event.key.keysym.scancode
                    if(virtKeyMapping.containsKey(code)) {
                        val input = ButtonMap(type = ControlType.KEY, scancode = virtKeyMapping[code]!!, name = name)
                        project.handleInput(input)
                    }
                }
                SDL_JOYBUTTONUP,
                SDL_JOYBUTTONDOWN -> {
                    val instanceId = event.jbutton.which
                    joysticks.find { it.getCurrentId() == instanceId }?.updateControllerButton(event)
                }
                SDL_CONTROLLERBUTTONDOWN,
                SDL_CONTROLLERBUTTONUP -> {
                    val instanceId = event.cbutton.which
                    joysticks.find { it.getCurrentId() == instanceId }?.updateControllerButton(event)
                }
                SDL_CONTROLLERAXISMOTION -> {
                    val instanceId = event.caxis.which
                    joysticks.find { it.getCurrentId() == instanceId }?.updateControllerAxis(event)
                }
                SDL_JOYAXISMOTION -> {
                    val instanceId = event.jaxis.which
                    joysticks.find { it.getCurrentId() == instanceId }?.updateControllerAxis(event)
                }
                SDL_JOYHATMOTION -> {
                    val instanceId = event.jhat.which
                    joysticks.find { it.getCurrentId() == instanceId }?.updateControllerHat(event)
                }
                SDL_CONTROLLERDEVICEADDED,
                SDL_JOYDEVICEADDED -> {
                    if(joyEnabled) {
                        val id = event.jdevice.which
                        writeLog("SDL detected new device id:$id")
                        if(joysticks.find { it.getDeviceId() == id } != null) {
                            writeLog("Deduped.")
                        } else {
                            if (SDL_IsGameController(id) == SDL_TRUE) {
                                SdlController.open(id)?.let {
                                    joysticks.add(it)
                                }
                            } else {
                                SdlJoystick.open(id)?.let {
                                    joysticks.add(it)
                                }
                            }
                        }
                    }
                }
                SDL_CONTROLLERDEVICEREMOVED -> {
                    if(joyEnabled) {
                        val instanceId = event.cdevice.which
                        writeLog("SDL detected controller removed instanceId:$instanceId")
                        joysticks.find { it.getCurrentId() == instanceId }?.let {
                            it.close()
                            joysticks.remove(it)
                        }
                    }
                }
                SDL_JOYDEVICEREMOVED -> {
                    if(joyEnabled) {
                        val instanceId = event.jdevice.which
                        writeLog("SDL detected joystick removed instanceId:$instanceId")
                        joysticks.find { it.getCurrentId() == instanceId }?.let {
                            it.close()
                            joysticks.remove(it)
                        }
                    }
                }
            }
        }
        joysticks.forEach {
            if(joyEnabled) {
                while(it.getEvents().isNotEmpty()) {
                    val event = it.getEvents().removeFirst()
                    val controller = it.getCurrentId()
                    when(event.type) {
                        ControlType.AXIS -> {
                            val input = ButtonMap(
                                type = ControlType.AXIS,
                                controlId = controller,
                                axisId = event.id,
                                direction = event.direction,
                                name = "Joy $controller Axis ${event.id} ${if(event.direction == -1) "-" else "+"}"
                            )
                            project.handleInput(input, event.release)
                            if(trainingMode && !event.release) {
                                getTrainingModeButton(input)
                            }
                        }
                        ControlType.HAT -> {
                            val input = ButtonMap(
                                type = ControlType.HAT,
                                controlId = controller,
                                axisId = event.id,
                                direction = event.direction,
                                name = "Joy $controller Hat ${event.id} ${event.direction}"
                            )
                            project.handleInput(input, event.release)
                            if(trainingMode && !event.release) {
                                getTrainingModeButton(input)
                            }
                        }
                        ControlType.BUTTON -> {
                            val input = ButtonMap(
                                type = ControlType.BUTTON,
                                controlId = controller,
                                axisId = event.id,
                                direction = event.direction,
                                name = "Joy $controller Button ${event.id}")
                            project.handleInput(input, event.release)
                            if(trainingMode && !event.release) {
                                getTrainingModeButton(input)
                            }
                        }
                        else -> {}
                    }
                }
            } else {
                it.getEvents().clear()
            }
        }
    }

    override fun startRender() {
        SDL_RenderClear(renderer)
    }

    override fun endRender() {
        SDL_RenderPresent(renderer)
    }

    override fun showImage(textureHandle: Int, x: Int, y: Int) {
        val texture = textures[textureHandle]
        SDL_QueryTexture(texture, null, null, texW.ptr, texH.ptr)
        dstRect.x = x
        dstRect.y = y
        dstRect.w = texW.value * 2
        dstRect.h = texH.value * 2
        SDL_RenderCopy(renderer, texture, null, dstRect.ptr)
    }

    override fun showText(textLine: String, font: Int, x: Int, y: Int, align: TextAlignment) {
        when (align) {
            TextAlignment.LEFT -> fontRenderers[font].drawText(textLine, x, y, false)
            TextAlignment.RIGHT -> fontRenderers[font].drawText(textLine, x, y, true)
            TextAlignment.CENTER -> {
                fontRenderers[font].apply {
                    val (width, height) = getTextDimensions(textLine)
                    fontRenderers[font].drawText(textLine, x-width/2, y, false)
                }
            }
        }
    }

    override fun getControllerList(): List<Controller> {
        return joysticks
    }

    override fun setJoystickEnabled(joyEnabled: Boolean) {
        this.joyEnabled = joyEnabled
    }

    val fontSrcRect: SDL_Rect = MemScope().alloc()
    val fontDstRect: SDL_Rect = MemScope().alloc()

    private fun surfaceToTexture(surface: CPointer<SDL_Surface>?): Int {
        val tex = SDL_CreateTextureFromSurface(renderer, surface)
        SDL_FreeSurface(surface)
        if (tex == null) {
            return -1
        }
        textures.add(tex)
        return textures.size - 1
    }

    override fun loadTexture(image: PCXImage): Int {
        val surface = surfaceFromPcx(image)
        return surfaceToTexture(surface)
    }

    @OptIn(ExperimentalUnsignedTypes::class)
    override fun loadTextureFromRaster(raster: UByteArray, width: Int, height: Int): Int {
        val newSurface = SDL_CreateRGBSurface(0, width, height, 32, 0xffu, 0xff00u, 0xff0000u, 0xff000000u)
        SDL_memcpy(newSurface?.pointed?.pixels, raster.refTo(0), raster.size.toULong())
        return surfaceToTexture(newSurface)
    }
    override fun loadFont(fontMapping: OmfFont, textureHandle: Int): Int {
        val fontRenderer = object: OmfFontRenderer() {
            override val font: OmfFont
                get() = fontMapping
            override val scale: Float
                get() = 2f

            override fun drawCharacter(glyph: OmfFont.Glyph, character: Char, x: Int, y: Int, scale: Float) {
                fontSrcRect.x = glyph.x
                fontSrcRect.y = glyph.y
                fontSrcRect.w = glyph.w
                fontSrcRect.h = glyph.h
                fontDstRect.x = x
                fontDstRect.y = y
                fontDstRect.w = (glyph.w * scale).toInt()
                fontDstRect.h = (glyph.h * scale).toInt()
                SDL_RenderCopy(renderer, textures[textureHandle], fontSrcRect.ptr, fontDstRect.ptr)
            }
        }
        fontRenderers.add(fontRenderer)
        return fontRenderers.size-1
    }
}

val virtKeyMapping = mapOf(
    SDL_SCANCODE_ESCAPE to VIRT_ESC,
    SDL_SCANCODE_A to VIRT_A,
    SDL_SCANCODE_B to VIRT_B,
    SDL_SCANCODE_C to VIRT_C,
    SDL_SCANCODE_D to VIRT_D,
    SDL_SCANCODE_E to VIRT_E,
    SDL_SCANCODE_F to VIRT_F,
    SDL_SCANCODE_G to VIRT_G,
    SDL_SCANCODE_H to VIRT_H,
    SDL_SCANCODE_I to VIRT_I,
    SDL_SCANCODE_J to VIRT_J,
    SDL_SCANCODE_K to VIRT_K,
    SDL_SCANCODE_L to VIRT_L,
    SDL_SCANCODE_M to VIRT_M,
    SDL_SCANCODE_N to VIRT_N,
    SDL_SCANCODE_O to VIRT_O,
    SDL_SCANCODE_P to VIRT_P,
    SDL_SCANCODE_Q to VIRT_Q,
    SDL_SCANCODE_R to VIRT_R,
    SDL_SCANCODE_S to VIRT_S,
    SDL_SCANCODE_T to VIRT_T,
    SDL_SCANCODE_U to VIRT_U,
    SDL_SCANCODE_V to VIRT_V,
    SDL_SCANCODE_W to VIRT_W,
    SDL_SCANCODE_X to VIRT_X,
    SDL_SCANCODE_Y to VIRT_Y,
    SDL_SCANCODE_Z to VIRT_Z,
    SDL_SCANCODE_RETURN to VIRT_RETURN,
    SDL_SCANCODE_KP_ENTER to VIRT_KP_ENTER,
    SDL_SCANCODE_1 to VIRT_1,
    SDL_SCANCODE_2 to VIRT_2,
    SDL_SCANCODE_3 to VIRT_3,
    SDL_SCANCODE_4 to VIRT_4,
    SDL_SCANCODE_5 to VIRT_5,
    SDL_SCANCODE_6 to VIRT_6,
    SDL_SCANCODE_7 to VIRT_7,
    SDL_SCANCODE_8 to VIRT_8,
    SDL_SCANCODE_9 to VIRT_9,
    SDL_SCANCODE_0 to VIRT_0,
    SDL_SCANCODE_KP_1 to VIRT_KP_1,
    SDL_SCANCODE_KP_2 to VIRT_KP_2,
    SDL_SCANCODE_KP_3 to VIRT_KP_3,
    SDL_SCANCODE_KP_4 to VIRT_KP_4,
    SDL_SCANCODE_KP_5 to VIRT_KP_5,
    SDL_SCANCODE_KP_6 to VIRT_KP_6,
    SDL_SCANCODE_KP_7 to VIRT_KP_7,
    SDL_SCANCODE_KP_8 to VIRT_KP_8,
    SDL_SCANCODE_KP_9 to VIRT_KP_9,
    SDL_SCANCODE_KP_0 to VIRT_KP_0,
    SDL_SCANCODE_LEFTBRACKET to VIRT_LBRACKET,
    SDL_SCANCODE_RIGHTBRACKET to VIRT_RBRACKET,
    SDL_SCANCODE_BACKSLASH to VIRT_BCKSLSH,
    SDL_SCANCODE_BACKSPACE to VIRT_BKSP,
    SDL_SCANCODE_SPACE to VIRT_SPACE,
    SDL_SCANCODE_TAB to VIRT_TAB,
    SDL_SCANCODE_LSHIFT to VIRT_LSHIFT,
    SDL_SCANCODE_LCTRL to VIRT_LCTRL,
    SDL_SCANCODE_LALT to VIRT_LALT,
    SDL_SCANCODE_RALT to VIRT_RALT,
    SDL_SCANCODE_RCTRL to VIRT_RCTRL,
    SDL_SCANCODE_RSHIFT to VIRT_RSHIFT,
    SDL_SCANCODE_MINUS to VIRT_DASH,
    SDL_SCANCODE_EQUALS to VIRT_EQUALS,
    SDL_SCANCODE_INSERT to VIRT_INS,
    SDL_SCANCODE_DELETE to VIRT_DEL,
    SDL_SCANCODE_HOME to VIRT_HOME,
    SDL_SCANCODE_END to VIRT_END,
    SDL_SCANCODE_PAGEUP to VIRT_PGUP,
    SDL_SCANCODE_PAGEDOWN to VIRT_PGDN,
    SDL_SCANCODE_F1 to VIRT_F1,
    SDL_SCANCODE_F2 to VIRT_F2,
    SDL_SCANCODE_F3 to VIRT_F3,
    SDL_SCANCODE_F4 to VIRT_F4,
    SDL_SCANCODE_F5 to VIRT_F5,
    SDL_SCANCODE_F6 to VIRT_F6,
    SDL_SCANCODE_F7 to VIRT_F7,
    SDL_SCANCODE_F8 to VIRT_F8,
    SDL_SCANCODE_F9 to VIRT_F9,
    SDL_SCANCODE_F10 to VIRT_F10,
    SDL_SCANCODE_F11 to VIRT_F11,
    SDL_SCANCODE_F12 to VIRT_F12,
    SDL_SCANCODE_COMMA to VIRT_COMMA,
    SDL_SCANCODE_PERIOD to VIRT_PERIOD,
    SDL_SCANCODE_SLASH to VIRT_FWDSLSH,
    SDL_SCANCODE_CAPSLOCK to VIRT_CAPS,
    SDL_SCANCODE_GRAVE to VIRT_GRAV,
    SDL_SCANCODE_SEMICOLON to VIRT_COLON,
    SDL_SCANCODE_APOSTROPHE to VIRT_QUOT,
    SDL_SCANCODE_UP to VIRT_UP,
    SDL_SCANCODE_DOWN to VIRT_DOWN,
    SDL_SCANCODE_LEFT to VIRT_LEFT,
    SDL_SCANCODE_RIGHT to VIRT_RIGHT,
    SDL_SCANCODE_KP_DIVIDE to VIRT_KP_DIVIDE,
    SDL_SCANCODE_KP_MULTIPLY to VIRT_KP_MULTIPLY,
    SDL_SCANCODE_KP_MINUS to VIRT_KP_MINUS,
    SDL_SCANCODE_KP_ENTER to VIRT_KP_ENTER,
    SDL_SCANCODE_KP_PLUS to VIRT_KP_PLUS,
    SDL_SCANCODE_KP_PERIOD to VIRT_KP_PERIOD,
    SDL_SCANCODE_NUMLOCKCLEAR to VIRT_NUM,
    SDL_SCANCODE_PRINTSCREEN to VIRT_PRNTSCRN,
    SDL_SCANCODE_SCROLLLOCK to VIRT_SCRLCK,
    SDL_SCANCODE_PAUSE to VIRT_PAUSE,
)

actual fun getDosboxScancodeMap(): Map<Int, Int> {
    return virtKeyMapping.entries.associateBy({ it.value }) { it.value }
}

actual fun getDosboxStagingScancodeMap(): Map<Int, Int> {
    return virtKeyMapping.entries.associateBy({ it.value }) { it.key.toInt() }
}

val dosKeyMapping = mapOf(
    SDL_SCANCODE_A to DOS_A,
    SDL_SCANCODE_B to DOS_B,
    SDL_SCANCODE_C to DOS_C,
    SDL_SCANCODE_D to DOS_D,
    SDL_SCANCODE_E to DOS_E,
    SDL_SCANCODE_F to DOS_F,
    SDL_SCANCODE_G to DOS_G,
    SDL_SCANCODE_H to DOS_H,
    SDL_SCANCODE_I to DOS_I,
    SDL_SCANCODE_J to DOS_J,
    SDL_SCANCODE_K to DOS_K,
    SDL_SCANCODE_L to DOS_L,
    SDL_SCANCODE_M to DOS_M,
    SDL_SCANCODE_N to DOS_N,
    SDL_SCANCODE_O to DOS_O,
    SDL_SCANCODE_P to DOS_P,
    SDL_SCANCODE_Q to DOS_Q,
    SDL_SCANCODE_R to DOS_R,
    SDL_SCANCODE_S to DOS_S,
    SDL_SCANCODE_T to DOS_T,
    SDL_SCANCODE_U to DOS_U,
    SDL_SCANCODE_V to DOS_V,
    SDL_SCANCODE_W to DOS_W,
    SDL_SCANCODE_X to DOS_X,
    SDL_SCANCODE_Y to DOS_Y,
    SDL_SCANCODE_Z to DOS_Z,
    SDL_SCANCODE_RETURN to DOS_RETURN,
    SDL_SCANCODE_KP_ENTER to DOS_RETURN,
    SDL_SCANCODE_1 to DOS_1,
    SDL_SCANCODE_2 to DOS_2,
    SDL_SCANCODE_3 to DOS_3,
    SDL_SCANCODE_4 to DOS_4,
    SDL_SCANCODE_5 to DOS_5,
    SDL_SCANCODE_6 to DOS_6,
    SDL_SCANCODE_7 to DOS_7,
    SDL_SCANCODE_8 to DOS_8,
    SDL_SCANCODE_9 to DOS_9,
    SDL_SCANCODE_0 to DOS_0,
)