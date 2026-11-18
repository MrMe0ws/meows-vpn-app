# Сборка

Android Studio не нужна.

```powershell
.\android\build.ps1            # release (подписанный, R8) -> android\dist\MeowsVPN-release.apk
.\android\build.ps1 -Debug     # debug                     -> android\dist\MeowsVPN-debug.apk (applicationId с суффиксом .debug)
.\android\build.ps1 -Play      # Google Play (flavor play)  -> android\dist\MeowsVPN-play.aab, см. docs/play.md
```

## Инструменты (вне проекта и OneDrive)
`C:\MeowsTools`: `jdk17`, `gradle-8.14.3`, `android-sdk` (platform 36, build-tools 36.0.0, NDK 29.0.14206865), `meows.keystore`.
Если инструментов нет — скачать JDK 17 (Temurin), Gradle 8.14.3, Android cmdline-tools, затем
`sdkmanager "platform-tools" "platforms;android-36" "build-tools;36.0.0"` и принять лицензии.

## Почему зеркало
Gradle/AGP отказываются работать в пути с кириллицей («Рабочий стол»). `android/build.ps1` зеркалит проект в `C:\MeowsBuild\src`
(robocopy), собирает там, результат — в `C:\MeowsBuild\out`, готовый APK копируется в `dist\`. Правь файлы только в проекте, не в зеркале.

## Подпись
Keystore `C:\MeowsTools\meows.keystore`, alias `meows`; пароль — в `C:\MeowsTools\keypass.txt` (или `$env:MEOWS_KEYPASS`). Ни ключ, ни пароль **не коммитим**.
Ключ нельзя терять и менять: от него зависит возможность обновления APK поверх (в т.ч. автообновление). Хранить копию вне ПК.

## Версии (подобраны вручную — см. `decisions.md`)
AGP 8.11.1 · Kotlin 2.2.0 · Compose BOM 2025.10.00 · **material3 1.5.0-alpha04** (Expressive API) · material-kolor 3.0.1 · zxing-android-embedded 4.3.0.

## Ядро (Xray)
Универсальная APK: `arm64-v8a`, `armeabi-v7a`, `x86_64`, `x86` (`ndk.abiFilters` в `app/build.gradle.kts`). Ядро — Go-бинарник `libxkcore.so` из Xray-core `a52db8f6ed19` + обёртка `core/`.

Пересборка всех ABI: `.\core\build-android.ps1` (нужны Go, git и Android NDK: `sdkmanager "ndk;29.0.14206865"`).
- `arm64-v8a` — чистый Go (`CGO_ENABLED=0`), как исходное ядро Breeze.
- `armeabi-v7a`, `x86`, `x86_64` — cgo через NDK clang, `-buildmode=pie`. Из обёртки убираются `main.go`/`measure_cgo.go` (c-shared) и тег `!cgo`.
- Флаг `-checklinkname=0` нужен из-за `wlynxg/anet`.

Проверено на устройстве: **только arm64** (Pixel 10 — 64-битный). Остальные ABI собраны и проверены по ELF-заголовкам (тип PIE, архитектура, загрузчик), но не запускались.
## Windows
Ядро: `.\core\build-windows.ps1` (чистый Go, `GOOS=windows`). Приложение: `cd windows; .\build.ps1` → `windows\dist\Meows-win-x64-<версия>.zip`. Подробно — `docs/windows/overview.md`.