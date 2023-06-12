package com.justnopoint.nova.menu

import com.justnopoint.nova.*
import okio.Path.Companion.toPath

class MainMenu(project: NovaProject) : NovaMenu(project) {
    enum class State {
        MENU, KEYCONFIG1, GAME, REPLAY, NOVA_CONFIG
    }

    private var selectedIndex = 0
    private var currentState = State.MENU

    private var idleCounter = 0
    private var idleLimit = 60 * 90

    private val keyConfigMenu = KeyConfigMenu(project)
    private val gameMenu = GameMenu(project)
    private val configMenu = ConfigMenu(project)
    private val replayMenu = ReplayMenu(project)

    override fun reset() {
        idleCounter = 0
        currentState = State.MENU
        selectedIndex = 0
        keyConfigMenu.reset()
        gameMenu.reset()
        replayMenu.reset()
    }

    fun gameEnd() {
        idleCounter = 0
        currentState = State.MENU
        gameMenu.reset()
    }

    private fun canPlay(): Boolean {
        val dosboxErrors = project.novaConf.validateDosbox()
        if(dosboxErrors != null) return false
        val missingFiles = project.novaConf.validateOmfSetup()
        if(missingFiles.contains("FILE0001.EXE")) return false
        if(missingFiles.contains(SoundCard.FILENAME)) return false
        return true
    }

    override fun render(window: NovaWindow, frame: Int) {
        idleCounter++

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
            2 -> {
                if(canPlay())
                    replayMenu.render(window, frame)
                else
                    window.showText("Check settings!", MenuFonts.smallFont_red, hintCoordX, hintCoordY)
            }
            3 -> configMenu.render(window, frame)
        }

        if(project.novaConf.attract) {
            if (currentState != State.GAME) {
                if (idleCounter > idleLimit) {
                    currentState = State.GAME
                    replayMenu.randomReplay()
                }
            }
        }
    }

    override fun handleInput(input: ButtonMap) {
        if(debug) {
            writeLog(input.toString())
        }
        idleCounter = 0

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
                    if(canPlay()) {
                        replayMenu.loadReplays(project.novaConf.omfPath.toPath())
                    }
                }
            }
        }
    }

    override fun menuInput(menuInput: MenuInput, input: ButtonMap) {
        when(menuInput) {
            MenuInput.LEFT -> left()
            MenuInput.RIGHT -> right()
            MenuInput.SELECT -> select()
            MenuInput.CANCEL -> cancel()
            else -> {
                when(selectedIndex) {
                    0 -> gameMenu.menuInput(menuInput, input)
                    1 -> keyConfigMenu.menuInput(menuInput, input)
                    2 -> replayMenu.menuInput(menuInput, input)
                    3 -> configMenu.menuInput(menuInput, input)
                }
            }
        }
    }

    fun left() {
        selectedIndex--
        if(selectedIndex < 0) selectedIndex = 3
    }

    fun right() {
        selectedIndex++
        if(selectedIndex > 3) selectedIndex = 0
    }

    fun select() {
        when(selectedIndex) {
            0 -> {
                if(canPlay()) {
                    currentState = State.GAME
                    gameMenu.select()
                    if(gameMenu.currentState == GameMenu.State.MENU) {
                        currentState = State.MENU
                    }
                }
            }
            1 -> {
                if(keyConfigMenu.startConfig()) {
                    currentState = State.KEYCONFIG1
                }
            }
            2 -> {
                if(canPlay()) {
                    currentState = State.GAME
                    replayMenu.select()
                }
            }
            3 -> {
                if(configMenu.startConfig()) {
                    currentState = State.NOVA_CONFIG
                }
            }
        }
    }

    fun cancel() {
        //if(currentState == State.MENU) {
        //    project.quit()
        //}
    }
}