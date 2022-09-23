import kiss_SDL.*
import kotlinx.cinterop.*
import platform.posix.wchar_tVar
import platform.windows.*
import tinyfiledialogs.*

actual fun getNativeWindow(): NovaWindow? {
    val container = KissContainer()
    return if(initSDL(container)) {
        container.apply {
            buildUi()
        }
    } else {
        null
    }
}

var processInfo: CPointer<PROCESS_INFORMATION>? = null
val processExitCode = MemScope().alloc<DWORDVar>()
var trainingMode = false

class KissContainer: NovaWindow {
    private var renderer: CPointer<SDL_Renderer>? = null
    private val objects = MemScope().alloc<kiss_array>().ptr
    private val window = MemScope().alloc<kiss_window>().ptr
    private val text = MemScope().alloc<kiss_textbox>().ptr
    private val winobj = MemScope().alloc<kiss_array>().ptr
    private var joyEnabled = false
    private val joystickDevices = mutableMapOf<Int, CPointer<SDL_Joystick>>()
    private val joystickIdToDevice = mutableMapOf<Int, Int>()
    data class WindowContainer(var dosboxWindow: CPointer<SDL_Window>? = null, var dosboxRenderer: CPointer<SDL_Renderer>? = null)
    private val containerRef = StableRef.create(WindowContainer())
    //private var dosboxWindow: SDL_Window? = null
    private var dosboxMessage: CPointer<SDL_Texture>? = null
    private var dosboxCopyBuffer: CPointer<SDL_Surface>? = null
    private var dosboxCopyTex: CPointer<SDL_Texture>? = null
    private var dosboxRect: SDL_Rect = MemScope().alloc()

    private val winKeyHook = staticCFunction { nCode: Int, wParam: WPARAM, lParam: LPARAM ->
        if (wParam.toInt() == WM_KEYDOWN && nCode == HC_ACTION) {
            handleWinKey(lParam)
        }
        return@staticCFunction CallNextHookEx(null, nCode, wParam, lParam)
    }

    private var trainingHook: HHOOK? = null
    fun init(): Boolean {
        renderer = kiss_init("Nova Project".cstr, objects, 320, 240)

        trainingHook = SetWindowsHookEx?.invoke(WH_KEYBOARD_LL, winKeyHook, null, 0u)

        return renderer != null
    }

    override fun destroy() {
        UnhookWindowsHookEx(trainingHook)
    }

    fun buildUi() {
        kiss_array_new(winobj)
        kiss_array_append(objects, ARRAY_TYPE.toInt(), winobj)
        kiss_window_new(window, null, 1, 0, 0, 320, 240)
        kiss_textbox_new(text, window, 1, winobj, 10, 10, 300, 220)
        kiss_array_new(text.pointed.array)
        window.pointed.visible = 1
    }

    fun buildDosboxUi(dosboxWindow: CPointer<SDL_Window>?,dosboxRenderer: CPointer<SDL_Renderer>) {
        val font = TTF_OpenFont("kiss_font.ttf", 24)
        val fontColor: SDL_Color = MemScope().alloc()
        fontColor.r = 255u
        fontColor.g = 255u
        fontColor.b = 255u
        val helloWorld = TTF_RenderText_Solid(font, "Hello World", fontColor.readValue())
        dosboxMessage = SDL_CreateTextureFromSurface(dosboxRenderer, helloWorld)
        dosboxRect.w = helloWorld?.pointed?.w?:0
        dosboxRect.h = helloWorld?.pointed?.h?:0
        SDL_FreeSurface(helloWorld)
        dosboxRect.x = 0
        dosboxRect.y = 0
        val width = MemScope().alloc<IntVar>()
        val height = MemScope().alloc<IntVar>()
        SDL_GL_GetDrawableSize(dosboxWindow, width.ptr, height.ptr)
        dosboxCopyBuffer = SDL_CreateRGBSurface(0, width.value, height.value, 32, 0x00ff0000, 0x0000ff00, 0x000000ff, 0xff000000)
        dosboxCopyTex = SDL_CreateTextureFromSurface(dosboxRenderer, dosboxCopyBuffer)
    }

