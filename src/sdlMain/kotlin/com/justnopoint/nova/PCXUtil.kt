package com.justnopoint.nova

import SDL.*
import kotlinx.cinterop.*
import okio.Path.Companion.toPath

fun surfaceFromPcx(path: String): CPointer<SDL_Surface>? = memScoped {
    loadPcx(path.toPath())?.let { pcx ->
        return surfaceFromPcx(pcx)
    }
    return null
}

fun surfaceFromPcx(pcx: PCXImage): CPointer<SDL_Surface>? = memScoped {
    val newSurface = SDL_CreateRGBSurface(0, pcx.width, pcx.height, 8, 0u, 0u, 0u, 0u)
    val pcxPalette = allocArray<SDL_Color>(256)
    for(n in 0 until 256) {
        pcxPalette[n].r = pcx.paletteData[n*3]
        pcxPalette[n].g = pcx.paletteData[n*3+1]
        pcxPalette[n].b = pcx.paletteData[n*3+2]
        pcxPalette[n].a = 0xFFu
    }
    val format = newSurface?.pointed?.format
    val palette = format?.pointed?.palette
    SDL_SetPaletteColors(palette, pcxPalette, 0, 256)
    SDL_memcpy(newSurface?.pointed?.pixels, pcx.imageData.refTo(0), pcx.imageData.size.toULong())
    SDL_SetColorKey(newSurface, SDL_TRUE.toInt(), 0)
    return newSurface
}