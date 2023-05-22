package com.justnopoint.nova.simplemenu

import com.justnopoint.nova.MenuFonts
import com.justnopoint.nova.NovaProject

class DosboxConfigMenu(project: NovaProject, self: SimpleMenuItem): SimpleMenu(self) {
    init {
        addItem(SimpleMenuItem.Builder()
            .setTitle("Is DOSBox Staging?")
            .getter { if(project.novaConf.stagingCompat) "Yes" else "No" }
            .setter {
                project.novaConf.apply {
                    stagingCompat = !stagingCompat
                }
            }
            .setHint("Turn this on if you're having an issue with keyboard controls.")
            .build())
        addItem(SimpleMenuItem.Builder()
            .setTitle("Use My DOSBox Settings")
            .getter { if(project.novaConf.userConf) "Yes" else "No" }
            .setter {
                project.novaConf.apply {
                    userConf = !userConf
                }
            }
            .setHint("Turn this on to load DOSBox settings from your user profile. (-userconf parameter)")
            .build())
        addItem(SimpleMenuItem.Builder()
            .setTitle("Custom DOSBox Conf Path")
            .setValueFont(MenuFonts.menuFontYellow)
            .getter { project.novaConf.confPath }
            .setter {
                project.novaConf.apply {
                    confPath = project.showFileChooser(
                        confPath.ifEmpty { ".\\*.conf" },
                        "Select DOSBox configuration",
                        "*.conf",
                        "DOSBox configuration file")
                }
            }
            .setHint("If you have a specific DOSBox configuration for OMF, specify it here.")
            .build())
    }
}