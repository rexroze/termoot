package com.termoot.session

import android.util.Log
import com.jcraft.jsch.ChannelShell
import com.termoot.domain.model.SessionState
import com.termoot.domain.model.Workspace
import com.termoot.domain.model.WorkspaceType
import com.termoot.integration.ProotDistroBridge
import com.termoot.integration.SshBridge
import com.termoot.integration.TermuxBridge
import com.termux.terminal.TerminalEmulator
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Manages a single terminal session — local shell, proot-distro, or SSH.
 *
 * For [WorkspaceType.LOCAL_SHELL] and [WorkspaceType.PROOT_DISTRO], the session
 * delegates to the Termux library's [com.termux.terminal.TerminalSession], which
 * handles PTY allocation and process management. For [WorkspaceType.SSH], a JSch
 * [ChannelShell] provides the I/O channel and a dummy Termux session provides
 * the terminal emulator for rendering.
 */
class TerminalSession(
    val id: String,
    val workspace: Workspace,
    val name: String
) {

    @Volatile
    var state: SessionState = SessionState.DISCONNECTED
        private set

    @Volatile
    var errorMessage: String? = null
        private set

    /** The Termux TerminalSession from the library (handles PTY and process). */
    var termuxSession: com.termux.terminal.TerminalSession? = null
        private set

    /** JSch SSH session — only non-null for SSH workspace type. */
    var sshSession: com.jcraft.jsch.Session? = null
        private set

    /** The JSch shell channel — non-null for SSH type. */
    var sshChannel: ChannelShell? = null
        private set

    /** Input stream from the SSH channel (what we read from the remote terminal). */
    private var terminalInputStream: InputStream? = null

    /** Output stream to the SSH channel (what we write to the remote terminal). */
    private var terminalOutputStream: OutputStream? = null

    /** Callback invoked when new output bytes are available from the terminal. */
    @Volatile
    var onOutputAvailable: ((ByteArray) -> Unit)? = null

    /** Callback invoked when the session state changes. */
    @Volatile
    var onStateChanged: ((SessionState) -> Unit)? = null

    private val isDisconnecting = AtomicBoolean(false)

    /**
     * Connects the session based on [workspace.type].
     *
     * - [WorkspaceType.LOCAL_SHELL]: creates a Termux TerminalSession
     *   (the shell process spawns when TerminalView attaches and lays out).
     * - [WorkspaceType.PROOT_DISTRO]: creates a Termux TerminalSession for
     *   `proot-distro login <distro>`.
     * - [WorkspaceType.SSH]: establishes an SSH connection via JSch and creates
     *   a dummy Termux TerminalSession whose emulator is fed remote output.
     */
    fun connect() {
        if (state == SessionState.CONNECTED || state == SessionState.CONNECTING) return

        isDisconnecting.set(false)
        setState(SessionState.CONNECTING)

        try {
            when (workspace.type) {
                WorkspaceType.LOCAL_SHELL -> {
                    connectLocalShell()
                    setState(SessionState.CONNECTED)
                }
                WorkspaceType.PROOT_DISTRO -> {
                    connectProotDistro()
                    setState(SessionState.CONNECTED)
                }
                WorkspaceType.SSH -> {
                    // connectSsh() creates the Termux session on the calling thread,
                    // then launches JSch connection on a background thread.
                    // State CONNECTED/ERROR is set by the background thread.
                    connectSsh()
                }
            }
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to connect session [$id]: ${e.message}", e)
            errorMessage = e.message ?: "Unknown connection error"
            setState(SessionState.ERROR)
            cleanup()
        }
    }

    /**
     * Disconnects the session and releases all resources.
     * Safe to call multiple times.
     */
    fun disconnect() {
        if (state == SessionState.DISCONNECTED) return
        if (!isDisconnecting.compareAndSet(false, true)) return
        setState(SessionState.DISCONNECTED)
        cleanup()
    }

    /**
     * Writes a text string to the terminal.
     *
     * For local/proot sessions the Termux [termuxSession] handles the write to
     * the PTY.  For SSH sessions (where the dummy cat process has been killed
     * and [com.termux.terminal.TerminalSession.isRunning] returns false) the
     * write is sent to the JSch channel output stream.
     */
    fun write(text: String) {
        if (state != SessionState.CONNECTED) return
        try {
            val session = termuxSession
            if (session != null && session.isRunning()) {
                val data = text.toByteArray(Charsets.UTF_8)
                session.write(data, 0, data.size)
            } else {
                terminalOutputStream?.write(text.toByteArray(Charsets.UTF_8))
                terminalOutputStream?.flush()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Write error on session [$id]", e)
        }
    }

    /**
     * Writes raw bytes to the terminal.
     *
     * See [write] for the dispatch logic between Termux session and SSH channel.
     */
    fun writeBytes(data: ByteArray) {
        if (state != SessionState.CONNECTED) return
        try {
            val session = termuxSession
            if (session != null && session.isRunning()) {
                session.write(data, 0, data.size)
            } else {
                terminalOutputStream?.write(data)
                terminalOutputStream?.flush()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Write error on session [$id]", e)
        }
    }

    /**
     * Notifies the terminal of a size change (columns × rows).
     *
     * For local/proot, [com.termux.terminal.TerminalSession.updateSize] is
     * called automatically by TerminalView on layout; this explicit resize
     * also propagates to the SSH remote PTY when applicable.
     */
    fun resize(cols: Int, rows: Int) {
        // Note: termuxSession.updateSize() is called automatically by
        // TerminalView on layout with correct pixel dimensions; we only
        // need to resize the SSH channel PTY separately.
        sshChannel?.setPtySize(cols, rows, cols * 8, rows * 16)
    }

    // ---------------------------------------------------------------
    // Private connection helpers
    // ---------------------------------------------------------------

    private fun connectLocalShell() {
        val shellPath = TermuxBridge.getShell()
        val home = TermuxBridge.getHomeDirectory()
        val env = arrayOf(
            "TERM=xterm-256color",
            "HOME=$home",
            "PATH=/system/bin:/system/xbin:/data/data/com.termux/files/usr/bin"
        )
        // Only bash supports --login; sh and other shells do not.
        val isBash = shellPath.contains("bash")
        val args = if (isBash) arrayOf("--login") else emptyArray()

        termuxSession = com.termux.terminal.TerminalSession(
            shellPath,
            home,
            args,
            env,
            null,
            null
        )
    }

    private fun connectProotDistro() {
        val distro = workspace.distroName
            ?: throw IllegalStateException("PROOT_DISTRO workspace missing distroName")

        val cmd = ProotDistroBridge.loginCommand(distro)
        val home = TermuxBridge.getHomeDirectory()

        val shellPath = cmd[0]
        val args = cmd.drop(1).toTypedArray()

        val env = arrayOf(
            "TERM=xterm-256color",
            "HOME=$home",
            "PATH=/system/bin:/data/data/com.termux/files/usr/bin:/data/data/com.termux/files/usr/bin/applets"
        )

        termuxSession = com.termux.terminal.TerminalSession(
            shellPath,
            home,
            args,
            env,
            null,
            null
        )
    }

    private fun connectSsh() {
        val host = workspace.sshHost
            ?: throw IllegalStateException("SSH workspace missing sshHost")
        val user = workspace.sshUser
            ?: throw IllegalStateException("SSH workspace missing sshUser")
        val port = workspace.sshPort
        val password = workspace.sshPassword
        val keyPath = workspace.sshKeyPath

        // Create the dummy Termux TerminalSession on the CURRENT thread
        // (must have a Looper — this runs on the main thread via SessionManager).
        val home = TermuxBridge.getHomeDirectory()
        termuxSession = com.termux.terminal.TerminalSession(
            "/system/bin/cat",
            home,
            arrayOf(),
            arrayOf("TERM=xterm-256color", "HOME=$home"),
            null,
            null
        )

        // Launch JSch connection on a background thread to avoid
        // blocking the main thread on network I/O.
        Thread({
            try {
                sshSession = SshBridge.createSession(
                    host = host,
                    port = port,
                    user = user,
                    password = password,
                    keyPath = keyPath
                )

                sshChannel = SshBridge.openShell(sshSession!!)
                terminalInputStream = sshChannel?.inputStream
                terminalOutputStream = sshChannel?.outputStream

                // Signal connected before waiting for the emulator so
                // the TerminalView renders and creates the emulator.
                setState(SessionState.CONNECTED)

                // Wait for emulator to initialise (happens when TerminalView attaches)
                // then bridge SSH I/O to it.
                startSshBridge()
            } catch (e: Exception) {
                Log.e(TAG, "SSH connection failed for session [$id]: ${e.message}", e)
                errorMessage = e.message ?: "SSH connection failed"
                setState(SessionState.ERROR)
                cleanup()
            }
        }).apply {
            isDaemon = true
            name = "ssh-connect-$id"
            start()
        }
    }

    /**
     * Waits for the Termux emulator to be initialised (which happens when
     * TerminalView attaches and lays out), then bridges SSH channel I/O to
     * the emulator.
     *
     * The dummy `cat` process is killed once the emulator is ready; subsequent
     * writes through [termuxSession] become no-ops, so input must go through
     * [terminalOutputStream] (the JSch channel output).
     */
    private fun startSshBridge() {
        // Poll for the emulator (created by TerminalView's layout pass)
        var emulator: TerminalEmulator? = null
        while (emulator == null && state == SessionState.CONNECTED && !isDisconnecting.get()) {
            emulator = termuxSession?.getEmulator()
            if (emulator == null) {
                try { Thread.sleep(50) } catch (_: InterruptedException) { return }
            }
        }

        if (emulator != null && state == SessionState.CONNECTED && !isDisconnecting.get()) {
            bridgeSshToEmulator(emulator)
        }
    }

    private fun bridgeSshToEmulator(emulator: TerminalEmulator) {
        val channel = sshChannel ?: return
        val input = channel.inputStream

        // Kill the dummy cat process — after this termuxSession.write() is a no-op
        termuxSession?.finishIfRunning()

        Log.i(TAG, "SSH bridge started for session [$id]")

        val buffer = ByteArray(4096)
        try {
            while (state == SessionState.CONNECTED && !isDisconnecting.get()) {
                val bytesRead = input.read(buffer)
                if (bytesRead == -1) {
                    Log.i(TAG, "SSH channel closed for session [$id]")
                    if (!isDisconnecting.get()) {
                        setState(SessionState.DISCONNECTED)
                    }
                    break
                }
                if (bytesRead > 0) {
                    emulator.append(buffer, bytesRead)
                    onOutputAvailable?.invoke(buffer.copyOf(bytesRead))
                }
            }
        } catch (e: IOException) {
            if (!isDisconnecting.get()) {
                Log.w(TAG, "SSH I/O error in bridge thread", e)
                errorMessage = e.message
                setState(SessionState.ERROR)
            }
        } catch (e: Exception) {
            if (!isDisconnecting.get()) {
                Log.e(TAG, "Unexpected error in SSH bridge", e)
                errorMessage = e.message
                setState(SessionState.ERROR)
            }
        }
    }

    /**
     * Releases all resources: kills the Termux session, disconnects the SSH
     * channel and session, and clears I/O streams.
     */
    private fun cleanup() {
        try {
            termuxSession?.finishIfRunning()
        } catch (_: Exception) {}
        termuxSession = null

        try {
            sshChannel?.disconnect()
            sshChannel = null
        } catch (_: Exception) {}

        try {
            SshBridge.disconnect(sshSession)
            sshSession = null
        } catch (_: Exception) {}

        terminalInputStream = null
        terminalOutputStream = null
    }

    private fun setState(newState: SessionState) {
        state = newState
        onStateChanged?.invoke(newState)
    }

    companion object {
        private const val TAG = "TerminalSession"
    }
}
