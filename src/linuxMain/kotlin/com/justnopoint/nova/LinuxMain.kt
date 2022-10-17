package com.justnopoint.nova

actual fun getNativeWindow(): NovaWindow? {
    val container = LinuxContainer()
    return if(container.init()) {
        container
    } else {
        null
    }
}

class LinuxContainer: NovaWindowSDL() {
    override fun executeCommand(executable: String, command: String) {
        TODO("Not yet implemented")
    }

    override fun showFileChooser(start: String, prompt: String): String {
        TODO("Not yet implemented")
    }

    override fun showFolderChooser(start: String, prompt: String): String {
        TODO("Not yet implemented")
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