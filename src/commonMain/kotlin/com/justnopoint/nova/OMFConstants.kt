package com.justnopoint.nova
// Game var locations
val p1ScorePointer = 0x1FA250L
val p1HP = 0x15CE50L
val p1MaxHP = 0x15CE52L
val p1HPAlt = 0x15CE58L //Sometimes P1 HP is here instead, I don't know what causes this.
val p2HP = 0x15E144L
val p2MaxHP = 0x15E146L
val p1State = 0x215084L
val p1Stun = 0x2150C0L
val p1posX = 0x2150E0L
val p1posY = 0x2150E4L
val p2State = 0x220084L
val p2Stun = 0x2200C0L
val p2posX = 0x2200E0L
val p2posY = 0x2200E4L

// Good numbers to have
val p1StartXPosition = 28160u
val p2StartXPosition = 53760u
val groundYPosition = 48640u
val cornerLeftP1 = 5120u
val cornerLeftP2 = 12825u
val cornerRightP1 = 69120u
val cornerRightP2 = 76800u
