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
     * Returns the shell path — uses $SHELL if set, otherwise falls back to /bin/bash.
     */
    fun getShell(): String {
        return System.getenv("SHELL") ?: "/bin/bash"
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
            TermootApplication.instance.filesDir.absolutePath
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
