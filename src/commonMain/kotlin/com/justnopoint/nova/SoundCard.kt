package com.justnopoint.nova

import okio.*

class SoundCard {
    companion object {
        private const val FILENAME = "SOUNDCRD.INF"

        fun isGravisEnabled(omfPath: Path): Boolean {
            val soundcardFile = omfPath.div(FILENAME)
            val string = getFileSystem().openReadOnly(soundcardFile).use {
                it.source().buffer().use { buffer ->
                    buffer.skip(8)
                    val stringbytes = buffer.readByteArray(12)
                    stringbytes.decodeToString()
                }
            }
            return string == "MASIULTRASND"
        }
    }
}