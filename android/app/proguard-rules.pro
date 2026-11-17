# Ядро Xray — отдельный процесс, JNI не используется. Java-логика (XK*) вызывается из манифеста.
-keep class app.meows.vpn.XK* { *; }
-dontwarn org.slf4j.**
