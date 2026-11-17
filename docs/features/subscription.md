# Подписка

- Формат: ссылка Remnawave, отдающая **Xray JSON** (массив конфигов). Запрос идёт с `User-Agent: Happ/3.9.0`, заголовками `x-hwid`, `x-device-os/ver/model`, `x-app-name: MeowsVPN`.
- Ответ сохраняется в prefs (`sub.url`, `sub.body`, `sub.headers`, `sub.updated`). Из заголовков берём: `profile-title`, `announce`, `support-url`, `profile-web-page-url`, `profile-update-interval`, `subscription-userinfo` (трафик, срок).
- Если сервер отдал ссылки вместо Xray JSON — ошибка с подсказкой включить Xray JSON-шаблон в Remnawave.
- Автообновление: `P_AUTO` (0 — по подписке, −1 — выкл, N часов); при запуске — `P_LAUNCH`. Напоминания о сроке — `P_REMIND` (за 3 дня, за 1 день, в день окончания).

## Способы добавить
Меню «+» на главной (и `EmptyCard`, пока подписки нет):
1. **Вставить из буфера обмена** — `MeowsViewModel.addFromClipboard()`.
2. **QR-код** — `zxing-android-embedded` (`ScanContract`), без Google Play Services. Результат — та же ссылка.
3. **Ввести вручную** — `AddSubscriptionSheet`.
4. **Ссылка `meows://add/<url>`** (и `happ://add/…`) — открывается из браузера/кабинета, обрабатывается в `MainActivity.handleIntent`. Допускается URL-кодированная ссылка.

Все способы приходят в `MeowsViewModel.addSubscription(raw)` → `XKStore.fetch` (срезает префиксы `meows://add/`, `happ://add/`).

## Для кабинета
Готовая страница-кнопка — `docs/cabinet/open-in-app.html` (подставить `SUBSCRIPTION_URL`). На Android используется `intent://add/<url>#Intent;scheme=meows;package=app.meows.vpn;…`, на остальных `meows://add/<url>`.
Ограничения: Chrome не запускает кастомные схемы из адресной строки — только по нажатию на ссылку на странице; Telegram не делает их кликабельными. Проверено: `adb shell am start -d "meows://add/…"` открывает приложение.
