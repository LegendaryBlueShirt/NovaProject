package com.justnopoint.nova.simplemenu

import com.justnopoint.nova.MenuFonts
import com.justnopoint.nova.NovaProject
import com.justnopoint.nova.NovaWindow

class ConfigMenu2(project: NovaProject): SimpleMenu(TopLevelItem) {
    enum class SubmenuMode {
        NONE, NOVA, OMF, DOSBOX
    }
    private var submenuMode = SubmenuMode.NONE
    init {
        addItem(NovaConfigMenu(project, SimpleMenuItem.Builder()
            .setTitle("Nova Project Settings")
            .setter { submenuMode = SubmenuMode.NOVA }
            .build()))
        addItem(OmfConfigMenu(project, SimpleMenuItem.Builder()
            .setTitle("OMF2097 Settings")
            .setter { submenuMode = SubmenuMode.OMF }
            .build()))
        addItem(DosboxConfigMenu(project, SimpleMenuItem.Builder()
            .setTitle("DOSBox Settings")
            .setter { submenuMode = SubmenuMode.DOSBOX }
            .build()))
    }
}