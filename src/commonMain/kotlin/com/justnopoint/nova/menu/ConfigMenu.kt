package com.justnopoint.nova.menu

import com.justnopoint.nova.*

class ConfigMenu(project: NovaProject) : NovaMenu(project) {
    private var selectedIndex = 0
    enum class State {
        MENU, NOVACONF, OMFCONF, DOSBOXCONF
    }
    var currentState = State.MENU

    override fun menuInput(menuInput: MenuInput, input: ButtonMap) {
        when(menuInput) {
            MenuInput.UP -> up()
            MenuInput.DOWN -> down()
            MenuInput.SELECT -> select()
            MenuInput.CANCEL -> cancel()
            else -> {}
        }
    }

    fun up() {
        selectedIndex--
    }

    fun down() {
        selectedIndex++
    }

    override fun reset() {
        selectedIndex = 0
    }

    private fun canConfigOmf(): Boolean {
        return project.omfConfig != null
    }

    private fun canRunSetup(): Boolean {
        val missingFiles = project.novaConf.validateOmfSetup()
        return !missingFiles.contains(OMFConf.SETUP)
    }

    private fun canRunDosbox(): Boolean {
        return project.novaConf.validateDosbox() == null
    }

    override fun render(window: NovaWindow, frame: Int) {
        when(currentState) {
            State.MENU -> {
                if(selectedIndex < 0) selectedIndex = 2
                if(selectedIndex > 2) selectedIndex = 0

                renderText(window, "Nova Project Settings", 18 * scale, 20 * scale, selectedIndex == 0, frame)
                renderText(window, "OMF2097 Settings", 18 * scale, 33 * scale, selectedIndex == 1, frame, !(canRunDosbox() && canRunSetup()))
                renderText(window, "DOSBox Settings", 18 * scale, 46 * scale, selectedIndex == 2, frame)

                when(selectedIndex) {
                    0 -> window.showText("Change settings for this app.", MenuFonts.smallFont_gray, hintCoordX, hintCoordY)
                    1 -> window.showText("Change settings for the game.", MenuFonts.smallFont_gray, hintCoordX, hintCoordY)
                    2 -> window.showText("Change settings for DOSBox.", MenuFonts.smallFont_gray, hintCoordX, hintCoordY)
                }
            }
            State.NOVACONF -> {
                if(selectedIndex < 0) selectedIndex = 4
                if(selectedIndex > 4) selectedIndex = 0

                renderText(window, "DOSBox location", 18 * scale, 20 * scale, selectedIndex == 0, frame)
                renderText(window, "OMF location", 18 * scale, 46 * scale, selectedIndex == 1, frame)
                renderText(window, "Joystick Support", 18 * scale, 72 * scale, selectedIndex == 2, frame)
                renderText(window, "Save Replays", 18 * scale, 85 * scale, selectedIndex == 3, frame)
                renderText(window, "Attract Mode", 18 * scale, 98 * scale, selectedIndex == 4, frame)

                window.showText(project.novaConf.dosboxPath.reversed(), MenuFonts.smallFont_yellow,302*scale, 33*scale, TextAlignment.RIGHT)
                window.showText(project.novaConf.omfPath.reversed(), MenuFonts.smallFont_yellow,302*scale, 59*scale, TextAlignment.RIGHT)
                renderReversed(window, if(project.novaConf.joyEnabled) "On" else "Off", 302 * scale, 72 * scale, selectedIndex == 2, frame)
                renderReversed(window, if(project.novaConf.saveReplays) "On" else "Off", 302 * scale, 85 * scale, selectedIndex == 3, frame)
                renderReversed(window, if(project.novaConf.attract) "On" else "Off", 302 * scale, 98 * scale, selectedIndex == 4, frame)

                when(selectedIndex) {
                    0 -> window.showText("Set the location for DOSBox", MenuFonts.smallFont_gray, hintCoordX, hintCoordY)
                    1 -> window.showText("Set the location for OMF2097", MenuFonts.smallFont_gray, hintCoordX, hintCoordY)
                    2 -> window.showText("Turn this off if you are using a program like AntiMicro to map controllers to the\nkeyboard.", MenuFonts.smallFont_gray, hintCoordX, hintCoordY)
                    3 -> window.showText("Turn this on to automatically get REC files after every match.", MenuFonts.smallFont_gray, hintCoordX, hintCoordY)
                    4 -> window.showText("Turn this on to automatically play random replays when idle.", MenuFonts.smallFont_gray, hintCoordX, hintCoordY)
                }
            }
            State.DOSBOXCONF -> {
                if(selectedIndex < 0) selectedIndex = 2
                if(selectedIndex > 2) selectedIndex = 0

                renderText(window, "Is DOSBox Staging?", 18 * scale, 20 * scale, selectedIndex == 0, frame)
                renderText(window, "Use My DOSBox Settings", 18 * scale, 33 * scale, selectedIndex == 1, frame)
                renderText(window, "Custom DOSBox Conf Path", 18 * scale, 46 * scale, selectedIndex == 2, frame)

                renderReversed(window,
                    if(project.novaConf.stagingCompat) "Yes" else "No", 302 * scale, 20 * scale, selectedIndex == 0, frame)
                renderReversed(window, if(project.novaConf.userConf) "Yes" else "No", 302 * scale, 33 * scale, selectedIndex == 1, frame)
                window.showText(project.novaConf.confPath.reversed(), MenuFonts.smallFont_yellow,302*scale, 59*scale, TextAlignment.RIGHT)

                when(selectedIndex) {
                    0 -> window.showText("Turn this on if you're having an issue with keyboard controls.", MenuFonts.smallFont_gray, hintCoordX, hintCoordY)
                    1 -> window.showText("Turn this on to load DOSBox settings from your user profile. (-userconf parameter)", MenuFonts.smallFont_gray, hintCoordX, hintCoordY)
                    2 -> window.showText("If you have a specific DOSBox configuration for OMF, specify it here.", MenuFonts.smallFont_gray, hintCoordX, hintCoordY)
                }
            }
            State.OMFCONF -> {
                if(selectedIndex < 0) selectedIndex = 6
                if(selectedIndex > 6) selectedIndex = 0

                renderText(window, "Run Setup", 18 * scale, 20 * scale, selectedIndex == 0, frame, !(canRunDosbox()&&canRunSetup()))
                renderText(window, "Hyper Mode", 18 * scale, 33 * scale, selectedIndex == 1, frame, !canConfigOmf())
                renderText(window, "Rehit Mode", 18 * scale, 46 * scale, selectedIndex == 2, frame, !canConfigOmf())
                renderText(window, "Stage Hazards", 18 * scale, 59 * scale, selectedIndex == 3, frame, !canConfigOmf())
                renderText(window, "Game Speed", 18 * scale, 72 * scale, selectedIndex == 4, frame, !canConfigOmf())
                renderText(window, "Player 1 Power", 18 * scale, 85 * scale, selectedIndex == 5, frame, !canConfigOmf())
                renderText(window, "Player 2 Power", 18 * scale, 98 * scale, selectedIndex == 6, frame, !canConfigOmf())

                project.omfConfig?.apply {
                    val p1PowerString = "${1 / (2.25 - .25*p1power)}".take(5)
                    val p2PowerString = "${1 / (2.25 - .25*p2power)}".take(5)
                    renderReversed(window, if (hyperMode) "On" else "Off", 302 * scale, 33 * scale, selectedIndex == 1, frame)
                    renderReversed(window, if (rehitMode) "On" else "Off", 302 * scale, 46 * scale, selectedIndex == 2, frame)
                    renderReversed(window, if (hazards) "On" else "Off", 302 * scale, 59 * scale, selectedIndex == 3, frame)
                    renderReversed(window, "${speed*10}%", 302 * scale, 72 * scale, selectedIndex == 4, frame)
                    renderReversed(window, "${p1PowerString}x", 302 * scale, 85 * scale, selectedIndex == 5, frame)
                    renderReversed(window, "${p2PowerString}x", 302 * scale, 98 * scale, selectedIndex == 6, frame)
                }

                val hintText = when (selectedIndex) {
                    0 -> "Enter game setup, mandatory for fresh copies of OMF."
                    1 -> "Allow some special moves to be performed in the air."
                    2 -> "Allow air juggle combos."
                    3 -> "Enable or disable stage hazards."
                    4 -> "You should leave this at 80% in most cases."
                    5 -> "A handicap modifier, you may want to lower this for training."
                    6 -> "A handicap modifier, you may want to lower this for training."
                    else -> ""
                }
                window.showText(hintText, MenuFonts.smallFont_gray, hintCoordX, hintCoordY)
            }
        }
    }

