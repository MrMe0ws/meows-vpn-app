# Карта файлов

Обновляй при добавлении/переименовании файлов. Корень кода: `android/app/src/main/java/app/meows/vpn/`.

```
MainActivity.kt          Activity: разрешения VPN/уведомлений, QR-сканер, ссылки meows://add/…
MeowsApp.kt              корень Compose: Главная ⇄ Настройки, снэкбар, общие диалоги
MeowsViewModel.kt        состояние и команды для UI
XKStore.java             подписка, серверы, пинг, prefs            (легаси)
XKVpnService.java        VpnService, запуск ядра                   (легаси)
XKTileService.java       плитка быстрых настроек                   (легаси)
XKReceiver.java          напоминания о сроке, автозапуск           (легаси)
XKAppHolder.java         глобальный Context для плитки/сервиса     (легаси)
widget/          ConnectWidgetSmall.kt (1x1), ConnectWidgetBig.kt (2x2), Widgets.kt (касание, сервер, обновление), WidgetAnimator.kt (плавность) — features/widget.md

ui/theme/        Theme.kt (MeowsTheme), Palettes.kt
ui/components/   CatConnectButton.kt, PingChip.kt, Format.kt — общие виджеты и форматирование
ui/home/         HomeScreen.kt (сборка экрана), HomeToolbar.kt (нижняя панель: настройки и «+»), StatusText.kt,
                 SubscriptionHeader.kt, AccountActions.kt (Кабинет/Продлить/Поддержка), SupportSheet.kt,
                 ServerRow.kt (свайп влево открывает кнопку «Пинг»), EmptyCard.kt,
                 AddSubscriptionSheet.kt (ручной ввод), ConfigDialog.kt
ui/settings/     SettingsScreen.kt (роутер страниц, enum SettingsPage), SettingsHome.kt (список разделов),
                 SettingsComponents.kt (SettingsGroup, SwitchRow, ValueRow, PickRow, ChoiceDialog, каркас страницы)
ui/settings/pages/  AppearancePage, RoutingPage, SubscriptionPage, ConnectionPage, PingPage,
                    UpdatesPage, AboutPage, BypassPage, AppsPage — по одному файлу на раздел
ui/update/       UpdateDialog.kt
shop/            ShopLinks.kt (кабинет/поддержка/срок), Diagnostics.kt (отчёт для поддержки)
update/          AppLinks.kt (ссылки/репозиторий), ReleaseInfo.kt (+UpdateState), Version.kt,
                 UpdateChecker.kt (GitHub API), UpdateInstaller.kt (загрузка/установка), UpdateController.kt
```

Прочее: `android/app/src/main/res/` — иконки (`drawable-nodpi/logo_cat.webp`, `sleep_cat.webp`, `ic_launcher_*`), темы, `xml/file_paths.xml`;
`android/app/src/main/jniLibs/` — ядро; `core/` — исходники Go-обёртки; `android/build.ps1`; `docs/`.

## Как добавить…

- **Страницу настроек:** файл в `ui/settings/pages/`, элемент в `SettingsPage` (`SettingsScreen.kt`), строка в `SettingsHome.kt`.
- **Экран:** папка `ui/<имя>/`, подключить в `MeowsApp.kt`.
- **Настройку:** ключ в prefs; чтение `vm.flag/pref`, запись `vm.setFlag/setPref`; если влияет на VPN — `reconnect = true`.
