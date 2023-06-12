package com.justnopoint.nova

fun generateFlatColor(r: UByte, g: UByte, b: UByte): UByteArray {
    val result = UByteArray(48)
    for(c in 0 until 16) {
        val red = (r * ((17+c).toUInt()) / 32u) shr 2
        val green = (g * ((17+c).toUInt()) / 32u) shr 2
        val blue = (b * ((17+c).toUInt()) / 32u) shr 2
        result[c*3] = red.toUByte()
        result[c*3+1] = green.toUByte()
        result[c*3+2] = blue.toUByte()
    }
    return result
}

fun generateMetallicColor(r: UByte, g: UByte, b: UByte): UByteArray {
    val result = UByteArray(48)
    for(c in 0 until 16) {
        val red = (r * ((17+c).toUInt()) / 32u) shr 2
        val green = (g * ((17+c).toUInt()) / 32u) shr 2
        val blue = (b * ((17+c).toUInt()) / 32u) shr 2
        result[c*3] = red.toUByte()
        result[c*3+1] = green.toUByte()
        result[c*3+2] = blue.toUByte()
    }
    return result
}