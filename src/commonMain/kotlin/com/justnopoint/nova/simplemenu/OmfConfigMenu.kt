package com.justnopoint.nova.simplemenu

import com.justnopoint.nova.NovaProject
import com.justnopoint.nova.OMFConf

class OmfConfigMenu(val project: NovaProject, self: SimpleMenuItem): SimpleMenu(self) {
    init {
        addItem(SimpleMenuItem.Builder()
            .setTitle("Run Setup")
            .setHint("Enter game setup, mandatory for fresh copies of OMF.")
            .setter {
                if(canRunDosbox() && canRunSetup()) {
                    project.startSetup()
                }
            }
            .build())
        addItem(SimpleMenuItem.Builder()
            .setTitle("Hyper Mode")
            .setHint("Allow some special moves to be performed in the air.")
            .getter {
                if (project.omfConfig?.hyperMode == true) "On" else "Off"
            }
            .setter {
                project.omfConfig?.apply {
                    hyperMode = !hyperMode
                }
            }
            .build())
        addItem(SimpleMenuItem.Builder()
            .setTitle("Rehit Mode")
            .setHint("Allow air juggle combos.")
            .getter {
                if (project.omfConfig?.rehitMode == true) "On" else "Off"
            }
            .setter {
                project.omfConfig?.apply {
                    rehitMode = !rehitMode
                }
            }
            .build())
        addItem(SimpleMenuItem.Builder()
            .setTitle("Stage Hazards")
            .setHint("Enable or disable stage hazards.")
            .getter {
                if (project.omfConfig?.hazards == true) "On" else "Off"
            }
            .setter {
                project.omfConfig?.apply {
                    hazards = !hazards
                }
            }
            .build())
        addItem(SimpleMenuItem.Builder()
            .setTitle("Game Speed")
            .setHint("You should leave this at 80% in most cases.")
            .getter {
                "${(project.omfConfig?.speed?:0)*10}%"
            }
            .setter {
                project.omfConfig?.apply {
                    speed = (speed + 1) % 11
                }
            }
            .build())
        addItem(SimpleMenuItem.Builder()
            .setTitle("Player 1 Power")
            .setHint("A handicap modifier, you may want to lower this for training.")
            .getter {
                val p1Power = project.omfConfig?.p1power?:0
                val p1PowerString = "${1 / (2.25 - .25*p1Power)}".take(5)
                "${p1PowerString}x"
            }
            .setter {
                project.omfConfig?.apply {
                    p1power = (p1power + 1)
                }
            }
            .build())
        addItem(SimpleMenuItem.Builder()
            .setTitle("Player 2 Power")
            .setHint("A handicap modifier, you may want to lower this for training.")
            .getter {
                val p2Power = project.omfConfig?.p2power?:0
                val p2PowerString = "${1 / (2.25 - .25*p2Power)}".take(5)
                "${p2PowerString}x"
            }
            .setter {
                project.omfConfig?.apply {
                    p2power = (p2power + 1)
                }
            }
            .build())
    }

    private fun canRunSetup(): Boolean {
        val missingFiles = project.novaConf.validateOmfSetup()
        return !missingFiles.contains(OMFConf.SETUP)
    }

    private fun canRunDosbox(): Boolean {
        return project.novaConf.validateDosbox() == null
    }
}