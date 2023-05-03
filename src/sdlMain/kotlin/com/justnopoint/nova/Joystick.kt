package com.justnopoint.nova

import SDL.*
import kotlinx.cinterop.CPointer

class Joystick(private val controllerId: Int, private val controller: CPointer<SDL_Joystick>): Input {
    private val buttons: Map<UByte, Button>
    private val axes: Map<UByte, Axis>
    private val hats: Map<UByte, Hat>

    init {
        writeLog("Creating joystick $controllerId")
        val nButtons = SDL_JoystickNumButtons(controller)
        writeLog("Found $nButtons buttons")
        buttons = (0 until nButtons).associate { it.toUByte() to Button(it, false) }
        val nAxes = SDL_JoystickNumAxes(controller)
        writeLog("Found $nAxes axes")
        axes = (0 until nAxes).associate { it.toUByte() to Axis(it, 0) }
        val nHats = SDL_JoystickNumHats(controller)
        writeLog("Found $nHats hats")
        hats = (0 until nHats).associate { it.toUByte() to Hat(it, 0) }
    }

    private val events = ArrayDeque<ControllerEvent>()

    override fun updateControllerButton(event: SDL_Event) {
        when (event.type) {
            SDL_JOYBUTTONUP -> {
                val button = event.jbutton.button
                buttons[button]?.let {
                    if (it.pressed) {
                        it.pressed = false
                        events.addLast(ControllerEvent(ControlType.BUTTON, button.toInt(), 0, true))
                    }
                }
            }

            SDL_JOYBUTTONDOWN -> {
                val button = event.jbutton.button
                buttons[button]?.let {
                    if (!it.pressed) {
                        it.pressed = true
                        events.addLast(ControllerEvent(ControlType.BUTTON, button.toInt(), 0, false))
                    }
                }
            }
        }
    }

    override fun updateControllerAxis(event: SDL_Event) {
        val value = event.jaxis.value
        val dir = if(value < -12800) -1 else if(value > 12800) 1 else 0
        val axis = event.jaxis.axis
        axes[axis]?.let {
            if(dir != it.direction) {
                if(it.direction != 0) {
                    events.addLast(ControllerEvent(ControlType.AXIS, axis.toInt(), it.direction, true))
                }
                if(dir != 0) {
                    events.addLast(ControllerEvent(ControlType.AXIS, axis.toInt(), dir, false))
                }
                it.direction = dir
            }
        }
    }

    override fun updateControllerHat(event: SDL_Event) {
        val hatId = event.jhat.hat
        val dir = event.jhat.value.toInt()
        hats[hatId]?.let {
            val changed = it.direction xor dir
            listOf(SDL_HAT_UP, SDL_HAT_DOWN, SDL_HAT_LEFT, SDL_HAT_RIGHT).forEach { cardinalDir ->
                if(changed and cardinalDir == cardinalDir) {
                    events.addLast(ControllerEvent(ControlType.HAT, hatId.toInt(), cardinalDir, dir and cardinalDir == 0))
                }
            }
            it.direction = dir
        }
    }

    override fun close() {
        if (SDL_JoystickGetAttached(controller) != 0u) {
            writeLog("Closing joystick $controllerId")
            SDL_JoystickClose(controller)
        }
    }

    override fun getDeviceId(): Int {
        return controllerId
    }
    override fun getCurrentId(): SDL_JoystickID {
        return SDL_JoystickGetDeviceInstanceID(controllerId)
    }

    override fun getEvents(): ArrayDeque<ControllerEvent> {
        return events
    }

    companion object {
        fun open(deviceId: Int): Joystick? {
            SDL_JoystickOpen(deviceId)?.let {
                return Joystick(deviceId, it)
            }
            return null
        }
    }
}