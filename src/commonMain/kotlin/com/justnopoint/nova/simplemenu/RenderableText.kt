package com.justnopoint.nova.simplemenu

import com.justnopoint.nova.NovaWindow
import com.justnopoint.nova.TextAlignment

class RenderableText(
    var text: String,
    private val font: ConfiguredFont,
    val alignment: TextAlignment = TextAlignment.LEFT
) {
    private val scale = 2
    private val highlightLength = 60

    fun getLineHeight(): Int {
        return font.lineHeight
    }
    fun render(window: NovaWindow, selected: Boolean, frame: Int, x: Int, y: Int) {
        font.shadowFont?.let {
            window.showText(text, it, x, y + scale, alignment)
        }
        if (!selected) {
            window.showText(text, font.unselectedFont, x, y, alignment)
        } else {
            if (frame % (highlightLength*2) < highlightLength) {
                window.showText(text, font.getSelectedFont(), x, y, alignment)
            } else {
                window.showText(text, font.getSelectedFontHighlight(), x, y, alignment)
            }
        }
    }
}

data class ConfiguredFont(
    val lineHeight: Int,
    val unselectedFont: Int,
    val shadowFont: Int? = null,
    val selectedFont: Int? = null,
    val selectedFontHighlight: Int? = null
)

fun ConfiguredFont.getSelectedFont(): Int {
    return selectedFont?:unselectedFont
}

fun ConfiguredFont.getSelectedFontHighlight(): Int {
    return selectedFontHighlight?:selectedFont?:unselectedFont
}