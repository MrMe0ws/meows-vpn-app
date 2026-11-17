plugins {
    kotlin("jvm") version "2.2.20"
    id("org.jetbrains.kotlin.plugin.compose") version "2.2.20"
    id("org.jetbrains.compose") version "1.9.3"
}

group = "app.meows"
version = "1.0.0"

// Сборка вне OneDrive (build.ps1 задаёт MEOWS_WIN_BUILD_DIR)
System.getenv("MEOWS_WIN_BUILD_DIR")?.let { layout.buildDirectory.set(file(it)) }

dependencies {
    implementation(compose.desktop.currentOs)
    implementation(compose.material3)
    implementation(compose.materialIconsExtended)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.10.2")
    implementation("com.materialkolor:material-kolor:3.0.1")   // палитры Material You из seed-цвета
    implementation("org.json:json:20240303")
    implementation("net.java.dev.jna:jna-platform:5.14.0")      // реестр и WinInet для системного прокси
}

compose.desktop {
    application {
        mainClass = "app.meows.desktop.MainKt"
        // meows-core.exe и гео-файлы: resources/windows-x64 попадает рядом с приложением
        nativeDistributions {
            appResourcesRootDir.set(project.layout.projectDirectory.dir("resources"))
            packageName = "Meows"
            packageVersion = "1.0.0"
            description = "Meows VPN client"
            vendor = "Meows"
            modules("java.net.http", "jdk.unsupported", "java.naming")
            targetFormats(org.jetbrains.compose.desktop.application.dsl.TargetFormat.Msi)
            windows {
                iconFile.set(project.file("icons/meows.ico"))
                menuGroup = "Meows"
                perUserInstall = true          // без прав администратора: %LOCALAPPDATA%\Meows
                shortcut = true                // ярлык на рабочем столе
                dirChooser = false
                upgradeUuid = "8f3c2a6e-5d41-4b7a-9c1e-2b6f0a7d4e11"   // постоянный: по нему MSI обновляет прежнюю версию
            }
        }
    }
}