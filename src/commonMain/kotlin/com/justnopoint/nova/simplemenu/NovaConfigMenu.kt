package com.justnopoint.nova.simplemenu

import com.justnopoint.nova.MenuFonts
import com.justnopoint.nova.NovaProject
import com.justnopoint.nova.TextAlignment

class NovaConfigMenu(project: NovaProject, self: SimpleMenuItem): SimpleMenu(self) {
    init {
        addItem(SimpleMenuItem.Builder()
            .setTitle("DOSBox location")
            .setValueFont(MenuFonts.menuFontYellow)
            .setValueAlignment(TextAlignment.LEFT)
            .getter { project.novaConf.dosboxPath }
            .setHint("Set the location for DOSBox")
            .setter {
                project.novaConf.apply {
                    dosboxPath =
                        project.showFileChooser(dosboxPath.ifEmpty { ".\\DOSBox.exe" }, "Select DOSBox.exe", "*.exe", "executable files")
                }
            }
            .build())
        addItem(SimpleMenuItem.Builder()
            .setTitle("OMF location")
            .setValueFont(MenuFonts.menuFontYellow)
            .setValueAlignment(TextAlignment.LEFT)
            .getter { project.novaConf.dosboxPath }
            .setHint("Set the location for OMF2097")
            .setter {
                project.novaConf.apply {
                    omfPath = project.showFolderChooser(omfPath.ifEmpty { "." }, "Select OMF2097 Location")
                    project.loadOmfConfig()
                }
            }
            .build())
        addItem(SimpleMenuItem.Builder()
            .setTitle("Joystick Support")
            .setHint("Turn this off if you are using a program like AntiMicro to map controllers to the\nkeyboard.")
            .getter { if(project.novaConf.joyEnabled) "On" else "Off" }
            .setter {
                project.novaConf.apply {
                    joyEnabled = !joyEnabled
                    project.setJoystickEnabled(joyEnabled)
                }
            }
            .build())
        addItem(SimpleMenuItem.Builder()
            .setTitle("Save Replays")
            .setHint("Turn this on to automatically get REC files after every match.")
            .getter { if(project.novaConf.saveReplays) "On" else "Off" }
            .setter {
                project.novaConf.apply {
                    saveReplays = !saveReplays
                }
            }
            .build())
        addItem(SimpleMenuItem.Builder()
            .setTitle("Attract Mode")
            .setHint("Turn this on to automatically play random replays when idle.")
            .getter { if(project.novaConf.attract) "On" else "Off" }
            .setter {
                project.novaConf.apply {
                    attract = !attract
                }
            }
            .build())
    }
}