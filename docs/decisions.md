# Журнал решений

Формат: дата — решение — почему. Новое — сверху.

- **2026-11-17 — настройки: свёрнутые разделы + страницы.** Список рос; по канонам M3 — прогрессивное раскрытие. Роутер `SettingsPage`.
- **2026-11-17 — всё главное на главной, без нижней навигации.** Пользователь не хочет ходить на «Серверы»; два места не оправдывают NavigationBar.
- **2026-11-17 — спящий кот при отключении** (`sleep_cat.webp`, фон вырезан), логотип при подключении; «Zzz» анимацией в Compose.
- **2026-11-17 — QR через zxing-android-embedded**, а не ML Kit/Google code scanner: работает без Google Play Services.
- **2026-11-17 — автообновление через GitHub Releases** (APK вне Play). Установка через FileProvider + системный установщик.
- **2026-11-17 — `.dat` без сжатия в APK** (`noCompress`): иначе `openFd` падает («file can not be opened as a file descriptor»).
- **2026-11-17 — сборка из зеркала `C:\MeowsBuild\src`:** Gradle не принимает кириллицу в пути проекта.
- **material3 1.5.0-alpha04:** в 1.4.0 stable Expressive API скрыты (internal), 1.5.0-rc требует AGP 9.1 и compileSdk 37 — не готовы мигрировать.
- **Ядро arm64:** оставлен исходный `libxkcore.so` из Breeze; пересборка из Xray-core `a52db8f6ed19` + `core/` даёт тот же размер. Остальные ABI требуют cgo → NDK.
- **FGS type `specialUse` (subtype `vpn`)** для VpnService на targetSdk 34+.
- **Основа — Breeze VPN:** логику подписок/ядра оставили (рабочая), UI переписали на Compose + Material 3 Expressive.
