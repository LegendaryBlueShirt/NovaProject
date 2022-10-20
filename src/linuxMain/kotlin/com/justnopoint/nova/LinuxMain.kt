package com.justnopoint.nova

import kotlinx.cinterop.*
import platform.linux.posix_spawn
import platform.posix.*
import tinyfiledialogs.*

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

class LinuxContainer: NovaWindowSDL() {
    private var ar: Arena? = null

    override fun init(): Boolean {
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
            ar = null
            project.dosBoxFinished()
            childTerminated = false
        }
    }

    private val pidPtr = MemScope().alloc<pid_tVar>()

    override fun executeCommand(executable: String, args: List<String>) {
        val myargs = mutableListOf(executable)
        myargs.addAll(args)
        myargs.removeAt(1)
        popen(myargs.joinToString(" "), "r")
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

    override fun showFolderChooser(start: String, prompt: String): String {
        val selectedDirectory: CPointer<ByteVar>? =
            tinyfd_selectFolderDialog(
                aTitle = prompt,
                aDefaultPath = start)
        return selectedDirectory?.toKStringFromUtf8()?:""
    }

    override fun showFileChooser(start: String, prompt: String): String {
        val filters = MemScope().allocArrayOf("*.exe".cstr.getPointer(MemScope()))
        val selectedFile: CPointer<ByteVar>? =
            tinyfd_openFileDialog(
                aTitle = prompt,
                aDefaultPathAndFile = start,
                aAllowMultipleSelects = 0,
                aFilterPatterns = null,
                aNumOfFilterPatterns = 0,
                aSingleFilterDescription = "dosbox executable"
            )
        return selectedFile?.toKStringFromUtf8()?:""
    }

    override fun destroy() {

    }

    override fun enableTraining() {
        TODO("Not yet implemented")
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