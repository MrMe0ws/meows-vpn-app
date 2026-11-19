package app.meows.desktop.vpn

import app.meows.desktop.i18n.tr

/** Режим работы VPN на ПК. [key] хранится в настройках и передаётся ядру. */
enum class VpnMode(val key: String, private val titleRu: String, private val hintRu: String) {
    Tun("tun", "Все программы", "Как на телефоне: весь интернет компьютера. Нужны права администратора"),
    Proxy("proxy", "Системный прокси", "Только браузеры и программы с поддержкой прокси. Без администратора");

    val title: String get() = tr(titleRu)
    val hint: String get() = tr(hintRu)

    companion object { fun of(key: String?) = entries.firstOrNull { it.key == key } ?: Tun }
}
