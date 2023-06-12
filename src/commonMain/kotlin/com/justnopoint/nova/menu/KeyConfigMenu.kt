package com.justnopoint.nova.menu

import com.justnopoint.nova.*

class KeyConfigMenu(project: NovaProject) : NovaMenu(project) {
    private var selectedIndex = 0
    enum class KeyConfigState {
        MENU, CONFIGURE, AWAIT, TRAININGCONFIG, TRAININGAWAIT, CONTROLLERS
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
            KeyConfigState.CONTROLLERS -> {
                project.getControllerList().forEachIndexed { index, s ->
                    if(project.p1Controller == s) {
                        window.showText(s.getName(), MenuFonts.smallFont_yellow,18*scale, (20 + 8 * index) * scale, TextAlignment.LEFT)
                    } else if(project.p2Controller == s) {
                        window.showText(s.getName().reversed(), MenuFonts.smallFont_yellow,306*scale, (20 + 8 * index) * scale, TextAlignment.RIGHT)
                    } else {
                        window.showText(s.getName(), MenuFonts.smallFont_yellow,160*scale, (20 + 8 * index) * scale, TextAlignment.CENTER)
                    }
                }
            }
            KeyConfigState.MENU -> {
                if(selectedIndex < 0) selectedIndex = 3
                if(selectedIndex > 3) selectedIndex = 0
                renderText(window, "Set Controllers", 18*scale, 20*scale, selectedIndex == 0, frame)
                renderText(window, "Player 1 Controls", 18 * scale, 33 * scale, selectedIndex == 1, frame)
                renderText(window, "Player 2 Controls", 18 * scale, 46 * scale, selectedIndex == 2, frame)
                renderText(window, "Training Mode Controls", 18 * scale, 59 * scale, selectedIndex == 3, frame)

                window.showText("Set your in-game controls.", MenuFonts.smallFont_gray, hintCoordX, hintCoordY)
            }
            KeyConfigState.TRAININGAWAIT, KeyConfigState.TRAININGCONFIG -> {
                if(selectedIndex < 0) selectedIndex = 4
                if(selectedIndex > 4) selectedIndex = 0
                renderText(window, "Dummy Record", 18 * scale, 20 * scale, selectedIndex == 0, frame)
                renderText(window, "Dummy Playback", 18 * scale, 33 * scale, selectedIndex == 1, frame)
                renderText(window, "Reset", 18 * scale, 46 * scale, selectedIndex == 2, frame)
                renderText(window, "Reset (Left)", 18 * scale, 59 * scale, selectedIndex == 3, frame)
                renderText(window, "Reset (Right)", 18 * scale, 72 * scale, selectedIndex == 4, frame)

                project.novaConf.trainingConfig.apply {
                    listOf(record, playback, resetCenter, resetLeft, resetRight)
                        .forEachIndexed { index, control ->
                            if(control != null) {
                                val showRed = currentState == KeyConfigState.AWAIT && selectedIndex == index
                                renderText(
                                    window,
                                    "${control.name}",
                                    150 * scale,
                                    (20 + 13 * index) * scale,
                                    selectedIndex == index,
                                    frame,
                                    showRed
                                )
                            }
                        }
                }

                if(currentState == KeyConfigState.TRAININGAWAIT) {
                    window.showText("Press any key, gamepad, or joystick input.", MenuFonts.smallFont_gray, hintCoordX, hintCoordY)
                } else {
                    window.showText("Select to change this input.  The current mapping is shown on the right.", MenuFonts.smallFont_gray, hintCoordX, hintCoordY)
                }
            }
            KeyConfigState.AWAIT, KeyConfigState.CONFIGURE -> {
                if(selectedIndex < 0) selectedIndex = 6
                if(selectedIndex > 6) selectedIndex = 0
                renderText(window, "Up", 18 * scale, 20 * scale, selectedIndex == 0, frame)
                renderText(window, "Down", 18 * scale, 33 * scale, selectedIndex == 1, frame)
                renderText(window, "Left", 18 * scale, 46 * scale, selectedIndex == 2, frame)
                renderText(window, "Right", 18 * scale, 59 * scale, selectedIndex == 3, frame)
                renderText(window, "Punch", 18 * scale, 72 * scale, selectedIndex == 4, frame)
                renderText(window, "Kick", 18 * scale, 85 * scale, selectedIndex == 5, frame)
                renderText(window, "Menu", 18 * scale, 111 * scale, selectedIndex == 6, frame)

                currentConfig.apply {
                    listOf(up, down, left, right, punch, kick).forEachIndexed { index, control ->
                        val showRed = currentState == KeyConfigState.AWAIT && selectedIndex == index
                        renderText(window, "${control.name}", 90 * scale, (20 + 13*index) * scale, selectedIndex == index, frame, showRed)
                    }

                    val showRed = currentState == KeyConfigState.AWAIT && selectedIndex == 6
                    renderText(window, "${esc.name}", 90 * scale, 111 * scale, selectedIndex == 6, frame, showRed)
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
            KeyConfigState.TRAININGAWAIT -> {
                when(configKey) {
                    0 -> project.novaConf.trainingConfig.record = input
                    1 -> project.novaConf.trainingConfig.playback = input
                    2 -> project.novaConf.trainingConfig.resetCenter = input
                    3 -> project.novaConf.trainingConfig.resetLeft = input
                    4 -> project.novaConf.trainingConfig.resetRight = input
                }
                currentState = KeyConfigState.TRAININGCONFIG
            }
            KeyConfigState.AWAIT -> {
                when(configKey) {
                    0 -> currentConfig.up = input
                    1 -> currentConfig.down = input
                    2 -> currentConfig.left = input
                    3 -> currentConfig.right = input
                    4 -> currentConfig.punch = input
                    5 -> currentConfig.kick = input
                    6 -> currentConfig.esc = input
                }
                currentState = KeyConfigState.CONFIGURE
            }
            else -> {
                super.handleInput(input)
            }
        }
    }