    override fun render() {
        SDL_RenderClear(renderer)

        kiss_window_draw(window, renderer)
        kiss_textbox_draw(text, renderer)

        SDL_RenderPresent(renderer)

        val container = containerRef.get()
        container.dosboxRenderer?.let {
            if(dosboxMessage == null) {
                buildDosboxUi(container.dosboxWindow, it)
            }
            dosboxCopyBuffer?.pointed?.let { buffer ->
                SDL_RenderReadPixels(it, null,
                    SDL_GetWindowPixelFormat(container.dosboxWindow), buffer.pixels, buffer.pitch)
                SDL_UpdateTexture(dosboxCopyTex, null, buffer.pixels, buffer.pitch)
            }
            //SDL_RenderClear(it)

            SDL_RenderCopy(it, dosboxCopyTex, null, null)
            SDL_RenderCopy(it, dosboxMessage, null, dosboxRect.ptr)

            SDL_RenderPresent(it)
        }
    }

    val event = MemScope().allocArray<SDL_Event>(1)
    val winmsg = MemScope().alloc<tagMSG>().ptr
    override fun processEvents(project: NovaProject) {
        memScoped {
            while (SDL_PollEvent(event) != 0) {
                when(event[0].type) {
                    SDL_QUIT -> project.quit()
                    SDL_KEYDOWN -> {
                        val name = SDL_GetKeyName(event[0].key.keysym.sym)?.toKString()
                        val code = event[0].key.keysym.scancode
                        if(code == SDL_SCANCODE_ESCAPE) {
                            project.quit()
                        } else {
                            if(virtKeyMapping.containsKey(code)) {
                                val input = ButtonMap(type = ControlType.KEY, scancode = virtKeyMapping[code]!!, name = name)
                                project.handleInput(input)
                            }
                        }
                    }
                    SDL_JOYBUTTONDOWN -> {
                        val controller = getActualControllerId(event[0].jbutton.which)
                        val button = event[0].jbutton.button
                        val input = ButtonMap(type = ControlType.BUTTON, controlId = controller, axisId = button.toInt(), name = "Joy $controller Button $button")
                        project.handleInput(input)
                    }
                    SDL_JOYAXISMOTION -> {
                        val value = event[0].jaxis.value
                        val dir = if(value < -3200) 0 else if(value > 3200) 1 else 2
                        if(dir < 2) {
                            val controller = getActualControllerId(event[0].jaxis.which)
                            val axis = event[0].jaxis.axis
                            val input = ButtonMap(
                                type = ControlType.AXIS,
                                controlId = controller,
                                axisId = axis.toInt(),
                                direction = dir,
                                name = "Joy $controller Axis $axis ${if(dir == 0) "-" else "+"}"
                            )
                            project.handleInput(input)
                        }
                    }
                    SDL_JOYHATMOTION -> {
                        val dir = event[0].jhat.value.toInt()
                        if(dir != 0) {
                            val controller = getActualControllerId(event[0].jhat.which)
                            val hatId = event[0].jhat.hat
                            val input = ButtonMap(
                                type = ControlType.HAT,
                                controlId = controller,
                                axisId = hatId.toInt(),
                                direction = dir,
                                name = "Joy $controller Hat $hatId $dir"
                            )
                            project.handleInput(input)
                        }
                    }
                    SDL_JOYDEVICEADDED -> {
                        if(joyEnabled) {
                            println("Adding joystick ${event[0].jdevice.which}")
                            val id = event[0].jdevice.which
                            SDL_JoystickOpen(id)?.let {
                                joystickDevices[id] = it
                                joystickIdToDevice[SDL_JoystickGetDeviceInstanceID(id)] = id
                            }
                        }
                    }
                    SDL_JOYDEVICEREMOVED -> {
                        if(joyEnabled) {
                            println("Removing joystick ${event[0].jdevice.which}")
                            val instanceId = event[0].jdevice.which
                            val deviceId = joystickIdToDevice[instanceId]?: continue
                            joystickDevices[deviceId]?.let {
                                if (SDL_JoystickGetAttached(it) != 0u) {
                                    SDL_JoystickClose(it)
                                }
                                joystickDevices.remove(deviceId)
                                joystickIdToDevice.remove(instanceId)
                            }
                        }
                    }
                }
            }
            processInfo?.pointed?.let {
                GetExitCodeProcess(it.hProcess,processExitCode.ptr)
                if(processExitCode.value != STILL_ACTIVE) {
                    CloseHandle(it.hProcess)
                    CloseHandle(it.hThread)
                    processInfo = null
                    entryPointer = 0L
                    trainingMode = false
                    project.dosBoxFinished()
                    containerRef.get().dosboxWindow = null
                    containerRef.get().dosboxRenderer = null
                } else if(entryPointer == 0L) {
                    entryPointer = getBaseAddress(it.hProcess)
                    if (entryPointer != 0L) {
                        println("Entry pointer at $entryPointer")
                    } else {}
                } else if(null == containerRef.get().dosboxWindow) {
//                    EnumThreadWindows(it.dwThreadId, staticCFunction{hwnd, param ->
//                        //val threadId = GetWindowThreadProcessId(hwnd, null)
//                        if(hwnd != null) {
//                            val containerPtr: COpaquePointer? = interpretCPointer(nativeNullPtr + param)
//                            val myContainer = containerPtr?.asStableRef<WindowContainer>()
//                            myContainer?.get()?.apply {
//                                dosboxWindow = SDL_CreateWindowFrom(hwnd)
//                                dosboxRenderer = SDL_CreateRenderer(dosboxWindow, -1, 0)
//                            }
//                            println("Woooo")
//                        }
//                        return@staticCFunction 0
//                    }, containerRef.asCPointer().toLong())
                }
            }
        }
        while(PeekMessage?.invoke(winmsg, null, 0u, 0u, PM_REMOVE.toUInt()) != 0) {
            TranslateMessage(winmsg)
            DispatchMessage?.invoke(winmsg)
        }
    }

