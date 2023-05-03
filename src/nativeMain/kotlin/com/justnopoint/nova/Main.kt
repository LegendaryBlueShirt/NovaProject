package com.justnopoint.nova

import kotlinx.cinterop.*
import platform.posix.*
import platform.windows.*
import tinyfiledialogs.*

actual fun getNativeWindow(): NovaWindow? {
    val container = Win32Container()
    return if(container.init()) {
        container
    } else {
        null
    }
}

var processInfo: CPointer<PROCESS_INFORMATION>? = null
val processExitCode = MemScope().alloc<DWORDVar>()
var logFile: CPointer<FILE>? = null

class Win32Container: NovaWindowSDL() {
    //* Training Mode Input Handling *//
    private var trainingHook: HHOOK? = null
    private val winKeyHook = staticCFunction { nCode: Int, wParam: WPARAM, lParam: LPARAM ->
        if (wParam.toInt() == WM_KEYDOWN && nCode == HC_ACTION) {
            handleWinKey(lParam)
        }
        return@staticCFunction CallNextHookEx(null, nCode, wParam, lParam)
    }


    override fun init(): Boolean {
        if(!debug) {
            logFile = freopen("logfile.txt", "a", stdout)
            _dup2(1, 2)
        }

        trainingHook = SetWindowsHookEx?.invoke(WH_KEYBOARD_LL, winKeyHook, null, 0u)

        return super.init()
    }

    override fun destroy() {
        if(!debug) {
            fclose(logFile)
        }

        UnhookWindowsHookEx(trainingHook)
    }

    private val winmsg = MemScope().alloc<tagMSG>().ptr
    override fun processEvents(project: NovaProject) {
        super.processEvents(project)

        //DOSBox process handler
        processInfo?.pointed?.let {
            GetExitCodeProcess(it.hProcess, processExitCode.ptr)
            if(processExitCode.value != STILL_ACTIVE) {
                CloseHandle(it.hProcess)
                CloseHandle(it.hThread)
                processInfo = null
                entryPointer = 0L
                project.dosBoxFinished()
            } else if(entryPointer == 0L) {
                entryPointer = getBaseAddress(it.hProcess)
                if (entryPointer != 0L) {
                    println("Entry pointer at $entryPointer")
                    createJoyToKeyMappings(project.novaConf)
                }
            }
        }

        //Training mode handling
        while(PeekMessage?.invoke(winmsg, null, 0u, 0u, PM_REMOVE.toUInt()) != 0) {
            TranslateMessage(winmsg)
            DispatchMessage?.invoke(winmsg)
        }
    }

    var dummyMapper = emptyMap<ButtonMap, ButtonMap>()
    var inputToKeyMapping = emptyMap<ButtonMap, Int>()
    private fun createJoyToKeyMappings(conf: NovaConf) {
        dummyMapper = mapOf(
            conf.p1Config.up to conf.p2Config.up,
            conf.p1Config.down to conf.p2Config.down,
            conf.p1Config.left to conf.p2Config.left,
            conf.p1Config.right to conf.p2Config.right,
            conf.p1Config.punch to conf.p2Config.punch,
            conf.p1Config.kick to conf.p2Config.kick
        )
        val boundInputs = conf.getBoundInputs(includeEsc = true)
        val usedScancodes = boundInputs.filter { it.type == ControlType.KEY }.map { it.scancode }
        val possibleMappings = sdlKeyMapping.keys.filterNot { key ->
            usedScancodes.contains(key)
        }
        val nonKeyMappings = boundInputs.filterNot { it.type == ControlType.KEY }
        inputToKeyMapping = nonKeyMappings.zip(possibleMappings).toMap()
    }

    private val inputBuffer = MemScope().allocArray<INPUT>(1).also {
        it[0].type = INPUT_KEYBOARD.toUInt()
    }
    override fun sendKeyEvent(mappedButton: ButtonMap, up: Boolean, useDummy: Boolean, recorded: Boolean) {
        if(mappedButton.type == ControlType.KEY && !recorded) return

        if(useDummy || recorded) {
            val newInput = if(useDummy) {
                dummyMapper[mappedButton]
            } else if(recorded) {
                mappedButton
            } else {
                null
            }
           newInput?.let { newButton ->
                if(recordingActive) {
                    recordInput(newButton, up)
                }
                if(newButton.type == ControlType.KEY) {
                    inputBuffer[0].ki.wVk = sdlKeyMapping[newButton.scancode] ?: return

                } else {
                    val desiredVirtInput = inputToKeyMapping[newButton] ?: return
                    inputBuffer[0].ki.wVk = sdlKeyMapping[desiredVirtInput] ?: return
                }
                inputBuffer[0].ki.dwFlags = if (up) KEYEVENTF_KEYUP.toUInt() else 0u
                SendInput(1, inputBuffer, sizeOf<INPUT>().toInt())
            }
        } else {
            val desiredVirtInput = inputToKeyMapping[mappedButton] ?: return
            inputBuffer[0].ki.wVk = sdlKeyMapping[desiredVirtInput] ?: return
            inputBuffer[0].ki.dwFlags = if (up) KEYEVENTF_KEYUP.toUInt() else 0u
            SendInput(1, inputBuffer, sizeOf<INPUT>().toInt())
        }
    }

