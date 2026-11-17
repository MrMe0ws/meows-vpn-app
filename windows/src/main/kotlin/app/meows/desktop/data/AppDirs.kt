package app.meows.desktop.data

import java.io.File

/** Папка данных приложения: %APPDATA%\Meows (настройки, подписка, конфиг ядра, гео-файлы). */
object AppDirs {
    val data: File = File(System.getenv("APPDATA") ?: System.getProperty("user.home"), "Meows").apply { mkdirs() }
    val geo: File = File(data, "geo").apply { mkdirs() }
    fun file(name: String) = File(data, name)

    /** Папка с meows-core.exe и гео-файлами (ресурсы приложения; при запуске из Gradle — resources/windows-x64). */
    val resources: File = File(System.getProperty("compose.application.resources.dir") ?: "resources/windows-x64")
    val coreExe: File get() = File(resources, "meows-core.exe")
}