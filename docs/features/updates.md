# Обновления приложения

APK не из Google Play, поэтому обновляем сами через **GitHub Releases** репозитория из `update/AppLinks.kt`.

## Поток
1. **Проверка.** `UpdateChecker.latest()` → `GET /repos/{owner}/{repo}/releases?per_page=20`. Берётся самый свежий не-черновик и не-pre-release с тегом `android-vX.Y.Z` (репозиторий общий для платформ) и первый ассет `*.apk`.
   Сравнение — `Version.isNewer` (числовые сегменты, суффикс после `-` игнорируется).
2. **Когда.** Автоматически при запуске, **если включено** (prefs `updateCheck`, по умолчанию да), не чаще раза в 12 ч (`updateLastCheck`).
   Вручную — «Настройки → Обновления → Проверить сейчас». Ручная проверка всегда показывает результат и ошибки; автоматическая молчит при ошибках.
3. **Диалог.** `ui/update/UpdateDialog.kt`: версия, размер, описание релиза; «Скачать» → прогресс → «Установить»; «Позже»; «Пропустить версию» (`updateSkipped`, касается только автопроверки).
4. **Загрузка.** `UpdateInstaller.download` → `cacheDir/updates/MeowsVPN-<версия>.apk` (проверка размера). Идёт напрямую, мимо VPN (приложение исключено).
5. **Установка.** `FileProvider` (`${applicationId}.fileprovider`, `res/xml/file_paths.xml`) → системный установщик. Нужно разрешение
   «Установка из неизвестных источников» — если его нет, открываем системные настройки и просим повторить.

## Условия, чтобы обновление встало
- Тот же `applicationId` (`app.meows.vpn`) и **тот же ключ подписи** (`C:\MeowsTools\meows.keystore`) — иначе система откажет.
- `versionCode` нового APK больше текущего.
- В релизе GitHub есть `.apk` в ассетах, тег `android-vX.Y.Z`. Выпуск — `docs/release.md`.

## Состояния
`UpdateState`: Idle · Checking · UpToDate · Available · Downloading(percent) · Downloaded · Error. Логика — `UpdateController` (не в UI).
