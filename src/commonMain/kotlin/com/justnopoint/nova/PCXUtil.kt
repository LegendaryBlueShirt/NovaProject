package com.justnopoint.nova

import okio.*
import kotlin.experimental.and

fun loadPcx(path: Path): PCXImage? {
    return try {
        FileSystem.SYSTEM.read(file = path, readerAction = ::loadPcx)
    } catch (e: Exception) {
        showErrorPopup("Error Loading \"$path\"", e.message?:"Unknown Error")
        PCXImage(ByteArray(320*200), UByteArray(768), 320, 200)
    }
}

@Suppress("UNUSED_VARIABLE")
fun loadPcx(buffer: BufferedSource): PCXImage? {
    val id = buffer.readByte()
    if(id.toUInt() != 0x0Au) return null
    val version = buffer.readByte()
    val encoding = buffer.readByte()
    if(encoding.toUInt() != 1u) return null
    val bpp = buffer.readByte()
    if(bpp.toUInt() != 8u) return null
    val xStart = buffer.readShortLe()
    val yStart = buffer.readShortLe()
    val xEnd = buffer.readShortLe()
    val yEnd = buffer.readShortLe()
    val hRes = buffer.readShortLe()
    val vRes = buffer.readShortLe()
    val egaPal = buffer.readByteArray(48)
    val reserved = buffer.readByte()
    val bitPlanes = buffer.readByte()
    val scanlineSize = buffer.readShortLe().toUShort().toInt()
    val palType = buffer.readShortLe()
    val hScreenSize = buffer.readShortLe()
    val vScreenSize = buffer.readShortLe()
    val reserved2 = buffer.readByteArray(54)

    val width = xEnd-xStart+1
    val height = yEnd-yStart+1
    val linePadding = scanlineSize - width

    val imageData = ByteArray(width * height)
    val scanline = ByteArray(scanlineSize)

    val encodingMask = 0xC0.toByte()
    val runMask = 0x3F.toByte()

    var line = 0
    while(line < height) {
        var index = 0
        var runCount: Int
        var runValue: Byte
        while(index < scanlineSize) {
            runValue = buffer.readByte()
            if((runValue and encodingMask) == encodingMask) {
                runCount = (runValue and runMask).toUByte().toInt()
                runValue = buffer.readByte()
            } else {
                runCount = 1
            }
            while(runCount > 0 && index < scanlineSize) {
                scanline[index] = runValue
                runCount--
                index++
            }
        }
        scanline.copyInto(destination = imageData, destinationOffset = line*width, endIndex = width)
        line++
    }
    val palId = buffer.readByte()
    if(palId.toUByte().toInt() != 0x0C) return null
    val palette = buffer.readByteArray(768)
    return PCXImage(imageData, palette.toUByteArray(), width, height)
}

data class PCXImage(val imageData: ByteArray, val paletteData: UByteArray, val width: Int, val height: Int)