    override fun executeCommand(executable: String, command: String) {
        processInfo = executeCommandNative(executable, command)
        //dosboxWindow = SDL_CreateWindowFrom(hwnd)?.pointed
        //com.justnopoint.nova.getProcessInfo?.pointed?.hProcess
    }

    override fun showFolderChooser(start: String, prompt: String): String {
        val selectedDirectory: CPointer<wchar_tVar>? =
            tinyfd_selectFolderDialogW(
                aTitle = prompt.wcstr,
                aDefaultPath = start.wcstr)
        return selectedDirectory?.toKStringFromUtf16()?:""
    }

    override fun showFileChooser(start: String, prompt: String, filter: String, filterDesc: String): String {
        val filters = MemScope().allocArrayOf(filter.wcstr.getPointer(MemScope()))
        val selectedFile: CPointer<wchar_tVar>? =
            tinyfd_openFileDialogW(
                aTitle = prompt.wcstr,
                aDefaultPathAndFile = start.wcstr,
                aAllowMultipleSelects = 0,
                aFilterPatterns = filters,
                aNumOfFilterPatterns = 1,
                aSingleFilterDescription = filterDesc.wcstr
            )
        return selectedFile?.toKStringFromUtf16()?:""
    }
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
                val index = sdlKeyMapping.values.indexOf(key.vkCode.toUShort())
                if(index != -1) {
                    getTrainingModeInput(sdlKeyMapping.keys.elementAt(index))
                }
            }
        }
    }
}

