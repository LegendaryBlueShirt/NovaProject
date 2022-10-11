package com.justnopoint.nova.menu

import com.justnopoint.nova.MenuFonts
import com.justnopoint.nova.NovaProject
import com.justnopoint.nova.NovaWindow

class ConfigMenu(project: NovaProject) : NovaMenu(project) {
    private var selectedIndex = 0
    enum class State {
        MENU, NOVACONF, OMFCONF, DOSBOXCONF
    }
    var currentState = State.MENU

    override fun up() {
        selectedIndex--
    }

    override fun down() {
        selectedIndex++
    }

    override fun reset() {
        selectedIndex = 0
    }

    private fun canConfigOmf(): Boolean {
        return project.omfConfig != null
    }

    override fun render(window: NovaWindow, frame: Int) {
        when(currentState) {
            State.MENU -> {
                if(selectedIndex < 0) selectedIndex = 2
                if(selectedIndex > 2) selectedIndex = 0

                renderText(window, "Nova Project Settings", 18 * scale, 20 * scale, selectedIndex == 0, frame)
                renderText(window, "OMF2097 Settings", 18 * scale, 33 * scale, selectedIndex == 1, frame, !canConfigOmf())
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

                window.showText(project.novaConf.dosboxPath.reversed(), MenuFonts.smallFont_yellow,302*scale, 33*scale, true)
                window.showText(project.novaConf.omfPath.reversed(), MenuFonts.smallFont_yellow,302*scale, 59*scale, true)
                renderReversed(window, if(project.novaConf.joyEnabled) "On" else "Off", 302 * scale, 72 * scale, selectedIndex == 3, frame)
                renderReversed(window, if(project.novaConf.saveReplays) "On" else "Off", 302 * scale, 85 * scale, selectedIndex == 5, frame)
                renderReversed(window, if(project.novaConf.attract) "On" else "Off", 302 * scale, 98 * scale, selectedIndex == 6, frame)

                when(selectedIndex) {
                    0 -> window.showText("Set the location for DOSBox", MenuFonts.smallFont_gray, hintCoordX, hintCoordY)
                    1 -> window.showText("Set the location for OMF2097", MenuFonts.smallFont_gray, hintCoordX, hintCoordY)
                    2 -> window.showText("Turn this off if you are using a program like AntiMicro to map controllers to the\nkeyboard.", MenuFonts.smallFont_gray, hintCoordX, hintCoordY)
                    3 -> window.showText("Turn this on to automatically get REC files after every match.", MenuFonts.smallFont_gray, hintCoordX, hintCoordY)
                    4 -> window.showText("Turn this on to automatically play random replays when idle.", MenuFonts.smallFont_gray, hintCoordX, hintCoordY)
                }
            }
            State.DOSBOXCONF -> {
                renderText(window, "Is DOSBox Staging?", 18 * scale, 20 * scale, selectedIndex == 0, frame)
                renderText(window, "Use My DOSBox Settings", 18 * scale, 33 * scale, selectedIndex == 1, frame)
                renderText(window, "Custom DOSBox Conf Path", 18 * scale, 46 * scale, selectedIndex == 2, frame)

                renderReversed(window,
                    if(project.novaConf.stagingCompat) "Yes" else "No", 302 * scale, 20 * scale, selectedIndex == 0, frame)
                renderReversed(window, if(project.novaConf.userConf) "Yes" else "No", 302 * scale, 33 * scale, selectedIndex == 1, frame)
                window.showText(project.novaConf.confPath.reversed(), MenuFonts.smallFont_yellow,302*scale, 59*scale, true)

                when(selectedIndex) {
                    0 -> window.showText("Turn this on if you're having an issue with keyboard controls.", MenuFonts.smallFont_gray, hintCoordX, hintCoordY)
                    1 -> window.showText("Turn this on to load DOSBox settings from your user profile. (-userconf parameter)", MenuFonts.smallFont_gray, hintCoordX, hintCoordY)
                    2 -> window.showText("If you have a specific DOSBox configuration for OMF, specify it here.", MenuFonts.smallFont_gray, hintCoordX, hintCoordY)
                }
            }
            State.OMFCONF -> {
                if(selectedIndex < 0) selectedIndex = 5
                if(selectedIndex > 5) selectedIndex = 0

                renderText(window, "Hyper Mode", 18 * scale, 20 * scale, selectedIndex == 0, frame)
                renderText(window, "Rehit Mode", 18 * scale, 33 * scale, selectedIndex == 1, frame)
                renderText(window, "Stage Hazards", 18 * scale, 46 * scale, selectedIndex == 2, frame)
                renderText(window, "Game Speed", 18 * scale, 59 * scale, selectedIndex == 3, frame)
                renderText(window, "Player 1 Power", 18 * scale, 72 * scale, selectedIndex == 4, frame)
                renderText(window, "Player 2 Power", 18 * scale, 85 * scale, selectedIndex == 5, frame)

                project.omfConfig?.apply {
                    val p1PowerString = "${1 / (2.25 - .25*p1power)}".take(5)
                    val p2PowerString = "${1 / (2.25 - .25*p2power)}".take(5)
                    renderReversed(window, if (hyperMode) "On" else "Off", 302 * scale, 20 * scale, selectedIndex == 0, frame)
                    renderReversed(window, if (rehitMode) "On" else "Off", 302 * scale, 33 * scale, selectedIndex == 1, frame)
                    renderReversed(window, if (hazards) "On" else "Off", 302 * scale, 46 * scale, selectedIndex == 2, frame)
                    renderReversed(window, "${speed*10}%", 302 * scale, 59 * scale, selectedIndex == 3, frame)
                    renderReversed(window, "${p1PowerString}x", 302 * scale, 72 * scale, selectedIndex == 4, frame)
                    renderReversed(window, "${p2PowerString}x", 302 * scale, 85 * scale, selectedIndex == 5, frame)
                }

                when(selectedIndex) {
                    0 -> window.showText("Allow some special moves to be performed in the air.", MenuFonts.smallFont_gray, hintCoordX, hintCoordY)
                    1 -> window.showText("Allow air juggle combos.", MenuFonts.smallFont_gray, hintCoordX, hintCoordY)
                    2 -> window.showText("Enable or disable stage hazards.", MenuFonts.smallFont_gray, hintCoordX, hintCoordY)
                    3 -> window.showText("You should leave this at 80% in most cases.", MenuFonts.smallFont_gray, hintCoordX, hintCoordY)
                    4 -> window.showText("A handicap modifier, you may want to lower this for training.", MenuFonts.smallFont_gray, hintCoordX, hintCoordY)
                    5 -> window.showText("A handicap modifier, you may want to lower this for training.", MenuFonts.smallFont_gray, hintCoordX, hintCoordY)
                }
            }
        }
    }

