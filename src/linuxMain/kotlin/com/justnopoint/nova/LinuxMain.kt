package com.justnopoint.nova

import kotlinx.cinterop.*
import platform.posix.*
import platform.posix.FILE
import platform.posix.__u_char
import platform.posix.__u_short
import tinyfiledialogs.*
import xtest.*
import xtest.__u_int
import xtest.__u_long
import xtest.tm

actual fun getNativeWindow(): NovaWindow? {
    val container = LinuxContainer()
    return if(container.init()) {
        container
    } else {
        null
    }
}

var pid: Int? = null
var childTerminated = false
val signalHandler = staticCFunction { signal: Int ->
    println("Received child signal $signal")
    pid?.let {
        while (waitpid(it, null, WNOHANG) > 0) {/*No-op*/
        }

        val check = kill(it, 0)
        if(check != 0) {
            pid = null
            childTerminated = true
        }
    }
    //pid = null
    //childTerminated = true
    val n = 0
}
var logFile: CPointer<FILE>? = null
val time: tm = MemScope().alloc()
var baseAddress: Long = 0L
val bytePointer = MemScope().alloc<__u_char>(0u)
val shortPointer = MemScope().alloc<__u_short>(0u)
val intPointer = MemScope().alloc<__u_int>(0u)

actual fun writeLog(message: String) {
    if(debug) {
        println(message)
    } else {
        //GetSystemTime(time.ptr)
        //val formattedTime =
        //    "[${time.wMonth}-${time.wDay}-${time.wYear} ${time.wHour}:${time.wMinute}:${time.wSecond}] $message\n"
        fprintf(stdout, "$message\n")
    }
}
actual fun showErrorPopup(title: String, message: String) {
    if(debug) {
        println("$title - $message")
    } else {
        //GetSystemTime(time.ptr)
        //val formattedTime =
        //    "[${time.wMonth}-${time.wDay}-${time.wYear} ${time.wHour}:${time.wMinute}:${time.wSecond}] $title - $message\n"
        fprintf(stderr, "$message\n")
    }
    tinyfd_messageBox(title, message, "ok", "error", 1)
}

class LinuxContainer: NovaWindowSDL() {
    private var mappingsGenerated = true
    private var display: CPointer<Display>? = null

    override fun init(): Boolean {
        if(!debug) {
            logFile = freopen("logfile.txt", "a", stdout)
            dup2(1, 2)
        }

        display = XOpenDisplay(null)
        if(super.init()) {
            val act = MemScope().alloc<sigaction>()
            sigemptyset(act.sa_mask.ptr)
            act.sa_flags = 0
            act.__sigaction_handler.sa_handler = signalHandler
            sigaction(SIGCHLD, act.ptr, null)
            return true
        }
        return false
    }

    override fun processEvents(project: NovaProject) {
        super.processEvents(project)

        if(childTerminated) {
            project.dosBoxFinished()
            childTerminated = false
        } /*else if (baseAddress == 0uL) {
            pid?.let {
                writeLog("Get base address")
                val entryPointer = getBaseAddressLin(it)
                writeLog("Entry pointer at $entryPointer")
                baseAddress = entryPointer + 32uL
            }
        }*/

        if(!mappingsGenerated) {
            createJoyToKeyMappings(project.novaConf)
            mappingsGenerated = true
        }
    }

    private val pidPtr = MemScope().alloc<pid_tVar>()

    override fun executeCommand(executable: String, args: List<String>) {
        println("Time to start")
        val myargs = mutableListOf(executable)
        myargs.addAll(args)
        myargs.removeAt(1)
        baseAddress = popenGetBaseAddress(myargs.joinToString(" "), "r") + 0x40000L
        println("Base Address $baseAddress")
        sleep(2)
        println(readMemoryInt(0))
        //popen(myargs.joinToString(" "), "r")
        val actualpid = getProcessOutput("pgrep dosbox")
        println("Got PID $actualpid")
        pid = actualpid.trim().toInt()
        mappingsGenerated = false
//        ar = Arena().apply {
//            val argc = args.toCStringArray(this)
//            //val argp = argc.
//
//            //val argp = StableRef.create(argc)
//            //sleep(1)
//            val result = posix_spawn(pidPtr.ptr, executable, null, null, argc, __environ)
//            if (result == 0) {
//                println(pidPtr.value)
//                pid = pidPtr.value
//            } else {
//                println("Error $result")
//            }
//        }
        val p2pilotaddr = readMemoryInt(p2PilotPointer)
        p2struct.ptrAddr = p2pilotaddr.toLong()
        println("I think p2 ptr is at $p2pilotaddr")
        println("I think p2 pilot name is at ${p2struct.name}")
    }

