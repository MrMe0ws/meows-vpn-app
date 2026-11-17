package app.meows.desktop.vpn

/** Режим работы VPN на ПК. [key] хранится в настройках и передаётся ядру. */
enum class VpnMode(val key: String, val title: String, val hint: String) {
    Tun("tun", "Все программы", "Как на телефоне: весь интернет компьютера. Нужны права администратора"),
    Proxy("proxy", "Системный прокси", "Только браузеры и программы с поддержкой прокси. Без администратора");

    companion object { fun of(key: String?) = entries.firstOrNull { it.key == key } ?: Tun }
}
