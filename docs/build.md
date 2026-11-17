# Сборка

Android Studio не нужна.

```powershell
.\android\build.ps1            # release (подписанный, R8) -> android\dist\MeowsVPN-release.apk
.\android\build.ps1 -Debug     # debug                     -> android\dist\MeowsVPN-debug.apk (applicationId с суффиксом .debug)
```

## Инструменты (вне проекта и OneDrive)
`C:\MeowsTools`: `jdk17`, `gradle-8.14.3`, `android-sdk` (platform 36, build-tools 36.0.0), `meows.keystore`.
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
```
git clone https://github.com/XTLS/Xray-core ; git checkout a52db8f6ed19
скопировать core\*.go (кроме xk_test.go) в Xray-core\libxk\
$env:CGO_ENABLED=0; $env:GOOS="android"; $env:GOARCH="arm64"
go build -trimpath -ldflags "-s -w -checklinkname=0" -o libxkcore.so ./libxk
```
Результат — `android/app/src/main/jniLibs/<abi>/libxkcore.so`. Для armeabi-v7a/x86/x86_64 нужен Android NDK (cgo): см. `roadmap.md`.
