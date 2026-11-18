package app.meows.vpn

/** Какая сборка: Google Play (без автообновления и ссылок на оплату) или прямая (GitHub/сайт). */
object Edition {
    @JvmField val isPlay: Boolean = BuildConfig.PLAY
    const val PLAY_URL = "https://play.google.com/store/apps/details?id=app.meows.vpn"
}
