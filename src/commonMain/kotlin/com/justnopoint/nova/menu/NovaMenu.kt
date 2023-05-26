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

    protected fun renderCentered(window: NovaWindow, text: String, x: Int, y: Int, selected: Boolean, frame: Int, red: Boolean = false) {
        window.showText(text, MenuFonts.bigFont_shadow, x, y + scale, TextAlignment.CENTER)
        if (!selected) {
            window.showText(text, if(red) MenuFonts.bigFont_red else MenuFonts.bigFont_normal, x, y, TextAlignment.CENTER)
        } else {
            if (frame % 120 < 60) {
                window.showText(text, if (red) MenuFonts.bigFont_red else MenuFonts.bigFont_highlight1, x, y, TextAlignment.CENTER)
            } else {
                window.showText(text, if (red) MenuFonts.bigFont_red_lighter else MenuFonts.bigFont_highlight2, x, y, TextAlignment.CENTER)
            }
        }
    }

    protected fun renderReversed(window: NovaWindow, text: String, x: Int, y: Int, selected: Boolean, frame: Int, red: Boolean = false) {
        window.showText(text.reversed(), MenuFonts.bigFont_shadow, x, y + scale, TextAlignment.RIGHT)
        if (!selected) {
            window.showText(text.reversed(), if(red) MenuFonts.bigFont_red else MenuFonts.bigFont_normal, x, y, TextAlignment.RIGHT)
        } else {
            if (frame % 120 < 60) {
                window.showText(text.reversed(), if (red) MenuFonts.bigFont_red else MenuFonts.bigFont_highlight1, x, y, TextAlignment.RIGHT)
            } else {
                window.showText(text.reversed(), if (red) MenuFonts.bigFont_red_lighter else MenuFonts.bigFont_highlight2, x, y, TextAlignment.RIGHT)
            }
        }
    }

    open fun handleInput(input: ButtonMap) {
        when(input.type) {
            ControlType.KEY -> {
                when(input.scancode) {
                    VIRT_LEFT -> menuInput(MenuInput.LEFT, input)
                    VIRT_RIGHT -> menuInput(MenuInput.RIGHT, input)
                    VIRT_UP -> menuInput(MenuInput.UP, input)
                    VIRT_DOWN -> menuInput(MenuInput.DOWN, input)
                    VIRT_RETURN, VIRT_KP_ENTER -> menuInput(MenuInput.SELECT, input)
                    VIRT_ESC -> menuInput(MenuInput.CANCEL, input)
                }
            }
            ControlType.AXIS -> {
                if(input.axisId == 0) {
                    if(input.direction == -1) {
                        menuInput(MenuInput.LEFT, input)
                    } else {
                        menuInput(MenuInput.RIGHT, input)
                    }
                } else if(input.axisId == 1) {
                    if(input.direction == -1) {
                        menuInput(MenuInput.UP, input)
                    } else {
                        menuInput(MenuInput.DOWN, input)
                    }
                }
            }
            ControlType.BUTTON -> {
                if(input.direction == 0) {
                    if(input.axisId == 0) {
                        menuInput(MenuInput.SELECT, input)
                    } else {
                        menuInput(MenuInput.CANCEL, input)
                    }
                } else {
                    when(input.direction) {
                        1 -> menuInput(MenuInput.UP, input)
                        4 -> menuInput(MenuInput.DOWN, input)
                        8 -> menuInput(MenuInput.LEFT, input)
                        2 -> menuInput(MenuInput.RIGHT, input)
                    }
                }
            }
            ControlType.HAT -> {
                when(input.direction) {
                    1 -> menuInput(MenuInput.UP, input)
                    4 -> menuInput(MenuInput.DOWN, input)
                    8 -> menuInput(MenuInput.LEFT, input)
                    2 -> menuInput(MenuInput.RIGHT, input)
                }
            }
        }
    }

    open fun menuInput(menuInput: MenuInput, input: ButtonMap) {}
}

enum class MenuInput {
    UP, DOWN, LEFT, RIGHT, SELECT, CANCEL
}