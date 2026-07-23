package com.termoot.integration

import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

object ProotDistroBridge {

    private const val PROOT_DISTRO_CMD = "proot-distro"

    /**
     * Returns whether the proot-distro command is available on the system.
     */
    fun isProotDistroAvailable(): Boolean {
        return try {
            val process = Runtime.getRuntime().exec(
                arrayOf("which", PROOT_DISTRO_CMD)
            )
            val exitCode = process.waitFor()
            process.destroy()
            exitCode == 0
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Retrieves the list of installed proot-distro names by parsing
     * the output of `proot-distro list`.
     *
     * The typical output format is:
     *   Proot-Distro list:
     *   Name                      Installed  Components
     *   ubuntu                    yes        ...
     *   debian                    yes        ...
     *
     * Returns an empty list if the command fails or no distributions are found.
     */
    fun getInstalledDistros(): List<String> {
        return try {
            val process = Runtime.getRuntime().exec(
                arrayOf(PROOT_DISTRO_CMD, "list")
            )
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val lines = reader.readLines()
            process.waitFor()
            reader.close()
            process.destroy()

            parseDistroList(lines)
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Parses the raw output lines from `proot-distro list` to extract
     * installed distribution names.
     *
     * The parser skips the header lines and looks for lines where the
     * second column (Installed) contains "yes". Returns the trimmed
     * name from the first column.
     */
    private fun parseDistroList(lines: List<String>): List<String> {
        if (lines.size < 3) return emptyList()

        // Skip header lines (first 2 lines: title + header row)
        val dataLines = lines.drop(2)

        return dataLines.mapNotNull { line ->
            val trimmed = line.trim()
            if (trimmed.isBlank()) return@mapNotNull null

            // Split on 2+ spaces to get columns
            val columns = trimmed.split(Regex("\\s{2,}"))
            if (columns.size < 2) return@mapNotNull null

            val name = columns[0].trim()
            val installed = columns.getOrNull(1)?.trim()?.lowercase()

            if (installed == "yes") name else null
        }
    }

    /**
     * Returns the command array for `proot-distro login <distro>`.
     */
    fun loginCommand(distroName: String): List<String> {
        return listOf(PROOT_DISTRO_CMD, "login", distroName)
    }
}
