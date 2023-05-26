package com.justnopoint.nova

import kotlinx.cinterop.*
import platform.linux.posix_spawn
import platform.posix.*
import tinyfiledialogs.*
import xtest.*

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
    }
    pid = null
    childTerminated = true
}

actual fun writeLog(message: String) {
    TODO()
}
actual fun showErrorPopup(title: String, message: String) {
    TODO()
}

class LinuxContainer: NovaWindowSDL() {
    private var mappingsGenerated = true
    private var display: CPointer<Display>? = null

    override fun init(): Boolean {
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
        }

        if(!mappingsGenerated) {
            createJoyToKeyMappings(project.novaConf)
            mappingsGenerated = true
        }
    }

    private val pidPtr = MemScope().alloc<pid_tVar>()

    override fun executeCommand(executable: String, args: List<String>) {
        val myargs = mutableListOf(executable)
        myargs.addAll(args)
        myargs.removeAt(1)
        popen(myargs.joinToString(" "), "r")
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
                result += buffer
            }
        } catch (e: Exception) {
            pclose(pipe)
            throw e
        }
        pclose(pipe)
        return result
    }

    override fun showFileChooser(start: String, prompt: String, filter: String, filterDesc: String): String {
        TODO("Not yet implemented")
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

    override fun destroy() {
        XCloseDisplay(display)
    }

    override fun enableTraining() {

    }

    override fun sendKeyEvent(mappedButton: ButtonMap, up: Boolean, useDummy: Boolean, recorded: Boolean) {
        TODO("Not yet implemented")
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

    override fun sendKeyEvent(mappedButton: ButtonMap, up: Boolean) {
        if(mappedButton.type == ControlType.KEY) return

        val desiredVirtInput = inputToKeyMapping[mappedButton] ?: return

        val tempCode = XKeysymToKeycode(display, sdlKeyMapping[desiredVirtInput]!!)
        if(tempCode > 0u) {
            XTestFakeKeyEvent(display, tempCode.toUInt(), if(up) 0 else 1, 0)
            XFlush(display)
        }
    }
}

actual fun readMemoryShort(address: Long): UShort {
    TODO()
}
actual fun readMemoryInt(address: Long): UInt {
    TODO()
}
actual fun writeMemoryInt(address: Long, value: UInt) {}
actual fun writeMemoryShort(address: Long, value: UShort) {}
actual fun writeMemoryByte(address: Long, value: UByte) {}

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
    VIRT_L to XK_l.toULong()
)

actual fun getPossibleMappings(): Map<Int, Any> {
    return sdlKeyMapping
}