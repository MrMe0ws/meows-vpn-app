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

## Постоянные ссылки на свежую сборку («плавающие» релизы)
Кабинет магазина и сайт дают прямую ссылку без номера версии:
- Android: `https://github.com/MrMe0ws/meows-vpn-app/releases/download/android-latest/Meows.apk`
- Windows: `https://github.com/MrMe0ws/meows-vpn-app/releases/download/windows-latest/Meows.msi`

Как это устроено: релизы с тегами `android-latest` и `windows-latest` (не prerelease, не «Latest» репозитория), в каждом один файл с постоянным именем.
Обновляет их workflow `.github/workflows/floating-latest.yml` при **каждом** выходе релиза `android-v…` / `windows-v…` — он запускает `tools/publish-latest.ps1`
(скачивает ассет релиза, удаляет старый `Meows.apk`/`Meows.msi`, кладёт новый, обновляет описание). Руками ничего делать не нужно.
- Вручную (если Actions не сработал): `.\tools\publish-latest.ps1 -Tag android-v1.4.0` (нужен авторизованный `gh`).
- Приложения их не видят: проверка обновлений берёт только теги `android-v…` / `windows-v…`.
- Плавающие релизы не удалять и не помечать как prerelease/«Latest».

## Нумерация версий
X.Y.Z: **третья цифра — только исправления багов и мелочи**; набор новых функций или разделов интерфейса — вторая цифра (X.Y.0); крупная переработка — первая. Перед выпуском посмотри, что вошло, и назови выбранный номер владельцу. (Windows 1.0.2 по ошибке вышла как «патч» с кучей новых функций — это было минимум 1.1.0.)

