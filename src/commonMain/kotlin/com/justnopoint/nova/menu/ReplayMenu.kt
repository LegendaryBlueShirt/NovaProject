package com.justnopoint.nova.menu

import com.justnopoint.nova.*
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import platform.posix.rand

class ReplayMenu(project: NovaProject) : NovaMenu(project) {
    private var firstItem = 0
    private val maximumReplays = 8

    private var selected = 0
    private var replays: List<Replay> = emptyList()

    init {
        val omfpath = project.novaConf.omfPath
        if(omfpath.isNotBlank()) {
            loadReplays(omfpath.toPath())
        }
    }

    override fun reset() {
        selected = 0
    }

    override fun render(window: NovaWindow, frame: Int) {
        if(replays.isEmpty()) return

        if(selected >= replays.size) selected = 0
        if(selected < 0) selected = replays.size - 1
        if(selected < firstItem) firstItem = selected
        if(selected >= firstItem+maximumReplays) firstItem = selected-maximumReplays+1

        replays.drop(firstItem).take(maximumReplays).forEachIndexed { index, replay ->
            replay.apply {
                val instant = Instant.fromEpochMilliseconds(timestamp)
                val date = instant.toLocalDateTime(TimeZone.currentSystemDefault())
                val replayText = "$date"
                renderText(window,
                    replayText,
                    18 * scale,
                    (20 + 13*index)*scale,
                    selected == (firstItem+index),
                    frame)
            }
        }

        replays[selected].apply {
            window.showText("$p1Name/$p1Har vs. $p2Name/$p2Har", MenuFonts.smallFont_sand, hintCoordX, hintCoordY)
            window.showText("Last frame - $lastTick".reversed(), MenuFonts.smallFont_yellow, 612, hintCoordY, TextAlignment.RIGHT)
            window.showText("Rounds -", MenuFonts.smallFont_sand, hintCoordX, hintCoordY+16)
            window.showText(getRounds(), MenuFonts.smallFont_yellow, hintCoordX+70, hintCoordY+16)
            window.showText("Hazards -", MenuFonts.smallFont_sand, hintCoordX+166, hintCoordY+16)
            window.showText(if(hazards) "On" else "Off", MenuFonts.smallFont_yellow, hintCoordX+238, hintCoordY+16)
            window.showText("Hyper -", MenuFonts.smallFont_sand, hintCoordX+276, hintCoordY+16)
            window.showText(if(hyperMode) "On" else "Off", MenuFonts.smallFont_yellow, hintCoordX+332, hintCoordY+16)
            window.showText("Rehit -", MenuFonts.smallFont_sand, hintCoordX+374, hintCoordY+16)
            window.showText(if(rehit) "On" else "Off", MenuFonts.smallFont_yellow, hintCoordX+428, hintCoordY+16)
            window.showText(stage, MenuFonts.smallFont_yellow, hintCoordX+472, hintCoordY+16)
        }
    }

    fun loadReplays(folder: Path) {
        selected = 0
        firstItem = 0

        val fs = FileSystem.SYSTEM
        if(!fs.exists(folder)) {
            replays = emptyList()
            return
        }
        val replayFiles = fs.list(folder).filter { it.name.endsWith(suffix = ".rec", ignoreCase = true) }
        replays = replayFiles.mapNotNull {
            try {
                Replay.loadReplay(it)
            } catch (e: Exception) {
                null
            }
        }
    }

    override fun menuInput(menuInput: MenuInput, input: ButtonMap) {
        when(menuInput) {
            MenuInput.UP -> up()
            MenuInput.DOWN -> down()
            MenuInput.SELECT -> select()
            else -> {}
        }
    }

    fun up() {
        selected--
    }

    fun down() {
        selected++
    }

    fun select() {
        val relative = replays[selected].path.relativeTo(project.novaConf.omfPath.toPath())
        project.startReplay(relative)
    }

    fun randomReplay() {
        if(replays.isEmpty()) return
        val randomReplay = replays.indices.random()
        val relative = replays[randomReplay].path.relativeTo(project.novaConf.omfPath.toPath())
        project.startReplay(relative)
    }
}