package com.justnopoint.nova.simplemenu

import com.justnopoint.nova.NovaWindow

object TopLevelItem: MenuItem {
    override fun onSelect() {}

    override fun getHeight(): Int {
        return 0
    }

    override fun render(window: NovaWindow, selected: Boolean, frame: Int, y: Int) {}
}