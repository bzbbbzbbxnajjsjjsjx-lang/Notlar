package com.vardiya.sludge.domain

/** How the equalizer is doing right now, in the order a person would fix things. */
enum class Health { OFF, WAITING, ACTIVE }

data class Status(val health: Health, val headline: String, val detail: String)

/**
 * The one line the home screen leads with, worked out from what is really true: the switch and the audio sessions
 * players have announced (what actually gets processed).
 */
fun statusOf(enabled: Boolean, sessions: Int): Status = when {
    !enabled -> Status(Health.OFF, "Kapalı", "Ses olduğu gibi çalıyor")
    sessions > 0 -> Status(Health.ACTIVE, "Ses işleniyor", "$sessions çalar bağlı")
    else -> Status(Health.WAITING, "Çalar bekleniyor", "Müzik uygulamanda bir şarkı başlat; harici ekolayzır desteği gerekir")
}