    override fun menuInput(menuInput: MenuInput, input: ButtonMap) {
        when(currentState) {
            KeyConfigState.CONFIGURE, KeyConfigState.MENU, KeyConfigState.TRAININGCONFIG -> {
                when(menuInput) {
                    MenuInput.UP -> selectedIndex--
                    MenuInput.DOWN -> selectedIndex++
                    MenuInput.SELECT -> select()
                    MenuInput.CANCEL -> cancel()
                    else -> {}
                }
            }
            KeyConfigState.CONTROLLERS -> {
                val controller = project.getControllerList().find { it.getDeviceId() == input.controlId }

                when(menuInput) {
                    MenuInput.LEFT -> {
                        if(project.p2Controller == controller) {
                            project.p2Controller = null
                        } else if(project.p1Controller == null) {
                            project.p1Controller = controller
                        }
                    }
                    MenuInput.RIGHT -> {
                        if(project.p1Controller == controller) {
                            project.p1Controller = null
                        } else if(project.p2Controller == null) {
                            project.p2Controller = controller
                        }
                    }
                    else -> {}
                }
            }
            else -> { /** Should have been handled already **/ }
        }


    }

    private fun select() {
        when(currentState) {
            KeyConfigState.CONFIGURE -> {
                currentState = KeyConfigState.AWAIT
                configKey = selectedIndex
                val map = ButtonMap(type = ControlType.KEY, scancode = 0, name = "?")
                when (selectedIndex) {
                    0 -> currentConfig.up = map
                    1 -> currentConfig.down = map
                    2 -> currentConfig.left = map
                    3 -> currentConfig.right = map
                    4 -> currentConfig.punch = map
                    5 -> currentConfig.kick = map
                    6 -> currentConfig.esc = map
                }
            }
            KeyConfigState.TRAININGCONFIG -> {
                currentState = KeyConfigState.TRAININGAWAIT
                configKey = selectedIndex
                val map = ButtonMap(type = ControlType.KEY, scancode = 0, name = "?")
                when (selectedIndex) {
                    0 -> project.novaConf.trainingConfig.record = map
                    1 -> project.novaConf.trainingConfig.playback = map
                    2 -> project.novaConf.trainingConfig.resetCenter = map
                    3 -> project.novaConf.trainingConfig.resetLeft = map
                    4 -> project.novaConf.trainingConfig.resetRight = map
                }
            }
            else -> {}
        }
    }

    private fun cancel() {
        when(currentState) {
            KeyConfigState.CONTROLLERS,
            KeyConfigState.TRAININGCONFIG,
            KeyConfigState.CONFIGURE -> {
                currentState = KeyConfigState.MENU
                project.novaConf.checkErrors()
            }
            else -> {}
        }
    }

    fun startConfig(): Boolean {
        return when (selectedIndex) {
            0 -> {
                //currentState = KeyConfigState.CONTROLLERS
                false
            }
            3 -> {
                currentState = KeyConfigState.TRAININGCONFIG
                true
            }
            else -> {
                currentState = KeyConfigState.CONFIGURE
                currentConfig = if (selectedIndex == 1) project.novaConf.p1Config else project.novaConf.p2Config
                true
            }
        }
    }
}