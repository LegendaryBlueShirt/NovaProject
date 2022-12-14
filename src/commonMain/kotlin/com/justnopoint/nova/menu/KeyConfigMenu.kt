package com.justnopoint.nova.menu

import com.justnopoint.nova.*

class KeyConfigMenu(project: NovaProject) : NovaMenu(project) {
    private var selectedIndex = 0
    enum class KeyConfigState {
        MENU, CONFIGURE, AWAIT
    }
    var currentState = KeyConfigState.MENU
    private lateinit var currentConfig: ControlMapping

    private var configKey = 0

    override fun reset() {
        currentState = KeyConfigState.MENU
        selectedIndex = 0
    }

    override fun render(window: NovaWindow, frame: Int) {
        when(currentState) {
            KeyConfigState.MENU -> {
                if(selectedIndex < 0) selectedIndex = 1
                if(selectedIndex > 1) selectedIndex = 0
                renderText(window, "Player 1 Controls", 18 * scale, 20 * scale, selectedIndex == 0, frame)
                renderText(window, "Player 2 Controls", 18 * scale, 33 * scale, selectedIndex == 1, frame)

                window.showText("Set your in-game controls.", MenuFonts.smallFont_gray, hintCoordX, hintCoordY)
            }
            KeyConfigState.AWAIT, KeyConfigState.CONFIGURE -> {
                if(selectedIndex < 0) selectedIndex = 5
                if(selectedIndex > 5) selectedIndex = 0
                renderText(window, "Up", 18 * scale, 20 * scale, selectedIndex == 0, frame)
                renderText(window, "Down", 18 * scale, 33 * scale, selectedIndex == 1, frame)
                renderText(window, "Left", 18 * scale, 46 * scale, selectedIndex == 2, frame)
                renderText(window, "Right", 18 * scale, 59 * scale, selectedIndex == 3, frame)
                renderText(window, "Punch", 18 * scale, 72 * scale, selectedIndex == 4, frame)
                renderText(window, "Kick", 18 * scale, 85 * scale, selectedIndex == 5, frame)

                currentConfig.apply {
                    listOf(up, down, left, right, punch, kick).forEachIndexed { index, control ->
                        val showRed = currentState == KeyConfigState.AWAIT && selectedIndex == index
                        renderText(window, "${control.name}", 90 * scale, (20 + 13*index) * scale, selectedIndex == index, frame, showRed)
                    }
                }

                if(currentState == KeyConfigState.AWAIT) {
                    window.showText("Press any key, gamepad, or joystick input.", MenuFonts.smallFont_gray, hintCoordX, hintCoordY)
                } else {
                    window.showText("Select to change this input.  The current mapping is shown on the right.", MenuFonts.smallFont_gray, hintCoordX, hintCoordY)
                }
            }
        }
    }

    override fun handleInput(input: ButtonMap) {
        when(currentState) {
            KeyConfigState.CONFIGURE, KeyConfigState.MENU -> {
                super.handleInput(input)
            }
            KeyConfigState.AWAIT -> {
                when(configKey) {
                    0 -> currentConfig.up = input
                    1 -> currentConfig.down = input
                    2 -> currentConfig.left = input
                    3 -> currentConfig.right = input
                    4 -> currentConfig.punch = input
                    5 -> currentConfig.kick = input
                }
                currentState = KeyConfigState.CONFIGURE
            }
        }
    }

    override fun up() {
        selectedIndex--
    }

    override fun down() {
        selectedIndex++
    }

    override fun select() {
        if(currentState == KeyConfigState.CONFIGURE) {
            currentState = KeyConfigState.AWAIT
            configKey = selectedIndex
            val map = ButtonMap(type = ControlType.KEY, scancode = 0, name = "?")
            when(selectedIndex) {
                0 -> currentConfig.up = map
                1 -> currentConfig.down = map
                2 -> currentConfig.left = map
                3 -> currentConfig.right = map
                4 -> currentConfig.punch = map
                5 -> currentConfig.kick = map
            }
        }
    }

    override fun cancel() {
        if(currentState == KeyConfigState.CONFIGURE) {
            currentState = KeyConfigState.MENU
            project.novaConf.checkErrors()
        }
    }

    fun startConfig() {
        currentState = KeyConfigState.CONFIGURE
        currentConfig = if(selectedIndex == 0) project.novaConf.p1Config else project.novaConf.p2Config
    }
}