    override fun clearText() {
        kiss_array_free(text.pointed.array)
        kiss_array_new(text.pointed.array)
    }

    override fun showText(textLine: String) {
        kiss_array_appendstring(text.pointed.array, 0, textLine.cstr, null)
    }

    override fun executeCommand(executable: String, command: String) {
        processInfo = executeCommandNative(executable, command)
        //dosboxWindow = SDL_CreateWindowFrom(hwnd)?.pointed
        //processInfo?.pointed?.hProcess
    }

    override fun enableTraining() {
        trainingMode = true
    }

    override fun showFolderChooser(start: String, prompt: String): String {
        val selectedDirectory: CPointer<wchar_tVar>? =
            tinyfd_selectFolderDialogW(
                aTitle = prompt.wcstr,
                aDefaultPath = start.wcstr)
        return selectedDirectory?.toKStringFromUtf16()?:""
    }

    override fun showFileChooser(start: String, prompt: String): String {
        val filters = MemScope().allocArrayOf("*.exe".wcstr.getPointer(MemScope()))
        val selectedFile: CPointer<wchar_tVar>? =
            tinyfd_openFileDialogW(
                aTitle = prompt.wcstr,
                aDefaultPathAndFile = start.wcstr,
                aAllowMultipleSelects = 0,
                aFilterPatterns = filters,
                aNumOfFilterPatterns = 1,
                aSingleFilterDescription = "executable files".wcstr
            )
        return selectedFile?.toKStringFromUtf16()?:""
    }

    override fun setJoystickEnabled(joyEnabled: Boolean) {
        this.joyEnabled = joyEnabled
        if(joyEnabled) {
            val joys = SDL_NumJoysticks()
            (0 until joys).forEach { id ->
                SDL_JoystickOpen(id)?.let {
                    joystickDevices[id] = it
                    joystickIdToDevice[SDL_JoystickGetDeviceInstanceID(id)] = id
                }
            }
        } else {
            joystickDevices.forEach { (_, joy) ->
                if (SDL_JoystickGetAttached(joy) != 0u) {
                    SDL_JoystickClose(joy)
                }
            }
            joystickIdToDevice.clear()
            joystickDevices.clear()
        }
    }

    private fun getActualControllerId(controllerIndex: Int): Int {
        return joystickIdToDevice.keys.indexOf(controllerIndex)
    }
}

fun initSDL(container: KissContainer): Boolean {
    var success = true

    if(!container.init()) {
        success = false
    }else if( SDL_Init(SDL_INIT_GAMECONTROLLER or SDL_INIT_VIDEO) != 0) {
        println("SDL could not initialize! SDL_Error: ${SDL_GetError()}")
        success = false
    }
    SDL_JoystickEventState(SDL_ENABLE)
    return success
}