    private fun popenGetBaseAddress(command: String, modes: String): Long = memScoped {
        var result = ""
        val buffer = allocArray<ByteVar>(128)
        val pipe = popen("$command 2>&1", modes)
        pipe?.let {
            while (feof(pipe) == 0) {
                if (fgets(buffer, 128, pipe) != null) {
                    result = buffer.getPointer(this).toKString()
                    if (result.contains("Base address:")) {
                        break
                    }
                }
            }
        }
        val hex = result.substring(result.indexOf("0x")+2).trim()
        return hex.toLong(16)
    }

    //fun findDosbox(): Boolean {
    //    var result = validateDosbox("dosbox")
    //    if(result != null) {
    //        return true
    //    }
    //    var result =

    //    return false
    //}

    fun validateDosbox(dosboxCommand: String): String? {
        val result = getProcessOutput("$dosboxCommand -version | grep version")
        return if(!result.contains("command not found")) {
            result
        } else {
            null
        }
    }
    fun getProcessOutput(command: String): String = memScoped {
        var result = ""
        val buffer = allocArray<ByteVar>(128)
        val pipe = popen(command, "r")
        try {
            while (fgets(buffer, 128, pipe) != null) {
                result += buffer.getPointer(MemScope()).toKString()
            }
        } catch (e: Exception) {
            pclose(pipe)
            throw e
        }
        pclose(pipe)
        return result
    }

    override fun showFolderChooser(start: String, prompt: String): String {
        val selectedDirectory: CPointer<ByteVar>? =
            tinyfd_selectFolderDialog(
                aTitle = prompt,
                aDefaultPath = start)
        return selectedDirectory?.toKStringFromUtf8()?:""
    }

    override fun showFileChooser(start: String, prompt: String, filter: String, filterDesc: String): String {
        val filters = MemScope().allocArrayOf(filter.cstr.getPointer(MemScope()))
        val selectedFile: CPointer<ByteVar>? =
            tinyfd_openFileDialog(
                aTitle = prompt,
                aDefaultPathAndFile = start,
                aAllowMultipleSelects = 0,
                aFilterPatterns = filters,
                aNumOfFilterPatterns = 1,
                aSingleFilterDescription = filterDesc
            )
        return selectedFile?.toKStringFromUtf8()?:""
    }

    override fun showTextInput(title: String, prompt: String): String {
        val input =
            tinyfd_inputBox(
                aTitle = title,
                aMessage = prompt,
                aDefaultInput = "")
        return input?.toKStringFromUtf8()?:""
    }

    override fun destroy() {
        if(!debug) {
            fclose(logFile)
        }

        XCloseDisplay(display)
    }

//    var inputToKeyMapping = emptyMap<ButtonMap, Int>()
//    private fun createJoyToKeyMappings(conf: NovaConf) {
//        val boundInputs = conf.getBoundInputs()
//        val usedScancodes = boundInputs.filter { it.type == ControlType.KEY }.map { it.scancode }
//        val possibleMappings = sdlKeyMapping.keys.filterNot { key ->
//            usedScancodes.contains(key)
//        }
//        val nonKeyMappings = boundInputs.filterNot { it.type == ControlType.KEY }
//        inputToKeyMapping = nonKeyMappings.zip(possibleMappings).toMap()
//    }

//    override fun sendKeyEvent(mappedButton: ButtonMap, up: Boolean, useDummy: Boolean, recorded: Boolean) {
//        if(mappedButton.type == ControlType.KEY) return
//
//        val desiredVirtInput = inputToKeyMapping[mappedButton] ?: return
//
//        val tempCode = XKeysymToKeycode(display, sdlKeyMapping[desiredVirtInput]!!)
//        if(tempCode > 0u) {
//            XTestFakeKeyEvent(display, tempCode.toUInt(), if(up) 0 else 1, 0)
//            XFlush(display)
//        }
//    }

