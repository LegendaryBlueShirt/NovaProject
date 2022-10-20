package com.justnopoint.nova

import kotlinx.cinterop.*
import platform.posix.wchar_tVar
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
var trainingMode = false

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
        trainingHook = SetWindowsHookEx?.invoke(WH_KEYBOARD_LL, winKeyHook, null, 0u)

        return super.init()
    }

    override fun enableTraining() {
        trainingMode = true
    }

    override fun destroy() {
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
                trainingMode = false
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

    var inputToKeyMapping = emptyMap<ButtonMap, Int>()
    private fun createJoyToKeyMappings(conf: NovaConf) {
        val boundInputs = conf.getBoundInputs()
        val usedScancodes = boundInputs.filter { it.type == ControlType.KEY }.map { it.scancode }
        val possibleMappings = sdlKeyMapping.keys.filterNot { key ->
            usedScancodes.contains(key)
        }
        val nonKeyMappings = boundInputs.filterNot { it.type == ControlType.KEY }
        inputToKeyMapping = nonKeyMappings.zip(possibleMappings).toMap()
    }

    private val inputBuffer = MemScope().allocArray<INPUT>(1)
    override fun sendKeyEvent(mappedButton: ButtonMap, up: Boolean) {
        if(mappedButton.type == ControlType.KEY) return

        val desiredVirtInput = inputToKeyMapping[mappedButton] ?: return
        inputBuffer[0].type = INPUT_KEYBOARD.toUInt()
        inputBuffer[0].ki.wVk = sdlKeyMapping[desiredVirtInput] ?: return
        inputBuffer[0].ki.dwFlags = if(up) KEYEVENTF_KEYUP.toUInt() else 0u
        SendInput(1, inputBuffer, sizeOf<INPUT>().toInt())
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
    VIRT_L to 0x4C.toUShort()
)

actual fun getPossibleMappings(): Map<Int, Any> {
    return sdlKeyMapping
}