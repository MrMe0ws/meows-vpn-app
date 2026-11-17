package app.meows.vpn.update

/** Ссылки проекта. Единственное место, где указан репозиторий. */
object AppLinks {
    const val GITHUB_OWNER = "MrMe0ws"
    const val GITHUB_REPO = "meows-vpn-app"

    const val GITHUB_URL = "https://github.com/$GITHUB_OWNER/$GITHUB_REPO"
    const val RELEASES_URL = "$GITHUB_URL/releases"

    /** Репозиторий общий для платформ (android/, позже ios/), поэтому ищем среди последних релизов только Android-теги. */
    const val RELEASES_API = "https://api.github.com/repos/$GITHUB_OWNER/$GITHUB_REPO/releases?per_page=20"
    const val ANDROID_TAG_PREFIX = "android-v"
}