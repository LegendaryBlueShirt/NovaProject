package com.justnopoint.nova
// Game var locations
const val p1ScorePointer = 0x1FA250L
const val p1HP = 0x15CE50L
const val p1MaxHP = 0x15CE52L
const val p1HPAlt = 0x15CE58L //Sometimes P1 HP is here instead, I don't know what causes this.
const val p2HP = 0x15E144L
const val p2MaxHP = 0x15E146L
const val p1State = 0x215084L
const val p1Stun = 0x2150C0L
const val p1posX = 0x2150E0L
const val p1posY = 0x2150E4L
const val p2State = 0x220084L
const val p2Stun = 0x2200C0L
const val p2posX = 0x2200E0L
const val p2posY = 0x2200E4L

// Good numbers to have
const val p1StartXPosition = 28160u
const val p2StartXPosition = 53760u
const val groundYPosition = 48640u
const val cornerLeftP1 = 5120u
const val cornerLeftP2 = 12825u
const val cornerRightP1 = 69120u
const val cornerRightP2 = 76800u
