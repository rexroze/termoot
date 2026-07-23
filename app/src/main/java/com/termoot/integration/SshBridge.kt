package com.termoot.integration

import com.jcraft.jsch.ChannelShell
import com.jcraft.jsch.JSch
import com.jcraft.jsch.JSchException
import com.jcraft.jsch.Session
import java.io.File

object SshBridge {

    private const val DEFAULT_TIMEOUT_MS = 10000
    private const val DEFAULT_PORT = 22

    /**
     * Creates an authenticated JSch SSH session.
     *
     * Authentication is attempted in the following order:
     * 1. Key-based authentication if [keyPath] is provided
     * 2. Password-based authentication if [password] is provided
     *
     * @param host The SSH hostname or IP address.
     * @param port The SSH port (default 22).
     * @param user The SSH username.
     * @param password Optional password for password-based auth.
     * @param keyPath Optional path to a private key file for key-based auth.
     * @return A connected [Session] instance.
     * @throws JSchException if connection or authentication fails.
     */
    @Throws(JSchException::class)
    fun createSession(
        host: String,
        port: Int = DEFAULT_PORT,
        user: String,
        password: String? = null,
        keyPath: String? = null
    ): Session {
        val jsch = JSch()

        // Add identity from private key if provided
        if (!keyPath.isNullOrBlank()) {
            val keyFile = File(keyPath)
            if (keyFile.exists()) {
                if (!password.isNullOrBlank()) {
                    jsch.addIdentity(keyFile.absolutePath, password)
                } else {
                    jsch.addIdentity(keyFile.absolutePath)
                }
            }
        }

        val session = jsch.getSession(user, host, port)
        session.setTimeout(DEFAULT_TIMEOUT_MS)

        // Disable strict host key checking for compatibility; a real app
        // should manage known hosts properly.
        session.setConfig("StrictHostKeyChecking", "no")

        if (!password.isNullOrBlank() && keyPath.isNullOrBlank()) {
            session.setPassword(password)
        }

        session.connect(DEFAULT_TIMEOUT_MS)
        return session
    }

    /**
     * Opens a shell channel on an existing SSH session.
     *
     * The returned channel is connected and ready for I/O. The caller
     * can obtain the input/output streams via [ChannelShell.getInputStream]
     * and [ChannelShell.getOutputStream].
     *
     * @param session A connected JSch SSH session.
     * @return A connected [ChannelShell] instance.
     * @throws JSchException if the channel cannot be opened.
     */
    @Throws(JSchException::class)
    fun openShell(session: Session): ChannelShell {
        val channel = session.openChannel("shell") as ChannelShell
        channel.setPtyType("xterm-256color")
        channel.connect(DEFAULT_TIMEOUT_MS)
        return channel
    }

    /**
     * Gracefully disconnects an SSH session and all its channels.
     *
     * This method is safe to call even if the session is already
     * disconnected or null.
     */
    fun disconnect(session: Session?) {
        if (session == null || !session.isConnected) return
        try {
            session.disconnect()
        } catch (e: Exception) {
            android.util.Log.w("SshBridge", "Error disconnecting SSH session", e)
        }
    }
}
