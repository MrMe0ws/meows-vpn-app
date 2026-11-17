# Meows для Windows

Папка `windows/` — Compose Multiplatform Desktop (Kotlin/JVM), тот же интерфейс Material 3, что и на Android (палитры, спящий кот и лапка), но с **десктопным макетом в три колонки**: рейка разделов слева (Серверы · Добавить · Настройки; сюда же встанут «Логи» и др.), подписка и серверы в центре, справа — режим, кнопка подключения и выбранный сервер.

## Как устроено
```
Meows.exe (JVM + Compose)  ──stdin/stdout──►  meows-core.exe (Go, Xray-core)
   подписка, серверы, UI, трей                  режим «Системный прокси»: SOCKS5 127.0.0.1:10808 + HTTP 127.0.0.1:10809
   системный прокси Windows  ◄── включает ──┘   режим «Все программы» (TUN): адаптер MeowsTun через wintun.dll
```

## Режимы работы (`vpn/VpnMode.kt`)
| Режим | Что это | Права |
|---|---|---|
| **Все программы (TUN)** — по умолчанию | Ядро создаёт виртуальный адаптер `MeowsTun` (wintun), маршрут `0.0.0.0/0` в него, утечки DNS закрыты фильтрами WFP (`autoSystemWfpBlockLeak: ["dns"]`). Весь трафик ПК идёт по правилам, как на телефоне. | администратор |
| **Системный прокси** | Локальные прокси + запись в реестре; работают браузеры и программы, учитывающие прокси. | не нужны |

- Конфиг TUN: `core/conf.go: buildWinTunConfig` (поверх `buildTunConfig` Android; адаптер `MeowsTun`, шлюз `10.255.0.1/30`, DNS 1.1.1.1/8.8.8.8, свои сокеты ядра привязаны к физическому интерфейсу — `autoOutboundsInterface`). `core/desktop_main.go` выбирает режим по полю `mode` в `core.json`. Только IPv4 в таблице маршрутов (IPv6 не заворачивается — тех. долг).
- **`wintun.dll`** лежит в `windows/resources/windows-x64/` рядом с `meows-core.exe` (официальная сборка 0.14.1 с wintun.net, подпись WireGuard LLC валидна; загрузка — только по разрешению владельца).
- **Права администратора** (`system/Elevation.kt`): при запуске в режиме TUN приложение, если не администратор, перезапускает себя через `ShellExecute runas` (одно окно UAC), новый процесс ждёт закрытия прежнего (`--wait-pid`). Если UAC отклонён — работаем без прав, при «Подключить» спросим снова. Флаги: `--connect` (подключиться после запуска), `--no-elevate`.
- **Автозапуск** (`system/Autostart.kt`): с правами администратора — задача планировщика «Meows» с наивысшими правами (без UAC при входе); без них — ключ HKCU Run (при входе будет окно UAC, если режим TUN).
- Смена режима при подключении — переподключение. Подсказка «Какой режим выбрать?» (`ModeInfoDialog`) — короткие объяснения для пользователя.

- **Ядро** — тот же Go-код (`core/`), собранный под Windows: `core/build-windows.ps1` → `windows/resources/windows-x64/meows-core.exe` (+ geo-файлы). Точка входа `core/desktop_main.go` (режим `tun` или `proxy`); маршрутизация (`routing.go`, `geo.go`, `conf.go`) общая с Android.
- **Закрылось приложение — закрылось ядро:** ядро читает stdin; закрытие stdin = выход.
- **Системный прокси:** `vpn/SystemProxy.kt` (реестр HKCU + WinInet через JNA). Прежние значения запоминаются; при аварийном выходе восстанавливаются при следующем запуске. В режиме «Системный прокси» программы, игнорирующие его, VPN не используют — для них режим «Все программы».
- **Подписка:** `data/Subscription.kt` — как на Android: User-Agent `MeowsVPN/<версия>` с откатом на Happ, `x-hwid` = хеш MachineGuid, `x-device-os: Windows`.
- **Пинг:** `vpn/PingRunner.kt` — режим `measure` ядра.
- **Данные:** `%APPDATA%\Meows` (settings.json, core.json, geo/). Установка: `%LOCALAPPDATA%\Meows` (на пользователя, без администратора).

