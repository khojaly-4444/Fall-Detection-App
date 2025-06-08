package com.falldetect.falldetection.models

data class FallEvent(
    val fallType: String = "",
    val date: String = "",
    val time: String = "",
    val impactIntensity: String = ""
)