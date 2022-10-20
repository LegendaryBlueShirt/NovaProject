package com.justnopoint.nova

import okio.FileSystem
import okio.Path.Companion.toPath

class OmfFont(fontMap: String) {
    data class Glyph(val x:Int,val y:Int,val w:Int,val h:Int)

    private val glyphMap = mutableMapOf<Char, Glyph>()

    init {
        FileSystem.SYSTEM.read(file=fontMap.toPath()) {
            val start = readUtf8Line()?.split(":")?:return@read
            if(start.size < 4) return@read
            val fontHeight = start[0].toInt()
            start.subList(1,start.size).onEachIndexed { index, charCount ->
                val yIndex = 1 + (fontHeight+1)*index
                var xIndex = 0
                (0 until charCount.toInt()).forEach { _ ->
                    readUtf8Line()?.let {
                        if(it.length < 2) return@let
                        val charWidth = it[1].digitToInt()
                        glyphMap[it[0]] = Glyph(
                            x = ++xIndex,
                            y = yIndex,
                            w = charWidth,
                            h = fontHeight)
                        xIndex += charWidth
                    }
                }
            }
        }
    }

    fun getGlyph(character: Char): Glyph {
        if(!glyphMap.containsKey(character)) {
            return glyphMap[' ']!!
        }
        return glyphMap[character]!!
    }


}

abstract class OmfFontRenderer {
    abstract val font: OmfFont
    abstract val scale: Float

    fun drawText(message: String, x: Int, y: Int, reverse: Boolean) {
        var xPosition = x
        var yPosition = y
        message.forEach { c ->
            val glyph = font.getGlyph(c)
            if(c == '\n') {
                xPosition = x
                yPosition += ((glyph.h + 1) * scale).toInt()
            }
            if(reverse) {
                xPosition -= (glyph.w * scale).toInt()
                drawCharacter(glyph, c, xPosition, yPosition, scale)
            } else {
                drawCharacter(glyph, c, xPosition, yPosition, scale)
                xPosition += (glyph.w * scale).toInt()
            }
        }
    }

    abstract fun drawCharacter(glyph: OmfFont.Glyph, character: Char, x: Int, y: Int, scale: Float = 1.0f)
}