    fun select() {
        when(currentState) {
            State.NOVACONF -> {
                project.novaConf.apply {
                    when (selectedIndex) {
                        0 -> dosboxPath =
                            project.showFileChooser(dosboxPath.ifEmpty { ".\\DOSBox.exe" }, "Select DOSBox.exe", "*.exe", "executable files")
                        1 -> {
                            omfPath = project.showFolderChooser(omfPath.ifEmpty { "." }, "Select OMF2097 Location")
                            project.loadOmfConfig()
                        }
                        2 -> {
                            joyEnabled = !joyEnabled
                            project.setJoystickEnabled(joyEnabled)
                        }
                        3 -> saveReplays = !saveReplays
                        4 -> attract = !attract
                    }
                    checkErrors()
                }
            }
            State.DOSBOXCONF -> {
                project.novaConf.apply {
                    when(selectedIndex) {
                        0 -> stagingCompat = !stagingCompat
                        1 -> userConf = !userConf
                        2 -> confPath = project.showFileChooser(confPath.ifEmpty { ".\\*.conf" }, "Select DOSBox configuration", "*.conf", "DOSBox configuration file")
                    }
                }
            }
            State.OMFCONF -> {
                if(selectedIndex == 0) {
                    if(canRunDosbox() && canRunSetup()) {
                        project.startSetup()
                    }
                }
                project.omfConfig?.apply {
                    when(selectedIndex) {
                        1 -> hyperMode = !hyperMode
                        2 -> rehitMode = !rehitMode
                        3 -> hazards = !hazards
                        4 -> speed = (speed + 1) % 11
                        5 -> p1power = (p1power + 1)
                        6 -> p2power = (p2power + 1)
                    }
                }
            }
            else -> {}
        }
    }

    fun cancel() {
        if(currentState != State.MENU)
            currentState = State.MENU
    }

    fun startConfig(): Boolean {
        when(selectedIndex) {
            0 -> currentState = State.NOVACONF
            1 -> {
                if((canRunSetup()&&canRunDosbox()) || canConfigOmf()) {
                    currentState = State.OMFCONF
                } else {
                    return false
                }
            }
            2 -> currentState = State.DOSBOXCONF
        }
        selectedIndex = 0
        return true
    }
}