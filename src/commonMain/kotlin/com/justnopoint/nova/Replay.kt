package com.justnopoint.nova

import okio.FileHandle
import okio.Path.Companion.toPath
import okio.buffer
import okio.use
import kotlin.experimental.xor

class Replay {
    constructor(handle: FileHandle) {
        handle.source().buffer().use {
            val data = it.readByteArray(428)
            for(n in 0 until 428) {
                val value = ((428+n) and 0xFF).toByte()
                data[n] = data[n] xor value
            }
            getFileSystem().openReadWrite("test.bin".toPath(), mustCreate = false, mustExist = false).use {
                it.sink().buffer().use {
                    it.write(data)
                }
            }
        }
    }
}