## Файлы (`windows/src/main/kotlin/app/meows/desktop/`)
```
Main.kt                точка входа: аргументы, один экземпляр, трей, окно
AppModel.kt            состояние и команды (подписка, подключение, пинг, настройки, обновления)
data/                  AppDirs, SettingsStore (JSON), Hwid, Server/ServerParser, Subscription(+Client)
vpn/                   CoreProcess (запуск ядра), SystemProxy, PingRunner, VpnMode
system/                SingleInstance (порт 47831), Protocol (meows:// в HKCU), Autostart (задача планировщика / HKCU Run), Elevation (UAC)
update/                UpdateChecker (GitHub, тег windows-v*, ассет .msi), UpdateController, UpdateState/Version
ui/                    MeowsApp, UpdateDialog, Res, Format
ui/nav/                MeowsRail (NavigationRail: Серверы · Добавить · Настройки · Логи, внизу «О программе»), Page
ui/about/              AboutScreen (версия, ядро, режим, HWID, ссылки), UpdateBlock (проверка/скачивание/установка обновления)
ui/logs/               LogsScreen (фильтры Всё/VPN/Ядро/Подписка, копировать, очистить)
log/                   AppLog — журнал в памяти (~1500 строк), вывод ядра идёт с тегом CORE; ссылку подписки не пишем
ui/theme/TitleBarTint  цвет заголовка окна (DWM, Windows 11) = цвет рейки + 18% основного цвета темы; system/TitleBar.kt
ui/home/               HomeScreen (две панели), ServersPane (центр), ConnectPane (справа), ModePicker, ModeInfoDialog, ConnectButton, StatusText,
                       SubscriptionCard (обновить · пинг · ⋮), ServerRow(+FlagBadge, PingChip), AddDialog
ui/settings/           SettingsScreen, SystemGroup, UpdatesGroup
ui/theme/              MeowsTheme, Palettes (те же 8 палитр и порядок, что на Android)
```

## Возможности (v1.0.0)
- Подписка: вставка/ввод ссылки, **ссылка `meows://add/…`** (протокол регистрируется при первом запуске; из кабинета Windows откроет Meows), обновление, лимит устройств по HWID.
- Подключение/отключение в двух режимах (TUN по умолчанию / системный прокси), список серверов, пинг, темы и палитры (авто/светлая/тёмная, AMOLED), блок рекламы, расширенное «все российские сайты напрямую», кнопки «Кабинет»/«Поддержка».
- **Заголовок окна** подкрашен под тему (`DwmSetWindowAttribute`: цвет, цвет текста, тёмный режим) — окно остаётся системным (снап, resize). На Windows 10 цвет не применится (нужна Windows 11).
- **Логи** и **«О программе»** (идея — Happ): события приложения и ядра, копирование для поддержки; версия, HWID, ссылки, проверка обновлений.
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
**TUN:** запуск с UAC, кнопка «Подключить» → адаптер `MeowsTun` Up, `cloudflare.com/cdn-cgi/trace` и `ipinfo.io` показывают Германию (DE), `api.ip.sb`/`ifconfig.me` — напрямую по правилам; отключение удаляет адаптер, IP возвращается. Не проверено: автозапуск через задачу планировщика, поведение игр/UDP, режим на чужих ПК (VPN-клиенты/антивирусы рядом), IPv6.

**Прокси-режим (первая версия):**
Установка MSI, ярлык на рабочем столе, флаги стран (картинки), добавление подписки через `meows://add/…`, подключение (Cloudflare trace через прокси: DE/Франкфурт, напрямую: TH), отключение возвращает системный прокси, трей (закрытие скрывает, второй запуск показывает окно), автозапуск (ключ реестра включается/убирается), пинг не гонялся.
Заметка: `ifconfig.me` и `ipify.org` идут напрямую **по задумке шаблона** (они в списках roscomvpn `category-ru`/`whitelist`); для проверки IP через VPN использовать `ipinfo.io` или `cloudflare.com/cdn-cgi/trace`.

## Ограничения и план
- TUN: нет исключения приложений и IPv6-маршрутов; задача автозапуска не удаляется при удалении MSI (удаляется выключением переключателя).
- Нет исключения приложений, автообновления гео-файлов (на Android есть `GeoUpdater`), QR-кода.
- Нет подписи кода: Windows SmartScreen предупреждает при первом запуске установщика.
- Эмодзи-флагов в Windows нет: `ServerRow.FlagBadge` показывает **картинки флагов из набора Noto (Android)** — `src/main/resources/flags/<код>.png` (259 шт., ~0,6 МБ), скачаны скриптом `windows/tools/fetch-flags.ps1` (источник: googlefonts/noto-emoji, `third_party/region-flags`, Apache-2.0). Нет картинки — плашка с кодом страны.
- Логика подписки продублирована с Android (`XKStore.java` ↔ `data/*.kt`): общий модуль Kotlin Multiplatform — тех. долг.