    override fun select() {
        when(currentState) {
            State.NOVACONF -> {
                project.novaConf.apply {
                    when (selectedIndex) {
                        0 -> dosboxPath =
                            project.showFileChooser(dosboxPath.ifEmpty { ".\\DOSBox.exe" }, "Select DOSBox.exe", "*.exe", "executable files")
                        1 -> omfPath = project.showFolderChooser(omfPath.ifEmpty { "." }, "Select OMF2097 Location")
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
                project.omfConfig?.apply {
                    when(selectedIndex) {
                        0 -> hyperMode = !hyperMode
                        1 -> rehitMode = !rehitMode
                        2 -> hazards = !hazards
                        3 -> speed = (speed + 1) % 11
                        4 -> p1power = (p1power + 1)
                        5 -> p2power = (p2power + 1)
                    }
                }
            }
            else -> {}
        }
    }

    override fun cancel() {
        if(currentState != State.MENU)
            currentState = State.MENU
    }

    fun startConfig(): Boolean {
        when(selectedIndex) {
            0 -> currentState = State.NOVACONF
            1 -> {
                if(canConfigOmf()) {
                    currentState = State.OMFCONF
                } else {
                    return false
                }
            }
            2 -> currentState = State.DOSBOXCONF
        }
        return true
    }
}