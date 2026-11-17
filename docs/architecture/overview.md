# Архитектура: обзор

```
┌──────────────── ui/ (Compose, Material 3 Expressive) ────────────────┐
│ MeowsApp → HomeScreen | SettingsScreen → pages/*   UpdateDialog       │
└──────────────┬────────────────────────────────────────────────────────┘
               │ состояние (Compose State) и команды
        MeowsViewModel ─────────── update/UpdateController
               │
   ┌───────────┴───────────┐
   XKStore (Java)          XKVpnService (Java, VpnService)
   подписка, серверы,      поднимает TUN, запускает ядро процессом
   пинг, prefs             libxkcore.so (Xray + tun2socks)
```

## Слои

| Слой | Где | Роль |
|---|---|---|
| UI | `ui/**` | Только отрисовка и события. Никакой бизнес-логики и сети. |
| ViewModel | `MeowsViewModel.kt` | Склеивает UI с `XKStore`/`XKVpnService`, держит Compose-состояние. Тонкий. |
| Обновления | `update/**` | Проверка GitHub-релизов, загрузка и установка APK. Независимый модуль. |
| Данные/VPN (легаси) | `XK*.java` | Подписка, пинг, VPN-сервис, плитка, напоминания. Менять точечно. |
| Ядро | `jniLibs/<abi>/libxkcore.so` | Go-бинарник Xray; код обёртки в `core/`. |

## Потоки

- **Подключение:** кот-кнопка → `MainActivity.toggle()` (разрешение VPN) → `XKVpnService.start` → ядро. Состояние идёт обратно через `XKVpnService.Listener` → `MeowsViewModel.vpnState`.
- **Подписка:** `MeowsViewModel.addSubscription/refresh` → `XKStore.fetch` (HTTP, заголовок `User-Agent: Happ/…`, HWID) → prefs → `rev++` → UI перерисовывается.
- **Пинг:** `XKStore.pingAll/pingSome` → процесс ядра `measure` или TCP → `XKStore.pings` → `rev++`.
- **Обновление приложения:** `UpdateController` → GitHub API → `UpdateInstaller` → системный установщик.

## Реактивность

`MeowsViewModel.rev` — счётчик «что-то изменилось». Геттеры VM (`servers`, `selected`, `pingOf`, `flag`, `pref`) читают `rev`,
поэтому Compose перерисовывает всё, что от них зависит. Новая настройка = запись в prefs через `setFlag/setPref` (они делают `bump()`).
