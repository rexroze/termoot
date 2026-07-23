package com.termoot.integration

import android.content.Context
import android.os.Environment
import com.termoot.TermootApplication
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

object TermuxBridge {

    private const val TERMUX_PACKAGE = "com.termux"

    /**
     * Checks whether Termux is installed and available on the device.
     */
    fun isTermuxRunning(): Boolean {
        return try {
            val pm = TermootApplication.instance.packageManager
            pm.getPackageInfo(TERMUX_PACKAGE, 0)
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Returns a shell path that is guaranteed to exist on the device.
     *
     * Priority order:
     * 1. `$SHELL` environment variable, if the file exists
     * 2. Termux bash at its standard install path
     * 3. Android system shell (`/system/bin/sh`) — present on all devices
     * 4. Fallback `/bin/sh`
     */
    fun getShell(): String {
        val envShell = System.getenv("SHELL")
        if (envShell != null && File(envShell).exists()) {
            return envShell
        }
        if (File("/data/data/com.termux/files/usr/bin/bash").exists()) {
            return "/data/data/com.termux/files/usr/bin/bash"
        }
        if (File("/system/bin/sh").exists()) {
            return "/system/bin/sh"
        }
        return if (File("/bin/sh").exists()) "/bin/sh" else "/system/bin/sh"
    }

    /**
     * Returns the home directory path.
     * Uses the Termux home convention (/data/data/com.termux/files/home)
     * if Termux is detected, otherwise falls back to the app's files directory.
     */
    fun getHomeDirectory(): String {
        return if (isTermuxRunning()) {
            "/data/data/$TERMUX_PACKAGE/files/home"
        } else {
            try {
                TermootApplication.instance.filesDir.absolutePath
            } catch (e: Exception) {
                "/data/data/com.termoot/files"
            }
        }
    }

    /**
     * Executes a shell command and returns the combined stdout+stderr output.
     * Returns null if the command fails or an exception occurs.
     */
    fun executeCommand(command: String): String? {
        return try {
            val runtime = Runtime.getRuntime()
            val process = runtime.exec(
                arrayOf(getShell(), "-c", command),
                getEnvironmentArray(),
                File(getHomeDirectory())
            )

            val reader = BufferedReader(
                InputStreamReader(process.inputStream)
            )
            val errorReader = BufferedReader(
                InputStreamReader(process.errorStream)
            )

            val output = StringBuilder()
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                output.appendLine(line)
            }
            while (errorReader.readLine().also { line = it } != null) {
                output.appendLine(line)
            }

            process.waitFor()
            reader.close()
            errorReader.close()
            process.destroy()

            output.toString().trim().ifEmpty { null }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Builds an environment variable array suitable for shell subprocesses.
     * Includes TERM, HOME, and PATH if available.
     */
    private fun getEnvironmentArray(): Array<String> {
        val env = mutableListOf(
            "TERM=xterm-256color",
            "HOME=${getHomeDirectory()}"
        )
        val path = System.getenv("PATH")
        if (path != null) {
            env.add("PATH=$path")
        }
        val lang = System.getenv("LANG")
        if (lang != null) {
            env.add("LANG=$lang")
        }
        return env.toTypedArray()
    }
}
