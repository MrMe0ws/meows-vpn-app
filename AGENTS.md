# Meows — инструкция для агентов (репозиторий `meows-vpn-app`)

Монорепо клиентов VPN по ссылке подписки Remnawave (`android/` — готово, `windows/` — первая версия, `ios/` — в планах, см. `docs/platforms.md`). Android: Kotlin + Jetpack Compose, Material 3 Expressive («Material You»).
Основа — Breeze VPN (логика на Java + ядро Xray). **Читай этот файл первым, дальше — только нужные страницы из `docs/`.**

## Золотые правила

1. **Маленькие файлы.** Никаких «больших» файлов. Один файл = одна смысловая единица (один экран, одна страница настроек,
   один компонент, один класс). Ориентир: Kotlin-файл до ~150 строк; если больше — делить. Подробно: `docs/conventions/files-and-code.md`.
2. **Папки по смыслу.** `ui/home`, `ui/settings/pages`, `ui/components`, `update`, … Новая фича = своя папка/файл, а не дописывание в чужой.
3. **Material You — закон.** Интерфейс строим по канонам Google (Material 3 Expressive): цветовые роли, динамические цвета,
   стандартные компоненты, мотион, правила раскладки. Подробно: `docs/design/`. **Если пользователь просит что-то,
   противоречащее Material You — сначала скажи об этом и предложи вариант в духе гайдлайнов**, решает пользователь.
4. **Документация живёт вместе с кодом.** Меняешь поведение или структуру — обнови соответствующий файл в `docs/` в том же изменении.
5. **Язык интерфейса — русский**, тон — `docs/design/copy-and-tone.md`. Название приложения: **«Meows»** (латиницей, без «VPN»). «Мяус VPN» — название сервиса/магазина.
6. **Не ломай то, что работает:** слой `XK*.java` и ядро (`jniLibs/*/libxkcore.so`) — легаси-слой, меняй точечно (`docs/architecture/vpn-core.md`).
7. **Скажи прямо, чего не проверил.** Сборка ≠ проверено на устройстве. Так и пиши.

## Карта документации

| Нужно… | Читай |
|---|---|
| Понять устройство приложения | `docs/architecture/overview.md`, `docs/architecture/structure.md` |
| Ядро, VpnService, Java-слой | `docs/architecture/vpn-core.md` |
| Правила кода, размер файлов, имена | `docs/conventions/files-and-code.md` |
| Дизайн: принципы, цвет, раскладка, UX, тексты | `docs/design/material-you.md`, `color-and-theme.md`, `layout-and-ux.md`, `copy-and-tone.md` |
| Как работает конкретная фича | `docs/features/*.md` |
| Собрать, подписать, выпустить релиз | `docs/build.md`, `docs/release.md` |
| Язык интерфейса (ru/en/авто), как добавлять строки | `docs/features/localization.md` |
| Что делать дальше | `docs/roadmap.md` |
| Устройство репозитория, iOS | `docs/platforms.md` |
| **Какие бывают сборки** (GitHub-APK / Play / Windows) — читать перед правками | `docs/editions.md` |
| Google Play (AAB, flavor `play`, консоль, политика) | `docs/play.md`, `docs/play-listing.md` |
| Windows-версия | `docs/windows/overview.md` |
| Что за магазин и как с ним связано приложение | `docs/business-context.md` |
| Почему сделано именно так | `docs/decisions.md` |

## Быстрые факты

- **Три продукта:** Android GitHub-APK (`direct`), Android Google Play (`play`, AAB, без оплаты и самообновления), Windows — различия в `docs/editions.md`.
- Пакет `app.meows.vpn`, minSdk 24, targetSdk 35 (у Play-варианта 36), compileSdk 36. Сборка: `.\android\build.ps1` (см. `docs/build.md`).
- APK универсальная: `arm64-v8a`, `armeabi-v7a`, `x86_64`, `x86` (ядро: `core/build-android.ps1`). На устройстве проверен только arm64.
- Репозиторий проекта указан в одном месте: `android/.../update/AppLinks.kt`. Релизы Android — теги `android-vX.Y.Z`.
- Проект лежит в папке с кириллицей — Gradle собирает из зеркала `C:\MeowsBuild\src` (скрипт делает сам).
