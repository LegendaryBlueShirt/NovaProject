package com.justnopoint.nova

import SDL.SDL_Event
import SDL.SDL_JoystickID

interface Input {
    fun updateControllerButton(event: SDL_Event)
    fun updateControllerAxis(event: SDL_Event)
    fun updateControllerHat(event: SDL_Event)
    fun close()
    fun getDeviceId(): Int
    fun getCurrentId(): SDL_JoystickID
    fun getEvents(): ArrayDeque<ControllerEvent>
}

data class Button(val buttonId: Int, var pressed: Boolean)
data class Hat(val hatId: Int, var direction: Int)
data class Axis(val axisId: Int, var direction: Int)

data class ControllerEvent(val type: ControlType, val id: Int, val direction: Int, val release: Boolean)