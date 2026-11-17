# Платформы и устройство репозитория

Один репозиторий `meows-vpn-app` — несколько клиентов. Так проще держать общие документы, ядро и релизы вместе.

```
meows-vpn-app/
  AGENTS.md  README.md  docs/     общее: правила, дизайн, решения, roadmap
  core/                            Go-обёртка ядра Xray (общая для платформ)
  android/                         Android-приложение (Kotlin, Compose, Material 3 Expressive) — сделано
  windows/                         Windows (Compose Desktop + ядро Xray) — первая версия, см. docs/windows/overview.md
  ios/                             iOS (SwiftUI + NetworkExtension) — пока не начато
```

- У каждой платформы свои сборка, релизы (`android-vX.Y.Z`, `ios-vX.Y.Z`) и **свой дизайн-канон**: Android — Material You (`docs/design/`), iOS — Human Interface Guidelines (папку добавить при старте).
- Общее: формат подписки, названия, бренд, тексты, логика серверов/пинга (на уровне идей; код не шарится).
- iOS на Windows не собрать: нужен Mac с Xcode. Ядро для iOS — другая сборка Xray (gomobile/XCFramework); в `core/` добавить отдельную обёртку.
- Документы, относящиеся только к Android (`architecture/structure.md`, `build.md`, `release.md`, `design/*`), при появлении iOS переехать в `docs/android/`.