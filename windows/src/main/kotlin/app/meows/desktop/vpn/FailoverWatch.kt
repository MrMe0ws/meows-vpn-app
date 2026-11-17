package app.meows.desktop.vpn

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Сторож подключения: раз в 30 с проверяет, что интернет через VPN жив ([probe]). Два сбоя подряд — вызывает [onLost]
 * (решение, что делать — на стороне приложения). Работает только пока подключено и включено автопереключение.
 */
class FailoverWatch(
    private val scope: CoroutineScope,
    private val enabled: () -> Boolean,
    private val connected: () -> Boolean,
    private val probe: () -> Boolean,
    private val onLost: suspend () -> Unit,
) {
    private var job: Job? = null

    fun start() {
        stop()
        job = scope.launch {
            var fails = 0
            while (isActive && connected()) {
                delay(30_000)
                if (!enabled() || !connected()) { fails = 0; continue }
                if (probe()) { fails = 0; continue }
                if (++fails >= 2) { fails = 0; onLost(); return@launch }
            }
        }
    }

    fun stop() { job?.cancel(); job = null }
}
