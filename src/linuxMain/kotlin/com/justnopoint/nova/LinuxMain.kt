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