package app.meows.vpn.i18n

import android.content.Context
import android.content.res.Resources
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import app.meows.vpn.XKStore

/**
 * Язык интерфейса: 0 — авто (язык системы: русский → русский, иначе английский), 1 — русский, 2 — английский.
 * Тексты в коде написаны по-русски и служат ключом: `tr("Настройки")`. Нет перевода — показываем русский.
 * Выбор лежит в Compose-состоянии, поэтому экраны, вызвавшие `tr`, перерисовываются при смене языка.
 */
object Lang {
    const val AUTO = 0
    const val RU = 1
    const val EN = 2
    private const val KEY = "lang"

    var choice by mutableIntStateOf(AUTO)
        private set

    private val en: Map<String, String> by lazy { enDict1 + enDict2 + enDict3 + enDict4 + enDict5 + enDict6 + enDict7 }

    @JvmStatic fun init(ctx: Context) { choice = XKStore.get(ctx.applicationContext).pref(KEY, AUTO) }

    @JvmStatic fun set(ctx: Context, value: Int) {
        XKStore.get(ctx.applicationContext).prefs().edit().putInt(KEY, value).apply()
        choice = value
    }

    val isEn: Boolean
        get() = when (choice) {
            RU -> false
            EN -> true
            else -> Resources.getSystem().configuration.locales[0].language != "ru"
        }

    @JvmStatic fun tr(ru: String): String = if (isEn) en[ru] ?: ru else ru
}

/** Перевод строки по русскому ключу. */
fun tr(ru: String): String = Lang.tr(ru)

/** Перевод шаблона с `%s`/`%d` и подстановка значений. */
fun trf(ru: String, vararg args: Any?): String = String.format(Lang.tr(ru), *args)

/** Слово «сервер» в нужной форме для числа [n] (по-русски три формы, по-английски две). */
fun serversWord(n: Int): String =
    if (Lang.isEn) (if (n == 1) tr("сервер") else tr("сервера"))
    else if (n % 10 == 1 && n % 100 != 11) "сервер"
    else if (n % 10 in 2..4 && n % 100 !in 12..14) "сервера"
    else "серверов"
