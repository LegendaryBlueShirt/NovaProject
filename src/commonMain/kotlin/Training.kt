fun getTrainingModeInput(code: Int) {
    when(code) {
        VIRT_F1 -> resetLeftSide()
        VIRT_F2 -> resetCenter()
        VIRT_F3 -> resetRightSide()
    }
}

fun resetLeftSide() {
    resetCharacters(hp = true, stun = true, state = true)
    writeMemoryInt(p1posY, groundYPosition)
    writeMemoryInt(p2posY, groundYPosition)
    writeMemoryInt(p1posX, cornerLeftP1)
    writeMemoryInt(p2posX, cornerLeftP2)
}

fun resetCenter() {
    resetCharacters(hp = true, stun = true, state = true)
    writeMemoryInt(p1posY, groundYPosition)
    writeMemoryInt(p2posY, groundYPosition)
    writeMemoryInt(p1posX, p1StartXPosition)
    writeMemoryInt(p2posX, p2StartXPosition)
}

fun resetRightSide() {
    resetCharacters(hp = true, stun = true, state = true)
    writeMemoryInt(p1posY, groundYPosition)
    writeMemoryInt(p2posY, groundYPosition)
    writeMemoryInt(p1posX, cornerRightP1)
    writeMemoryInt(p2posX, cornerRightP2)
}

fun resetCharacters(hp: Boolean, stun: Boolean, state: Boolean) {
    if(hp) {
        var maxHp = readMemoryShort(p1MaxHP)
        writeMemoryShort(p1HP, maxHp)
        maxHp = readMemoryShort(p2MaxHP)
        writeMemoryShort(p2HP, maxHp)
    }
    if(stun) {
        writeMemoryInt(p1Stun, 0u)
        writeMemoryInt(p2Stun, 0u)
    }
    if(state) {
        writeMemoryByte(p1State, 11u)
        writeMemoryByte(p2State, 11u)
    }
}

expect fun readMemoryShort(address: Long): UShort
expect fun readMemoryInt(address: Long): UInt
expect fun writeMemoryInt(address: Long, value: UInt)
expect fun writeMemoryShort(address: Long, value: UShort)
expect fun writeMemoryByte(address: Long, value: UByte)