var entryPointer = 0L
val bytePointer = MemScope().alloc<UCHAR>(0u)
val shortPointer = MemScope().alloc<UWORD>(0u)
val intPointer = MemScope().alloc<ULONG>(0u)

fun handleWinKey(keyPtr: Long) {
    if(!trainingMode) return
    processInfo?.pointed?.let { process ->
        GetExitCodeProcess(process.hProcess, processExitCode.ptr)
        if (processExitCode.value == STILL_ACTIVE) {
            keyPtr.toCPointer<tagKBDLLHOOKSTRUCT>()?.let { keyHook ->
                val key = keyHook.pointed
                trainingKeyMapping[key.vkCode.toInt()]?.let {
                    getTrainingModeInput(it)
                }
            }
        }
    }
}

actual fun readMemoryShort(address: Long): UShort {
    readMemoryValue(address, shortPointer.ptr, 2u)
    return shortPointer.value
}

actual fun readMemoryInt(address: Long): UInt {
    readMemoryValue(address, intPointer.ptr, 4u)
    return intPointer.value
}
fun readMemoryValue(offset: Long, output: CPointer<out CPointed>, outputSize: ULong) {
    if(entryPointer == 0L) return
    val result = ReadProcessMemory(processInfo?.pointed?.hProcess,
        interpretCPointer(nativeNullPtr + entryPointer + offset), output, outputSize, null)
    if (result == 0) {
        println("Error! ${GetLastError()}")
    }
}

actual fun writeMemoryByte(address: Long, value: UByte) {
    bytePointer.value = value
    writeMemoryValue(address, bytePointer.ptr, 1u)
}

actual fun writeMemoryShort(address: Long, value: UShort) {
    shortPointer.value = value
    writeMemoryValue(address, shortPointer.ptr, 2u)
}

actual fun writeMemoryInt(address: Long, value: UInt) {
    intPointer.value = value
    writeMemoryValue(address, intPointer.ptr, 4u)
}
fun writeMemoryValue(offset: Long, input: CPointer<out CPointed>, inputSize: ULong) {
    if(entryPointer == 0L) return
    val result = WriteProcessMemory(processInfo?.pointed?.hProcess,
        interpretCPointer(nativeNullPtr + entryPointer + offset), input, inputSize, null)
    if (result == 0) {
        println("Error! ${GetLastError()}")
    }
}

// Code adapted from a cheatengine script
// https://fearlessrevolution.com/viewtopic.php?f=11&t=7264
// Compatibility with multiple versions of DOSBox would frankly be impossible without this.
fun getBaseAddress(handle: HANDLE?): Long {
    val mem = MemScope().alloc<MEMORY_BASIC_INFORMATION>()
    var pointer = interpretCPointer<ULONGVar>(nativeNullPtr)
    while(true) {
        val result = VirtualQueryEx(handle, pointer, mem.ptr, sizeOf<MEMORY_BASIC_INFORMATION>().toULong())
        if(result == 0uL) {
            return 0
        } else {
            if((mem.RegionSize >= 0x1001000uL) && (mem.AllocationProtect == 4u)) {
                break
            }
            pointer = interpretCPointer(nativeNullPtr + pointer.toLong() + mem.RegionSize.toLong())
        }
    }
    return mem.BaseAddress.toLong() + 32
}

fun executeCommandNative(executable: String, command: String): CPointer<PROCESS_INFORMATION> {
    val pi = MemScope().alloc<PROCESS_INFORMATION>().ptr
    memScoped {
        val si = alloc<STARTUPINFO>().ptr
        val result = CreateProcess?.invoke(
            executable.wcstr.ptr,
            command.wcstr.ptr,
            null,
            null,
            0,
            0u,
            null,
            null,
            si,
            pi
        )
        if(result == 0) {
            error("Couldn't start program $executable - Error ${GetLastError()}")
        }
    }
    return pi
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

val trainingKeyMapping = mapOf(
    VK_F1 to VIRT_F1,
    VK_F2 to VIRT_F2,
    VK_F3 to VIRT_F3
)

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