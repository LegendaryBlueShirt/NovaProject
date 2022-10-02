package com.justnopoint.nova.menu

import com.justnopoint.nova.*

class MainMenu(project: NovaProject) : NovaMenu(project) {
    enum class State {
        MENU, KEYCONFIG1, GAME, REPLAY, NOVA_CONFIG
    }

    private var selectedIndex = 0
    private var currentState = State.MENU

    private val keyConfigMenu = KeyConfigMenu(project)
    private val gameMenu = GameMenu(project)
    private val configMenu = ConfigMenu(project)

    override fun reset() {
        currentState = State.MENU
        selectedIndex = 0
        keyConfigMenu.reset()
        gameMenu.reset()
    }

    private fun canPlay(): Boolean {
        return project.novaConf.dosboxPath.isNotBlank() && (project.omfConfig!=null)
    }

    override fun render(window: NovaWindow, frame: Int) {
        val coordY = 140 * scale

        if(currentState != State.GAME) {
            renderText(window, "Play", 14*scale, coordY, selectedIndex == 0, frame, !canPlay())
            renderText(window, "Controls", 62*scale, coordY, selectedIndex == 1, frame)
            renderText(window, "Replay", 140*scale, coordY, selectedIndex == 2, frame)
            renderText(window, "Settings", 208*scale, coordY, selectedIndex == 3, frame)
        }

        when(selectedIndex) {
            0 -> {
                if(canPlay())
                    gameMenu.render(window, frame)
                else
                    window.showText("Check settings!", MenuFonts.smallFont_red, hintCoordX, hintCoordY)
            }
            1 -> keyConfigMenu.render(window, frame)
            3 -> configMenu.render(window, frame)
        }
    }

    override fun handleInput(input: ButtonMap) {
        when(currentState) {
            State.MENU -> super.handleInput(input)
            State.KEYCONFIG1 -> {
                keyConfigMenu.handleInput(input)
                if(keyConfigMenu.currentState == KeyConfigMenu.KeyConfigState.MENU)
                    currentState = State.MENU
            }
            State.GAME -> { /** No-op **/ }
            State.REPLAY -> {}
            State.NOVA_CONFIG -> {
                configMenu.handleInput(input)
                if(configMenu.currentState == ConfigMenu.State.MENU) {
                    currentState = State.MENU
                    project.loadOmfConfig()
                }
            }
        }
    }

    override fun left() {
        selectedIndex--
        if(selectedIndex < 0) selectedIndex = 3
    }

    override fun right() {
        selectedIndex++
        if(selectedIndex > 3) selectedIndex = 0
    }

    override fun up() {
        when(selectedIndex) {
            0 -> gameMenu.up()
            1 -> keyConfigMenu.up()
            3 -> configMenu.up()
        }
    }

    override fun down() {
        when(selectedIndex) {
            0 -> gameMenu.down()
            1 -> keyConfigMenu.down()
            3 -> configMenu.down()
        }
    }

    override fun select() {
        when(selectedIndex) {
            0 -> {
                if(canPlay()) {
                    currentState = State.GAME
                    gameMenu.select()
                }
            }
            1 -> {
                currentState = State.KEYCONFIG1
                keyConfigMenu.startConfig()
            }
            2 -> {
                //REPLAYS
            }
            3 -> {
                if(configMenu.startConfig()) {
                    currentState = State.NOVA_CONFIG
                }
            }
        }
    }

    override fun cancel() {
        //if(currentState == State.MENU) {
        //    project.quit()
        //}
    }
}