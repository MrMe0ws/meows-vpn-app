# Какие бывают сборки Meows (чтобы не путаться)

Три разных продукта из одного репозитория. Перед любой правкой понять, к какому из них она относится.

| | Android **GitHub (direct)** | Android **Google Play (play)** | **Windows** |
|---|---|---|---|
| Папка | `android/`, flavor `direct` | `android/`, flavor `play` | `windows/` |
| Артефакт | `MeowsVPN-X.Y.Z.apk` | `MeowsVPN-play.aab` | `.msi`, `Meows-Setup-X.Y.Z.exe`, `.zip` |
| Сборка | `.\android\build.ps1` | `.\android\build.ps1 -Play` | `cd windows; .\build.ps1` |
| Откуда ставят | GitHub Releases `android-vX.Y.Z`, ссылка `android-latest/Meows.apk` из кабинета/сайта | Google Play | Releases `windows-vX.Y.Z`, `windows-latest/Meows-Setup.exe` |
| Обновление | **само**, через GitHub (`update/`), разрешение `REQUEST_INSTALL_PACKAGES` | только Google Play; кода обновлений нет | само, через GitHub (MSI) |
| Кабинет, «Продлить», ссылки на оплату, текст `announce` | **есть** | **нет** (политика платежей Play) | есть |
| «Поддержка» (бот) | есть | есть | есть |
| `QUERY_ALL_PACKAGES` | есть | **нет**, вместо него `<queries>` на запускаемые приложения (Play просит обоснование, наш сценарий в разрешённый список не входит) |
| `targetSdk` | 35 | **36** (требование Play с 31.08.2026) | — |
| Проверено на устройстве | Pixel 10 (arm64) | Pixel 10 (arm64), через `bundletool` | Windows 11 |
| Подпись | `C:\MeowsTools\meows.keystore` (alias `meows`) | тот же ключ (как ключ подписи приложения в Play) | — |

## Как отличать в коде
- Android: `Edition.isPlay` (`android/app/src/main/java/app/meows/vpn/Edition.kt`, из `BuildConfig.PLAY`). Всё, что связано с оплатой или самообновлением, прячется за `!Edition.isPlay`. Манифест Play-варианта — `android/app/src/play/AndroidManifest.xml` (убирает `REQUEST_INSTALL_PACKAGES` и FileProvider).
- Любая **новая** ссылка/кнопка, ведущая в кабинет, оплату, бота продаж или к скачиванию APK — по умолчанию прячется в Play-варианте.
- Версия (`versionCode`/`versionName`) общая у обоих Android-вариантов, `versionCode` только растёт. Выпуск обоих — вместе (GitHub-релиз и новая версия в Play).

## Чего держаться
- Не называть Play-вариант «Android-приложением вообще»: говорить «Play-версия» / «GitHub-APK».
- Windows-версия не связана ни с Play, ни с `Edition`.
- Всё про консоль, декларации, тексты карточки и правила Play — `docs/play.md`, `docs/play-listing.md`; картинки — `docs/play-assets/`.

## Данные Google Play (заполняется по ходу)
- Аккаунт разработчика: личный (создан в 2026, поэтому для продакшена нужны 12 тестировщиков на 14 дней).
- Приложение: имя «Meows», пакет `app.meows.vpn`, язык по умолчанию — русский, бесплатное.
- Политика конфиденциальности: https://mrme0ws.github.io/meows-vpn-app/privacy/
- Статус: см. `docs/play.md` → «Статус публикации».

## Убрано из Play-версии «на время модерации» (вернуть после релиза — решение владельца)
Кнопки «Кабинет»/«Продлить», пункты «Личный кабинет», «Открыть кабинет» при лимите устройств, ссылки на кабинет в уведомлениях, текст `announce`, подсказки про бота/кабинет в пустом экране и листе ссылки. Всё за `Edition.isPlay` — вернуть = убрать проверки в этих местах (`grep -rn "Edition.isPlay"`).
Не возвращать никогда: автообновление и `REQUEST_INSTALL_PACKAGES`. Правила платежей Play действуют и после публикации; решение о возврате — за владельцем, на его риск.
