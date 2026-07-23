# Keep Termux terminal libraries
-keep class com.termux.terminal.** { *; }
-keep class com.termux.view.** { *; }

# Keep Room entities
-keep class com.termoot.data.local.entity.** { *; }

# Keep SSH
-keep class com.jcraft.jsch.** { *; }
-dontwarn com.jcraft.jsch.**
