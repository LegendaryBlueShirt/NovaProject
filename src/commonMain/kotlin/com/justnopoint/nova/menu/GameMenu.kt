package com.justnopoint.nova.menu

import com.justnopoint.nova.ButtonMap
import com.justnopoint.nova.MenuFonts
import com.justnopoint.nova.NovaProject
import com.justnopoint.nova.NovaWindow

class GameMenu(project: NovaProject) : NovaMenu(project) {
    private var selectedIndex = 0

    enum class State {
        MENU, GAME, TRAINING
    }
    var currentState = State.MENU

    override fun reset() {
        currentState = State.MENU
        selectedIndex = 0
    }

    override fun render(window: NovaWindow, frame: Int) {
        if(selectedIndex < 0) selectedIndex = 4
        if(selectedIndex > 4) selectedIndex = 0

        when (currentState) {
            State.MENU -> {
                renderText(window, "Two Player Versus", 18 * scale, 20 * scale, selectedIndex == 0, frame)
                renderText(window, "Training Mode", 18 * scale, 33 * scale, selectedIndex == 1, frame)
                renderText(window, "Normal Start", 18 * scale, 46 * scale, selectedIndex == 2, frame)
                renderText(window, "Pilot Names", 18 * scale, 59 * scale, selectedIndex == 3, frame)
                renderText(window, "Enhancement", 18 * scale, 72 * scale, selectedIndex == 4, frame)
                renderText(window, "${project.novaConf.enhancement}", 180 * scale, 72 * scale, selectedIndex == 4, frame)

                val hintText = when (selectedIndex) {
                    0 -> "Fight against another player!"
                    1 -> "Start two player mode with helpful features enabled."
                    2 -> "Start the game normally, you will be taken to the normal game menu."
                    else -> ""
                }
                window.showText(hintText, MenuFonts.smallFont_gray, hintCoordX, hintCoordY)
            }
            State.GAME -> {
                renderText(window, "One Must Fall 2097", 104 * scale, 72 * scale, false, frame)
            }
            State.TRAINING -> {
                renderText(window, "F1  Reset left side", 104 * scale, 59 * scale, false, frame)
                renderText(window, "F2  Reset center", 104 * scale, 72 * scale, false, frame)
                renderText(window, "F3  Reset right side", 104 * scale, 85 * scale, false, frame)
            }
        }
    }

    override fun menuInput(menuInput: MenuInput, input: ButtonMap) {
        when(menuInput) {
            MenuInput.SELECT -> select()
            MenuInput.UP -> up()
            MenuInput.DOWN -> down()
            else -> {}
        }
    }

    fun select() {
        when(selectedIndex) {
            0 -> {
                project.startVs()
                currentState = State.GAME
            }
            1 -> {
                project.startTraining()
                currentState = State.TRAINING
            }
            2 -> {
                project.startNormal()
                currentState = State.GAME
            }
            3 -> {
                project.getPilotNames()
                currentState = State.MENU
            }
            4 -> {
                project.novaConf.enhancement++
                if(project.novaConf.enhancement > 3) {
                    project.novaConf.enhancement = 0
                }
                currentState = State.MENU
            }
        }
    }

    fun up() {
        selectedIndex--
    }

    fun down() {
        selectedIndex++
    }
}