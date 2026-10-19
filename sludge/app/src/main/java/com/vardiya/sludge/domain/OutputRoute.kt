package com.vardiya.sludge.domain

enum class OutputKind { UNKNOWN, SPEAKER, WIRED, BLUETOOTH }

/** Where the sound is going right now: the phone speaker, a wired headset or a Bluetooth device (with its names). */
data class OutputRoute(val kind: OutputKind = OutputKind.UNKNOWN, val names: List<String> = emptyList()) {
    companion object {
        val UNKNOWN = OutputRoute()
    }
}

/** The output in words, for the screen. */
fun OutputRoute.label(): String = when (kind) {
    OutputKind.UNKNOWN -> "bilinmiyor"
    OutputKind.SPEAKER -> "telefon hoparlörü"
    OutputKind.WIRED -> "kablolu kulaklık"
    OutputKind.BLUETOOTH -> names.firstOrNull { it.isNotBlank() } ?: "Bluetooth cihazı"
}

// "JBL Tune 520BT", "JBL TUNE520BT" and "JBL T520BT" are the same headphone: compare letters and digits only.
private fun squash(text: String) = text.lowercase().filter { it.isLetterOrDigit() }

/**
 * Whether this profile's headphone is what the sound is going to. A Bluetooth output whose name cannot be read
 * counts as a match (nothing says otherwise); the speaker, wired headsets and other Bluetooth devices do not.
 */
fun HeadphoneProfile.appliesTo(route: OutputRoute): Boolean = when (route.kind) {
    OutputKind.UNKNOWN -> true
    OutputKind.BLUETOOTH -> route.names.none { it.isNotBlank() } ||
        route.names.any { name -> (listOf(this.name) + aliases).any { squash(name).contains(squash(it)) } }
    OutputKind.SPEAKER, OutputKind.WIRED -> false
}

/** Whether the correction really shapes the sound now: the right headphone is connected, or the person forced it. */
fun EqSettings.correctionActive(route: OutputRoute): Boolean =
    Headphones.byId(headphoneId)?.let { headphoneAlways || it.appliesTo(route) } ?: false

/** What the equalizer is actually set to: the chosen curve through the headphone correction while it is active. */
fun EqSettings.effectiveBands(route: OutputRoute = OutputRoute.UNKNOWN): List<Float> =
    if (correctionActive(route)) Headphones.byId(headphoneId)!!.applyTo(bands) else bands
