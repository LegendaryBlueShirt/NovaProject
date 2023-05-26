package com.justnopoint.nova

import okio.*

class SoundCard {
    companion object {
        const val FILENAME = "SOUNDCRD.INF"

        fun isGravisEnabled(omfPath: Path): Boolean {
            val soundcardFile = omfPath.div(FILENAME)
            if(!FileSystem.SYSTEM.exists(soundcardFile))
                return true
            val string = FileSystem.SYSTEM.read(soundcardFile) {
                skip(8)
                val stringbytes = readByteArray(12)
                stringbytes.decodeToString()
            }
            return string == "MASIULTRASND"
        }
    }
}