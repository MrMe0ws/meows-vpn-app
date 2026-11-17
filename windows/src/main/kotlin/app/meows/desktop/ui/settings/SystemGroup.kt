package app.meows.desktop.ui.settings

import androidx.compose.runtime.Composable
import app.meows.desktop.AppModel

/** Интеграция с Windows: автозапуск, подключение при запуске, сворачивание в трей. */
@Composable
fun SystemGroup(app: AppModel) {
    Group("Система", "При автозапуске Meows стартует свёрнутым в трей. Иконка в трее: открыть, подключить/отключить, выйти.") {
        SwitchRow("Запускать вместе с Windows", null, app.autoStart, app::chooseAutoStart)
        SwitchRow("Подключаться при запуске", "Если подписка добавлена", app.autoConnect, app::chooseAutoConnect)
        SwitchRow("Автопереключение серверов", "Если сервер перестал отвечать, Meows подключится к ближайшему рабочему. Выключите, если нужна конкретная страна", app.autoSwitch, app::chooseAutoSwitch)
        SwitchRow("Напоминать об окончании", "Срок подписки и трафик — уведомление у иконки в трее", app.remind, app::chooseRemind)
        SwitchRow("Сворачивать в трей при закрытии", "Окно скрывается, VPN продолжает работать", app.closeToTray, app::chooseCloseToTray)
    }
}