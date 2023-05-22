package com.justnopoint.nova

import com.justnopoint.nova.simplemenu.ConfiguredFont

@ThreadLocal
object MenuFonts {
    private const val transparent: Byte = 0x0

    var bigFont_shadow = 0
    var bigFont_normal = 0
    var bigFont_highlight1 = 0
    var bigFont_highlight2 = 0
    var bigFont_blue = 0
    var bigFont_red = 0
    var bigFont_red_lighter = 0

    var smallFont_green = 0
    var smallFont_aqua = 0
    var smallFont_red = 0
    var smallFont_sand = 0
    var smallFont_yellow = 0
    var smallFont_gray = 0

    lateinit var menuFontDefault: ConfiguredFont
    lateinit var menuFontDisabled: ConfiguredFont
    lateinit var menuFontYellow: ConfiguredFont
    lateinit var menuFontGray: ConfiguredFont
    lateinit var menuFontSand: ConfiguredFont

    @OptIn(ExperimentalUnsignedTypes::class)
    fun initialize(window: NovaWindow, font1image: PCXImage, font2image: PCXImage) {
        val font1mapping = OmfFont("NETFONT1.txt")
        val font2mapping = OmfFont("NETFONT2.txt")

        val shadowColor = 0x9
        val normalColor = 0xA
        val highlight1Color = 0xB
        val highlight2Color = 0xC
        val blueColor = 0xD
        val redColor = 0xE
        val redLighterColor = 0xF
        copyPalette(font1image.paletteData, listOf(shadowColor), listOf(0x7))
        var font1tex = window.loadTexture(font1image)
        bigFont_shadow = window.loadFont(font1mapping, font1tex)
        copyPalette(font1image.paletteData, listOf(normalColor), listOf(0x7))
        font1tex = window.loadTexture(font1image)
        bigFont_normal = window.loadFont(font1mapping, font1tex)
        copyPalette(font1image.paletteData, listOf(highlight1Color), listOf(0x7))
        font1tex = window.loadTexture(font1image)
        bigFont_highlight1 = window.loadFont(font1mapping, font1tex)
        copyPalette(font1image.paletteData, listOf(highlight2Color), listOf(0x7))
        font1tex = window.loadTexture(font1image)
        bigFont_highlight2 = window.loadFont(font1mapping, font1tex)
        copyPalette(font1image.paletteData, listOf(blueColor), listOf(0x7))
        font1tex = window.loadTexture(font1image)
        bigFont_blue = window.loadFont(font1mapping, font1tex)
        copyPalette(font1image.paletteData, listOf(redColor), listOf(0x7))
        font1tex = window.loadTexture(font1image)
        bigFont_red = window.loadFont(font1mapping, font1tex)
        copyPalette(font1image.paletteData, listOf(redLighterColor), listOf(0x7))
        font1tex = window.loadTexture(font1image)
        bigFont_red_lighter = window.loadFont(font1mapping, font1tex)

        copyPalette(font2image.paletteData, (0x11 .. 0x17).toList(), (0x1 .. 0x7).toList())
        var font2tex = window.loadTexture(font2image)
        smallFont_green = window.loadFont(font2mapping, font2tex)
        copyPalette(font2image.paletteData, (0x19 .. 0x1F).toList(), (0x1 .. 0x7).toList())
        font2tex = window.loadTexture(font2image)
        smallFont_aqua = window.loadFont(font2mapping, font2tex)
        copyPalette(font2image.paletteData, (0x21 .. 0x27).toList(), (0x1 .. 0x7).toList())
        font2tex = window.loadTexture(font2image)
        smallFont_red = window.loadFont(font2mapping, font2tex)
        copyPalette(font2image.paletteData, (0x29 .. 0x2F).toList(), (0x1 .. 0x7).toList())
        font2tex = window.loadTexture(font2image)
        smallFont_sand = window.loadFont(font2mapping, font2tex)
        copyPalette(font2image.paletteData, (0x31 .. 0x37).toList(), (0x1 .. 0x7).toList())
        font2tex = window.loadTexture(font2image)
        smallFont_yellow = window.loadFont(font2mapping, font2tex)
        copyPalette(font2image.paletteData, (0x39 .. 0x3F).toList(), (0x1 .. 0x7).toList())
        font2tex = window.loadTexture(font2image)
        smallFont_gray = window.loadFont(font2mapping, font2tex)

        compileFonts()
    }

    private fun compileFonts() {
        menuFontDefault = ConfiguredFont(
            lineHeight = 13,
            unselectedFont = bigFont_normal,
            selectedFont = bigFont_highlight1,
            selectedFontHighlight = bigFont_highlight2,
            shadowFont = bigFont_shadow
        )
        menuFontDisabled = ConfiguredFont(
            lineHeight = 13,
            unselectedFont = bigFont_red,
            selectedFontHighlight = bigFont_red_lighter,
            shadowFont = bigFont_shadow
        )
        menuFontGray = ConfiguredFont(
            lineHeight = 7,
            unselectedFont = smallFont_gray
        )
        menuFontYellow = ConfiguredFont(
            lineHeight = 7,
            unselectedFont = smallFont_yellow
        )
        menuFontSand = ConfiguredFont(
            lineHeight = 7,
            unselectedFont = smallFont_sand
        )
    }

    private fun remapColors(data: ByteArray, fromColor: List<Int>, toColor: List<Int>) {
        data.forEachIndexed { index, value ->
            if(value == transparent) return@forEachIndexed
            val relIndex = fromColor.indexOf(value.toInt())
            if(relIndex != -1) {
                data[index] = toColor[relIndex].toUByte().toByte()
            }
        }
    }

    @OptIn(ExperimentalUnsignedTypes::class)
    private fun copyPalette(data: UByteArray, fromIndices: List<Int>, toIndices: List<Int>) {
        fromIndices.zip(toIndices).forEach { (from, to) ->
            data[to*3] = data[from*3]
            data[to*3+1] = data[from*3+1]
            data[to*3+2] = data[from*3+2]
        }
    }
}