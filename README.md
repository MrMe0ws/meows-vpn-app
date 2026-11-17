# Мяус VPN

VPN-клиент по ссылке подписки Remnawave (Xray JSON) для сервиса Мяус VPN.

| Платформа | Папка | Статус |
|---|---|---|
| Android (Kotlin, Jetpack Compose, Material 3 Expressive) | `android/` | готово, релизы — вкладка Releases |
| iOS | `ios/` | планируется |

Android-сборка (без Android Studio): `cd android; .\build.ps1` → `android\dist\MeowsVPN-release.apk`.

**Для разработчиков и агентов — `AGENTS.md` и папка `docs/`** (архитектура, правила кода, дизайн Material You, сборка, релизы, roadmap).

Основа — исходники Breeze VPN (логика подписок, VpnService, ядро Xray); интерфейс написан заново.