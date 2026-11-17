# Выпуск релиза Android (для автообновления)

Репозиторий общий для платформ, поэтому Android-релизы помечаются тегом **`android-vX.Y.Z`** (iOS в будущем — `ios-v…`).
Приложение при проверке обновлений берёт самый свежий релиз с таким тегом.

1. В `android/app/build.gradle.kts`: `versionName = "X.Y.Z"` и `versionCode` **+1** (versionCode только растёт, независимо от имени версии).
2. `cd android; .\build.ps1` → `android\dist\MeowsVPN-release.apk`. Проверить на устройстве.
3. Скопировать APK как `MeowsVPN-X.Y.Z.apk` и выпустить:
   ```powershell
   gh release create android-vX.Y.Z android\dist\MeowsVPN-X.Y.Z.apk --title "Meows X.Y.Z (Android)" --notes "что нового"
   ```
   Описание релиза показывается пользователю в диалоге обновления — пиши по-русски и коротко.
4. Не помечать как pre-release и не оставлять черновиком — такие релизы приложение игнорирует.
5. Пользователи получат предложение при следующей проверке (до 12 ч) или по «Проверить сейчас».

Обновится только APK, подписанный **тем же ключом** (см. `build.md`). Репозиторий публичный — иначе API без токена вернёт 404.
## Windows
Тег **`windows-vX.Y.Z`**, ассеты: `Meows-X.Y.Z.msi` (обязательно: его скачивает обновлялка) и `Meows-win-x64-X.Y.Z.zip` (портативная). Версия — в `windows/build.gradle.kts` (`version` и `packageVersion`).
```powershell
cd windows; .\build.ps1
gh release create windows-vX.Y.Z windows\dist\Meows-X.Y.Z.msi windows\dist\Meows-win-x64-X.Y.Z.zip --title "Meows X.Y.Z (Windows)" --notes "что нового"
```
Проверка обновления: поставить MSI меньшей версии, запустить, «Настройки → Обновления → Проверить сейчас».