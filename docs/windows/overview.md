# Meows для Windows

Папка `windows/` — Compose Multiplatform Desktop (Kotlin/JVM), тот же интерфейс Material 3, что и на Android (палитры, спящий кот и лапка, нижняя плавающая панель).

## Как устроено
```
Meows.exe (JVM + Compose)  ──stdin/stdout──►  meows-core.exe (Go, Xray-core)
   подписка, серверы, UI, трей                  локальный SOCKS5 127.0.0.1:10808 + HTTP 127.0.0.1:10809
   системный прокси Windows  ◄── включает/возвращает ──┘
```
- **Ядро** — тот же Go-код (`core/`), собранный под Windows: `core/build-windows.ps1` → `windows/resources/windows-x64/meows-core.exe` (+ geo-файлы). Точка входа `core/desktop_main.go` (локальные прокси вместо TUN); маршрутизация (`routing.go`, `geo.go`, `conf.go`) общая с Android.
- **Закрылось приложение — закрылось ядро:** ядро читает stdin; закрытие stdin = выход.
- **Системный прокси:** `vpn/SystemProxy.kt` (реестр HKCU + WinInet через JNA). Прежние значения запоминаются; при аварийном выходе восстанавливаются при следующем запуске. Приложения, игнорирующие системный прокси, VPN не используют (TUN-режима пока нет).
- **Подписка:** `data/Subscription.kt` — как на Android: User-Agent `MeowsVPN/<версия>` с откатом на Happ, `x-hwid` = хеш MachineGuid, `x-device-os: Windows`.
- **Пинг:** `vpn/PingRunner.kt` — режим `measure` ядра.
- **Данные:** `%APPDATA%\Meows` (settings.json, core.json, geo/). Установка: `%LOCALAPPDATA%\Meows` (на пользователя, без администратора).

## Файлы (`windows/src/main/kotlin/app/meows/desktop/`)
```
Main.kt                точка входа: аргументы, один экземпляр, трей, окно
AppModel.kt            состояние и команды (подписка, подключение, пинг, настройки, обновления)
data/                  AppDirs, SettingsStore (JSON), Hwid, Server/ServerParser, Subscription(+Client)
vpn/                   CoreProcess (запуск ядра), SystemProxy, PingRunner
system/                SingleInstance (порт 47831), Protocol (meows:// в HKCU), Autostart (HKCU Run)
update/                UpdateChecker (GitHub, тег windows-v*, ассет .msi), UpdateController, UpdateState/Version
ui/                    MeowsApp, UpdateDialog, Res, Format
ui/home/               HomeScreen, ConnectButton, StatusText, SubscriptionCard, ServerRow(+FlagBadge), HomeToolbar, AddDialog
ui/settings/           SettingsScreen, SystemGroup, UpdatesGroup
ui/theme/              MeowsTheme, Palettes (те же 8 палитр и порядок, что на Android)
```

## Возможности (v1.0.0)
- Подписка: вставка/ввод ссылки, **ссылка `meows://add/…`** (протокол регистрируется при первом запуске; из кабинета Windows откроет Meows), обновление, лимит устройств по HWID.
- Подключение/отключение через системный прокси, список серверов, пинг, темы и палитры (авто/светлая/тёмная, AMOLED), блок рекламы, расширенное «все российские сайты напрямую», кнопки «Кабинет»/«Поддержка».
- **Трей:** иконка (открыть · подключить/отключить · выход); закрытие окна сворачивает в трей (настройка).
- **Автозапуск** с Windows (в трей, `--minimized`) и **подключение при запуске** — настройки «Система».
- **Один экземпляр:** повторный запуск (ярлык, ссылка) показывает окно первого и передаёт ссылку.
- **Автообновление:** проверка GitHub при запуске (если включено, не чаще раза в 12 ч) и вручную; диалог «Позже / Скачать → Установить»; MSI ставится тихо (`msiexec /passive`), приложение закрывается и перезапускается само.
- **Установщик:** MSI на пользователя (ярлык на рабочем столе и в «Пуск»), обновление поверх по постоянному `upgradeUuid`.

## Сборка и релиз
- `cd windows; .\build.ps1` → `windows\dist\Meows-<версия>.msi` и `Meows-win-x64-<версия>.zip` (портативная папка). Нужен JDK 17 из `C:\MeowsTools` и собранное ядро.
- **Обёртка для WiX:** встроенный в Compose WiX 3.11 падает на ICE-валидации (`light.exe`, код 311) без доступа к службе Windows Installer. `build.ps1` подставляет `windows/tools/LightShim.cs` (добавляет `-sval` только при линковке, оригинал — `light-real.exe` + его `.config`) и запускает `packageMsi -x unzipWix`.
- Версия: `version` и `packageVersion` в `windows/build.gradle.kts` (формат X.Y.Z, MSI допускает major ≤ 255). Приложение читает её из `jpackage.app-version`.
- Релиз: тег **`windows-vX.Y.Z`**, в ассетах `.msi` (его качает обновлялка) и `.zip`. Команда — `docs/release.md`.

## Проверено на реальном ПК (2026-11-17)
Установка MSI, ярлык на рабочем столе, добавление подписки через `meows://add/…`, подключение (Cloudflare trace через прокси: DE/Франкфурт, напрямую: TH), отключение возвращает системный прокси, трей (закрытие скрывает, второй запуск показывает окно), автозапуск (ключ реестра включается/убирается), пинг не гонялся.
Заметка: `ifconfig.me` и `ipify.org` идут напрямую **по задумке шаблона** (они в списках roscomvpn `category-ru`/`whitelist`); для проверки IP через VPN использовать `ipinfo.io` или `cloudflare.com/cdn-cgi/trace`.

## Ограничения и план
- Нет TUN-режима (Wintun, права администратора): часть программ игнорирует системный прокси.
- Нет исключения приложений, автообновления гео-файлов (на Android есть `GeoUpdater`), QR-кода.
- Нет подписи кода: Windows SmartScreen предупреждает при первом запуске установщика.
- Эмодзи-флагов в Windows нет — показывается плашка с кодом страны (`FlagBadge`); картинки флагов — идея.
- Логика подписки продублирована с Android (`XKStore.java` ↔ `data/*.kt`): общий модуль Kotlin Multiplatform — тех. долг.