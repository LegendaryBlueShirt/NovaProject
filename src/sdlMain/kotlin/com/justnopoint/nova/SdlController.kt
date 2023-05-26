package com.justnopoint.nova

import SDL.*
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.toKString

class SdlController(private val controllerId: Int, private val controller: CPointer<SDL_GameController>): SdlInput {
    private val buttons = mutableMapOf<UByte, Button>()
    private val axes: Map<UByte, Axis>
    private val hats: Map<UByte, Hat>

    private val events = ArrayDeque<ControllerEvent>()
    init {
        writeLog("Creating controller $controllerId")
        val joy = SDL_GameControllerGetJoystick(controller)
        val nButtons = SDL_JoystickNumButtons(joy)
        writeLog("Found $nButtons buttons")
        buttons[SDL_CONTROLLER_BUTTON_DPAD_UP.toUByte()] = Button(SDL_CONTROLLER_BUTTON_DPAD_UP, false)
        buttons[SDL_CONTROLLER_BUTTON_DPAD_DOWN.toUByte()] = Button(SDL_CONTROLLER_BUTTON_DPAD_DOWN, false)
        buttons[SDL_CONTROLLER_BUTTON_DPAD_LEFT.toUByte()] = Button(SDL_CONTROLLER_BUTTON_DPAD_LEFT, false)
        buttons[SDL_CONTROLLER_BUTTON_DPAD_RIGHT.toUByte()] = Button(SDL_CONTROLLER_BUTTON_DPAD_RIGHT, false)
        //buttons = (0 until nButtons).associate { it.toUByte() to Button(it, false) }
        val nAxes = SDL_JoystickNumAxes(joy)
        writeLog("Found $nAxes axes")
        axes = (0 until nAxes).associate { it.toUByte() to Axis(it, 0) }
        val nHats = SDL_JoystickNumHats(joy)
        writeLog("Found $nHats hats")
        hats = (0 until nHats).associate { it.toUByte() to Hat(it, 0) }
    }

    private val directions = mapOf(
        SDL_CONTROLLER_BUTTON_DPAD_UP.toUByte() to 1,
        SDL_CONTROLLER_BUTTON_DPAD_DOWN.toUByte() to 4,
        SDL_CONTROLLER_BUTTON_DPAD_LEFT.toUByte() to 8,
        SDL_CONTROLLER_BUTTON_DPAD_RIGHT.toUByte() to 2)
    override fun updateControllerButton(event: SDL_Event) {
//        val buttonEvent = event.cbutton
//        if(!buttons.containsKey(buttonEvent.button)) {
//            buttons[buttonEvent.button] = Button(buttonEvent.button.toInt(), buttonEvent.state.toInt() == SDL_RELEASED)
//        }

        val button = event.cbutton.button
        //println("Got event for button $button")
        when (event.type) {
            SDL_CONTROLLERBUTTONUP -> {
                buttons.getOrPut(button) { Button(button.toInt(), true) }.let {
                    if (it.pressed) {
                        it.pressed = false
                        events.addLast(ControllerEvent(ControlType.BUTTON, button.toInt(), directions[button]?:0, true))
                    }
                }
            }

            SDL_CONTROLLERBUTTONDOWN -> {
                buttons.getOrPut(button) { Button(button.toInt(), false) }.let {
                    if (!it.pressed) {
                        it.pressed = true
                        events.addLast(ControllerEvent(ControlType.BUTTON, button.toInt(), directions[button]?:0, false))
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
//        val hatId = event.jhat.hat
//        val dir = event.jhat.value.toInt()
//        hats[hatId]?.let {
//            val changed = it.direction xor dir
//            listOf(SDL_HAT_UP, SDL_HAT_DOWN, SDL_HAT_LEFT, SDL_HAT_RIGHT).forEach { cardinalDir ->
//                if(changed and cardinalDir == cardinalDir) {
//                    events.addLast(ControllerEvent(ControlType.HAT, hatId.toInt(), cardinalDir, dir and cardinalDir == 0))
//                }
//            }
//            it.direction = dir
//        }
    }

    override fun close() {
        if (SDL_GameControllerGetAttached(controller) != 0u) {
            writeLog("Closing controller $controllerId")
            SDL_GameControllerClose(controller)
        }
    }

    override fun getDeviceId(): Int {
        return controllerId
    }
    override fun getCurrentId(): SDL_JoystickID {
        val joy = SDL_GameControllerGetJoystick(controller)
        return SDL_JoystickInstanceID(joy)
    }

    override fun getEvents(): ArrayDeque<ControllerEvent> {
        return events
    }

    override fun getName(): String {
        val myname = SDL_GameControllerName(controller)?.toKString()
        return "$myname $controllerId"
    }

    companion object {
        fun open(deviceId: Int): SdlController? {
            SDL_GameControllerOpen(deviceId)?.let {
                return SdlController(deviceId, it)
            }
            return null
        }
    }
}