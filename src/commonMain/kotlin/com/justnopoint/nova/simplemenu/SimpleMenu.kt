package com.justnopoint.nova.simplemenu

import com.justnopoint.nova.*
import com.justnopoint.nova.menu.MenuInput

open class SimpleMenu(private val self: MenuItem): MenuItem by self {
    var orientation = Orientation.VERTICAL
    var focused = false
    var selected = 0
    var items: MutableList<MenuItem> = mutableListOf()
    var topEdge = 0
    var leftEdge = 0
    var rightEdge = 0
    var hintCoord = 0 to 0

    enum class Orientation {
        HORIZONTAL, VERTICAL
    }

    override fun render(window: NovaWindow, selected: Boolean, frame: Int, y: Int) {
        if(focused) {
            var currentY = topEdge
            items.forEachIndexed { index, menuItem ->
                menuItem.render(window, index == this.selected, frame, currentY)
                currentY += menuItem.getHeight()
            }
        } else {
            self.render(window, selected, frame, y)
        }
    }

    fun addItem(item: MenuItem) {
        items.add(item)
        item.setParent(this)
    }

    fun handleInput(input: ButtonMap) {
        when (input.type) {
            ControlType.KEY -> {
                when (input.scancode) {
                    VIRT_LEFT -> menuInput(MenuInput.LEFT, input)
                    VIRT_RIGHT -> menuInput(MenuInput.RIGHT, input)
                    VIRT_UP -> menuInput(MenuInput.UP, input)
                    VIRT_DOWN -> menuInput(MenuInput.DOWN, input)
                    VIRT_RETURN, VIRT_KP_ENTER -> menuInput(MenuInput.SELECT, input)
                    VIRT_ESC -> menuInput(MenuInput.CANCEL, input)
                }
            }

            ControlType.AXIS -> {
                if (input.axisId == 0) {
                    if (input.direction == -1) {
                        menuInput(MenuInput.LEFT, input)
                    } else {
                        menuInput(MenuInput.RIGHT, input)
                    }
                } else if (input.axisId == 1) {
                    if (input.direction == -1) {
                        menuInput(MenuInput.UP, input)
                    } else {
                        menuInput(MenuInput.DOWN, input)
                    }
                }
            }

            ControlType.BUTTON -> {
                if (input.direction == 0) {
                    if (input.axisId == 0) {
                        menuInput(MenuInput.SELECT, input)
                    } else {
                        menuInput(MenuInput.CANCEL, input)
                    }
                } else {
                    when (input.direction) {
                        1 -> menuInput(MenuInput.UP, input)
                        4 -> menuInput(MenuInput.DOWN, input)
                        8 -> menuInput(MenuInput.LEFT, input)
                        2 -> menuInput(MenuInput.RIGHT, input)
                    }
                }
            }

            ControlType.HAT -> {
                when (input.direction) {
                    1 -> menuInput(MenuInput.UP, input)
                    4 -> menuInput(MenuInput.DOWN, input)
                    8 -> menuInput(MenuInput.LEFT, input)
                    2 -> menuInput(MenuInput.RIGHT, input)
                }
            }
        }
    }

    open fun menuInput(menuInput: MenuInput, input: ButtonMap): Boolean {
        items.filterIsInstance<SimpleMenu>().forEach {
            if(it.focused) {
                if(it.menuInput(menuInput, input)) {
                    return true
                }
            }
        }

         when(menuInput) {
            MenuInput.UP -> {
                if(orientation == Orientation.VERTICAL) {
                    selectPrev()
                    return true
                }
            }
            MenuInput.DOWN -> {
                if(orientation == Orientation.VERTICAL) {
                    selectNext()
                    return true
                }
            }
            MenuInput.LEFT -> {
                if(orientation == Orientation.HORIZONTAL) {
                    selectPrev()
                    return true
                }
            }
            MenuInput.RIGHT -> {
                if(orientation == Orientation.HORIZONTAL) {
                    selectNext()
                    return true
                }
            }
            MenuInput.SELECT -> {
                items[selected].apply {
                    if(this is SimpleMenu) {
                        focused = true
                    }
                    onSelect()
                }
                return true
            }
            MenuInput.CANCEL -> {}
        }
        return false
    }

    private fun selectNext() {
        selected++
        if(selected >= items.size) {
            selected = 0
        }
    }

    private fun selectPrev() {
        selected--
        if(selected < 0) {
            selected = items.size-1
        }
    }

    open fun reset() {
        selected = 0
        focused = false
    }
}