actual fun readMemoryByte(address: Long): UByte {
    readMemoryValue(address, bytePointer.ptr, 1u)
    return bytePointer.value
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
    val result = ReadProcessMemory(
        processInfo?.pointed?.hProcess,
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

actual fun writeMemoryString(address: Long, value: String, limit: Int) {
    var charactersWritten = 0
    value.utf8.getBytes().take(limit).forEach { byte ->
        writeMemoryByte(address + charactersWritten, byte.toUByte())
        charactersWritten++
    }
    while(charactersWritten < limit) {
        writeMemoryByte(address + charactersWritten, 0u)
        charactersWritten++
    }
}

fun writeMemoryValue(offset: Long, input: CPointer<out CPointed>, inputSize: ULong) {
    if(entryPointer == 0L) return
    val result = WriteProcessMemory(
        processInfo?.pointed?.hProcess,
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
            if((mem.RegionSize == 0x1001000uL) && (mem.AllocationProtect == 4u)) {
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
        val si = alloc<STARTUPINFO>()
        si.dwFlags = si.dwFlags or STARTF_USESTDHANDLES.toUInt()
        si.hStdInput = null
        si.hStdError = stderr
        si.hStdOutput = stdout

        val result = CreateProcess?.invoke(
            executable.wcstr.ptr,
            command.wcstr.ptr,
            null,
            null,
            0,
            0u,
            null,
            null,
            si.ptr,
            pi
        )
        if(result == 0) {
            error("Could not start program $executable - Error ${GetLastError()}")
        }
    }
    return pi
}

val time: SYSTEMTIME = MemScope().alloc()
actual fun writeLog(message: String) {
    if(debug) {
        println(message)
    } else {
        GetSystemTime(time.ptr)
        val formattedTime =
            "[${time.wMonth}-${time.wDay}-${time.wYear} ${time.wHour}:${time.wMinute}:${time.wSecond}] $message\n"
        fprintf(stdout, formattedTime)
    }
}
actual fun showErrorPopup(title: String, message: String) {
    if(debug) {
        println("$title - $message")
    } else {
        GetSystemTime(time.ptr)
        val formattedTime =
            "[${time.wMonth}-${time.wDay}-${time.wYear} ${time.wHour}:${time.wMinute}:${time.wSecond}] $title - $message\n"
        fprintf(stderr, formattedTime)
    }
    tinyfd_messageBox(title, message, "ok", "error", 1)
}

val trainingKeyMapping = mapOf(
    VK_F1 to VIRT_F1,
    VK_F2 to VIRT_F2,
    VK_F3 to VIRT_F3
)

val sdlKeyMapping = mapOf(
    VIRT_A to 0x41.toUShort(),
    VIRT_B to 0x42.toUShort(),
    VIRT_C to 0x43.toUShort(),
    VIRT_D to 0x44.toUShort(),
    VIRT_E to 0x45.toUShort(),
    VIRT_F to 0x46.toUShort(),
    VIRT_G to 0x47.toUShort(),
    VIRT_H to 0x48.toUShort(),
    VIRT_I to 0x49.toUShort(),
    VIRT_J to 0x4A.toUShort(),
    VIRT_K to 0x4B.toUShort(),
    VIRT_L to 0x4C.toUShort(),
    VIRT_M to 0x4D.toUShort(),
    VIRT_N to 0x4E.toUShort(),
    VIRT_O to 0x4F.toUShort(),
    VIRT_P to 0x50.toUShort(),
    VIRT_Q to 0x51.toUShort(),
    VIRT_R to 0x52.toUShort(),
    VIRT_S to 0x53.toUShort(),
    VIRT_T to 0x54.toUShort(),
    VIRT_U to 0x55.toUShort(),
    VIRT_V to 0x56.toUShort(),
    VIRT_W to 0x57.toUShort(),
    VIRT_X to 0x58.toUShort(),
    VIRT_Y to 0x59.toUShort(),
    VIRT_Z to 0x5A.toUShort(),

    VIRT_BKSP to 0x08.toUShort(),
    VIRT_TAB to 0x09.toUShort(),

    VIRT_RETURN to 0x0D.toUShort(),

    VIRT_RALT to 0x12.toUShort(),
    VIRT_PAUSE to 0x13.toUShort(),
    VIRT_CAPS to 0x14.toUShort(),

    VIRT_SPACE to 0x20.toUShort(),
    VIRT_PGUP to 0x21.toUShort(),
    VIRT_PGDN to 0x22.toUShort(),
    VIRT_END to 0x23.toUShort(),
    VIRT_HOME to 0x24.toUShort(),
    VIRT_LEFT to 0x25.toUShort(),
    VIRT_UP to 0x26.toUShort(),
    VIRT_RIGHT to 0x27.toUShort(),
    VIRT_DOWN to 0x28.toUShort(),

    VIRT_INS to 0x2D.toUShort(),
    VIRT_DEL to 0x2E.toUShort(),

    VIRT_0 to 0x30.toUShort(),
    VIRT_1 to 0x31.toUShort(),
    VIRT_2 to 0x32.toUShort(),
    VIRT_3 to 0x33.toUShort(),
    VIRT_4 to 0x34.toUShort(),
    VIRT_5 to 0x35.toUShort(),
    VIRT_6 to 0x36.toUShort(),
    VIRT_7 to 0x37.toUShort(),
    VIRT_8 to 0x38.toUShort(),
    VIRT_9 to 0x39.toUShort(),

    VIRT_KP_0 to 0x60.toUShort(),
    VIRT_KP_1 to 0x61.toUShort(),
    VIRT_KP_2 to 0x62.toUShort(),
    VIRT_KP_3 to 0x63.toUShort(),
    VIRT_KP_4 to 0x64.toUShort(),
    VIRT_KP_5 to 0x65.toUShort(),
    VIRT_KP_6 to 0x66.toUShort(),
    VIRT_KP_7 to 0x67.toUShort(),
    VIRT_KP_8 to 0x68.toUShort(),
    VIRT_KP_9 to 0x69.toUShort(),
    VIRT_KP_MULTIPLY to 0x6A.toUShort(),
    VIRT_KP_PLUS to 0x6B.toUShort(),

    VIRT_KP_MINUS to 0x6D.toUShort(),
    VIRT_KP_PERIOD to 0x6E.toUShort(),
    VIRT_KP_DIVIDE to 0x6F.toUShort(),
    VIRT_F1 to 0x70.toUShort(),
    VIRT_F2 to 0x71.toUShort(),
    VIRT_F3 to 0x72.toUShort(),
    VIRT_F4 to 0x73.toUShort(),
    VIRT_F5 to 0x74.toUShort(),
    VIRT_F6 to 0x75.toUShort(),
    VIRT_F7 to 0x76.toUShort(),
    VIRT_F8 to 0x77.toUShort(),
    VIRT_F9 to 0x78.toUShort(),
    VIRT_F10 to 0x79.toUShort(),
    VIRT_F11 to 0x7A.toUShort(),
    VIRT_F12 to 0x7B.toUShort(),

    VIRT_NUM to 0x90.toUShort(),
    VIRT_SCRLCK to 0x91.toUShort(),

    VIRT_LSHIFT to 0xA0.toUShort(),
    VIRT_RSHIFT to 0xA1.toUShort(),
    VIRT_LCTRL to 0xA2.toUShort(),
    VIRT_RCTRL to 0xA3.toUShort(),
    VIRT_LALT to 0xA4.toUShort(),
    VIRT_RALT to 0xA5.toUShort(),

    VIRT_COLON to 0xBA.toUShort(),
    VIRT_EQUALS to 0xBB.toUShort(),
    VIRT_COMMA to 0xBC.toUShort(),
    VIRT_DASH to 0xBD.toUShort(),
    VIRT_PERIOD to 0xBE.toUShort(),
    VIRT_FWDSLSH to 0xBF.toUShort(),
    VIRT_GRAV to 0xC0.toUShort(),

    VIRT_LBRACKET to 0xDB.toUShort(),
    VIRT_BCKSLSH to 0xDC.toUShort(),
    VIRT_RBRACKET to 0xDD.toUShort(),
    VIRT_QUOT to 0xDE.toUShort(),
)

actual fun getPossibleMappings(): Map<Int, Any> {
    return sdlKeyMapping
}