# Ядро VPN и Java-слой

## Как работает подключение

1. `XKVpnService` создаёт TUN (`VpnService.Builder`), поднимает foreground-уведомление (тип `specialUse`, subtype `vpn`).
2. Запускает **как процесс** `nativeLibraryDir/libxkcore.so <core.json> <geo-dir> <socket>`. Поэтому `extractNativeLibs=true` и
   `useLegacyPackaging=true` — `.so` должен лежать на диске, а не внутри APK.
3. Дескриптор TUN передаётся ядру через abstract unix-socket (SCM_RIGHTS). Ядро печатает `XKOK` / `XKERR …`.
4. Своё приложение исключено из VPN (`addDisallowedApplication`) — подписка и пинг идут напрямую.

`geoip.dat`/`geosite.dat` лежат в `assets/` **несжатыми** (`noCompress += "dat"`): код читает размер через `openFd`.

## Режим `measure`

`libxkcore.so measure <sub.json> <url> <timeout-ms> <i,j,k>` печатает `XKMS <index> <ms>` для серверов из списка индексов.
`XKStore.pingSome(int[] only, …)` — пинг выбранных серверов (null = все).

## Правила для легаси-слоя `XK*.java`

- Менять точечно, не рефакторить «заодно». Новую логику — в Kotlin, по фичам.
- Публичные константы настроек — `XKStore.P_*`. Новые ключи Kotlin-кода — рядом с фичей (пример: `UpdateController`).
- Не переименовывать пакет/классы без правки `AndroidManifest.xml` и `proguard-rules.pro`.

## Ядро

Исходники обёртки — `core/*.go`. Сборка: см. `docs/build.md` (раздел «Ядро»). Версия Xray-core зафиксирована коммитом `a52db8f6ed19`.
Все ABI (`arm64-v8a`, `armeabi-v7a`, `x86`, `x86_64`), сборка — `core/build-android.ps1`, см. `docs/build.md`.
