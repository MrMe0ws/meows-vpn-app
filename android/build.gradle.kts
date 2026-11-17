plugins {
    id("com.android.application") version "8.11.1" apply false
    id("org.jetbrains.kotlin.android") version "2.2.0" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.2.0" apply false
}

// Сборка вне OneDrive: папка проекта синхронизируется и содержит кириллицу,
// а build-артефакты (сотни МБ) там только мешают. build.ps1 задаёт MEOWS_BUILD_DIR.
System.getenv("MEOWS_BUILD_DIR")?.let { out ->
    allprojects {
        layout.buildDirectory.set(file("$out/${project.name}"))
    }
}