    var dummyMapper = emptyMap<ButtonMap, ButtonMap>()
    var inputToKeyMapping = emptyMap<ButtonMap, Int>()
    private fun createJoyToKeyMappings(conf: NovaConf) {
        println("Createjoytokeymappings")
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

    private var tempCode: UByte = 0u
    override fun sendKeyEvent(mappedButton: ButtonMap, up: Boolean, useDummy: Boolean, recorded: Boolean) {
        println("SendKeyEvent $mappedButton")
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
                    tempCode = XKeysymToKeycode(display, sdlKeyMapping[newButton.scancode]!!)
                    //inputBuffer[0].ki.wVk = sdlKeyMapping[newButton.scancode] ?: return

                } else {
                    val desiredVirtInput = inputToKeyMapping[newButton] ?: return
                    tempCode = XKeysymToKeycode(display, sdlKeyMapping[desiredVirtInput]!!)
                    //inputBuffer[0].ki.wVk = sdlKeyMapping[desiredVirtInput] ?: return
                }
                //inputBuffer[0].ki.dwFlags = if (up) KEYEVENTF_KEYUP.toUInt() else 0u
                //SendInput(1, inputBuffer, sizeOf<INPUT>().toInt())

                if(tempCode > 0u) {
                    XTestFakeKeyEvent(display, tempCode.toUInt(), if(up) 0 else 1, 0)
                    XFlush(display)
                }
            }
        } else {
            val desiredVirtInput = inputToKeyMapping[mappedButton] ?: return
            tempCode = XKeysymToKeycode(display, sdlKeyMapping[desiredVirtInput]!!)
            //inputBuffer[0].ki.wVk = sdlKeyMapping[desiredVirtInput] ?: return
            //inputBuffer[0].ki.dwFlags = if (up) KEYEVENTF_KEYUP.toUInt() else 0u
            //SendInput(1, inputBuffer, sizeOf<INPUT>().toInt())

            if(tempCode > 0u) {
                XTestFakeKeyEvent(display, tempCode.toUInt(), if(up) 0 else 1, 0)
                XFlush(display)
            }
        }
    }
}

actual fun readMemoryByte(address: Long): UByte {
    val handle = fopen("/proc/$pid/mem", "rb")
    fseek(handle, baseAddress + address, SEEK_SET)
    fread(bytePointer.ptr, 1, 1, handle)
    fclose(handle)
    return bytePointer.value
}
actual fun readMemoryShort(address: Long): UShort {
    val handle = fopen("/proc/$pid/mem", "rb")
    fseek(handle, baseAddress + address, SEEK_SET)
    fread(shortPointer.ptr, 2, 1, handle)
    fclose(handle)
    return shortPointer.value
}
actual fun readMemoryInt(address: Long): UInt {
    try {
        val handle = open("/proc/$pid/mem", O_RDONLY)
//        if(handle == null) {
//            println("My handle is null!")
//            return 0u
//        }
        //println("Handle open")
        //val jkl = ftell(handle)
        //println(jkl)
        lseek(handle, baseAddress+address, SEEK_SET)
        //fseek(handle, baseAddress + address, SEEK_SET)
        //println("Seek successful")
        //fread(intPointer.ptr, 4, 1, handle)
        read(handle, intPointer.ptr, 4)
        //println("Value read")
        close(handle)
        //fclose(handle)
        //println("Handle closed")
    } catch (e: Exception) {
        println(e.message)
    }
    //println("Seek to $address")
    //println("Return ${intPointer.value}")
    return intPointer.value
}
actual fun writeMemoryInt(address: Long, value: UInt) {}
actual fun writeMemoryShort(address: Long, value: UShort) {}
actual fun writeMemoryByte(address: Long, value: UByte) {}
actual fun writeMemoryString(address: Long, value: String, limit: Int) {

//    var charactersWritten = 0
//    value.utf8.getBytes().take(limit).forEach { byte ->
//        writeMemoryByte(address + charactersWritten, byte.toUByte())
//        charactersWritten++
//    }
//    while(charactersWritten < limit) {
//        writeMemoryByte(address + charactersWritten, 0u)
//        charactersWritten++
//    }
}

//fun writeMemoryValue(offset: Long, input: CPointer<out CPointed>, inputSize: ULong) {
//    if(entryPointer == 0L) return
//    val result = WriteProcessMemory(
//        processInfo?.pointed?.hProcess,
//        interpretCPointer(nativeNullPtr + entryPointer + offset), input, inputSize, null)
//    if (result == 0) {
//        println("Error! ${GetLastError()}")
//    }
//}

val sdlKeyMapping = mapOf(
    VIRT_A to XK_a.toULong(),
    VIRT_B to XK_b.toULong(),
    VIRT_C to XK_c.toULong(),
    VIRT_D to XK_d.toULong(),
    VIRT_E to XK_e.toULong(),
    VIRT_F to XK_f.toULong(),
    VIRT_G to XK_g.toULong(),
    VIRT_H to XK_h.toULong(),
    VIRT_I to XK_i.toULong(),
    VIRT_J to XK_j.toULong(),
    VIRT_K to XK_k.toULong(),
    VIRT_L to XK_l.toULong(),
    VIRT_M to XK_m.toULong(),
    VIRT_N to XK_n.toULong()
)

actual fun getPossibleMappings(): Map<Int, Any> {
    return sdlKeyMapping
}