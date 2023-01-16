package com.justnopoint.nova.menu

import com.justnopoint.nova.*

abstract class NovaMenu(val project: NovaProject) {
    val scale = 2
    val hintCoordX = 14 * scale
    val hintCoordY = 151 * scale

    abstract fun reset()
    abstract fun render(window: NovaWindow, frame: Int)

    protected fun renderText(window: NovaWindow, text: String, x: Int, y: Int, selected: Boolean, frame: Int, red: Boolean = false) {
        window.showText(text, MenuFonts.bigFont_shadow, x, y + scale)
        if (!selected) {
            window.showText(text, if(red) MenuFonts.bigFont_red else MenuFonts.bigFont_normal, x, y)
        } else {
            if (frame % 120 < 60) {
                window.showText(text, if (red) MenuFonts.bigFont_red else MenuFonts.bigFont_highlight1, x, y)
            } else {
                window.showText(text, if (red) MenuFonts.bigFont_red_lighter else MenuFonts.bigFont_highlight2, x, y)
            }
        }
    }

    protected fun renderReversed(window: NovaWindow, text: String, x: Int, y: Int, selected: Boolean, frame: Int, red: Boolean = false) {
        window.showText(text.reversed(), MenuFonts.bigFont_shadow, x, y + scale, true)
        if (!selected) {
            window.showText(text.reversed(), if(red) MenuFonts.bigFont_red else MenuFonts.bigFont_normal, x, y, true)
        } else {
            if (frame % 120 < 60) {
                window.showText(text.reversed(), if (red) MenuFonts.bigFont_red else MenuFonts.bigFont_highlight1, x, y, true)
            } else {
                window.showText(text.reversed(), if (red) MenuFonts.bigFont_red_lighter else MenuFonts.bigFont_highlight2, x, y, true)
            }
        }
    }

    open fun handleInput(input: ButtonMap) {
        when(input.type) {
            ControlType.KEY -> {
                when(input.scancode) {
                    VIRT_LEFT -> left()
                    VIRT_RIGHT -> right()
                    VIRT_UP -> up()
                    VIRT_DOWN -> down()
                    VIRT_RETURN, VIRT_KP_ENTER -> select()
                    VIRT_ESC -> cancel()
                }
            }
            ControlType.AXIS -> {
                if(input.axisId == 0) {
                    if(input.direction == -1) {
                        left()
                    } else {
                        right()
                    }
                } else if(input.axisId == 1) {
                    if(input.direction == -1) {
                        up()
                    } else {
                        down()
                    }
                }
            }
            ControlType.BUTTON -> {
                if(input.axisId == 0) {
                    select()
                } else {
                    cancel()
                }
            }
            ControlType.HAT -> {
                when(input.direction) {
                    1 -> up()
                    4 -> down()
                    8 -> left()
                    2 -> right()
                }
            }
        }
    }

    open fun left() {}
    open fun right() {}
    open fun up() {}
    open fun down() {}
    open fun select() {}
    open fun cancel() {}
}