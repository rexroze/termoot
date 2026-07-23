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
import java.io.InputStream
import java.io.OutputStream
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Manages a single terminal session — local shell, proot-distro, or SSH.
 *
 * Each session wraps either a local [Process] (for LOCAL_SHELL and PROOT_DISTRO)
 * or a JSch [ChannelShell] (for SSH) and provides read/write access to the
 * underlying terminal. State transitions are tracked via [state].
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

    /** The Termux terminal-emulator engine, used to decode/encode terminal sequences. */
    var termuxEmulator: TerminalEmulator? = null
        private set

    /** The Termux TerminalSession from the library (may be null if JNI isn't available). */
    var termuxSession: com.termux.terminal.TerminalSession? = null
        private set

    /** JSch SSH session — only non-null for SSH workspace type. */
    var sshSession: com.jcraft.jsch.Session? = null
        private set

    /** The underlying local process — non-null for LOCAL_SHELL and PROOT_DISTRO. */
    var shellProcess: Process? = null
        private set

    /** The JSch shell channel — non-null for SSH type. */
    var sshChannel: ChannelShell? = null
        private set

    /** Input stream from the process/channel (what we read from the terminal). */
    private var terminalInputStream: InputStream? = null

    /** Output stream to the process/channel (what we write to the terminal). */
    private var terminalOutputStream: OutputStream? = null

    /** Callback invoked when new output bytes are available from the terminal. */
    @Volatile
    var onOutputAvailable: ((ByteArray) -> Unit)? = null

    /** Callback invoked when the session state changes. */
    @Volatile
    var onStateChanged: ((SessionState) -> Unit)? = null

    private val isDisconnecting = AtomicBoolean(false)
    private var ioThread: Thread? = null

    /**
     * Connects the session based on [workspace.type].
     *
     * - [WorkspaceType.LOCAL_SHELL]: spawns a local shell process.
     * - [WorkspaceType.PROOT_DISTRO]: spawns `proot-distro login <distro>`.
     * - [WorkspaceType.SSH]: establishes an SSH connection via JSch.
     */
    fun connect() {
        if (state == SessionState.CONNECTED || state == SessionState.CONNECTING) return

        isDisconnecting.set(false)
        setState(SessionState.CONNECTING)

        try {
            when (workspace.type) {
                WorkspaceType.LOCAL_SHELL -> connectLocalShell()
                WorkspaceType.PROOT_DISTRO -> connectProotDistro()
                WorkspaceType.SSH -> connectSsh()
            }
            setState(SessionState.CONNECTED)
        } catch (e: Exception) {
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
     * Writes a text string to the terminal's input stream.
     */
    fun write(text: String) {
        if (state != SessionState.CONNECTED) return
        try {
            terminalOutputStream?.write(text.toByteArray(Charsets.UTF_8))
            terminalOutputStream?.flush()
        } catch (e: Exception) {
            Log.w(TAG, "Write error on session [$id]", e)
        }
    }

    /**
     * Writes raw bytes to the terminal's input stream.
     */
    fun writeBytes(data: ByteArray) {
        if (state != SessionState.CONNECTED) return
        try {
            terminalOutputStream?.write(data)
            terminalOutputStream?.flush()
        } catch (e: Exception) {
            Log.w(TAG, "Write error on session [$id]", e)
        }
    }

    /**
     * Notifies the terminal of a size change (columns × rows).
     * For local processes this is best-effort; for SSH channels the PTY
     * size is sent via the JSch channel API.
     */
    fun resize(cols: Int, rows: Int) {
        termuxEmulator?.resize(cols, rows, cols * 8, rows * 16)
        sshChannel?.setPtySize(cols, rows, cols * 8, rows * 16)
    }

    // ---------------------------------------------------------------
    // Private connection helpers
    // ---------------------------------------------------------------

    private fun connectLocalShell() {
        val shell = TermuxBridge.getShell()
        val home = TermuxBridge.getHomeDirectory()
        val env = arrayOf(
            "TERM=xterm-256color",
            "HOME=$home",
            "PATH=/system/bin:/data/data/com.termux/files/usr/bin:/data/data/com.termux/files/usr/bin/applets"
        )

        val pb = ProcessBuilder(shell)
        pb.environment().put("TERM", "xterm-256color")
        pb.environment().put("HOME", home)
        pb.directory(java.io.File(home))
        pb.redirectErrorStream(true)

        shellProcess = pb.start()
        terminalInputStream = shellProcess?.inputStream
        terminalOutputStream = shellProcess?.outputStream

        startIoThread()
    }

    private fun connectProotDistro() {
        val distro = workspace.distroName
            ?: throw IllegalStateException("PROOT_DISTRO workspace missing distroName")

        val cmd = ProotDistroBridge.loginCommand(distro)
        val home = TermuxBridge.getHomeDirectory()

        val pb = ProcessBuilder(cmd)
        pb.environment().put("TERM", "xterm-256color")
        pb.environment().put("HOME", home)
        pb.directory(java.io.File(home))
        pb.redirectErrorStream(true)

        shellProcess = pb.start()
        terminalInputStream = shellProcess?.inputStream
        terminalOutputStream = shellProcess?.outputStream

        startIoThread()
    }

    private fun connectSsh() {
        val host = workspace.sshHost
            ?: throw IllegalStateException("SSH workspace missing sshHost")
        val user = workspace.sshUser
            ?: throw IllegalStateException("SSH workspace missing sshUser")
        val port = workspace.sshPort

        sshSession = SshBridge.createSession(
            host = host,
            port = port,
            user = user,
            password = workspace.sshPassword,
            keyPath = workspace.sshKeyPath
        )

        sshChannel = SshBridge.openShell(sshSession!!)
        terminalInputStream = sshChannel?.inputStream
        terminalOutputStream = sshChannel?.outputStream

        startIoThread()
    }

    /**
     * Starts a daemon thread that reads output from the terminal (process
     * stdout or SSH channel input) and forwards it to [onOutputAvailable].
     */
    private fun startIoThread() {
        val inputStream = terminalInputStream
            ?: throw IllegalStateException("terminalInputStream is null")

        ioThread = Thread {
            val buffer = ByteArray(4096)
            try {
                while (!isDisconnecting.get() && state != SessionState.DISCONNECTED) {
                    val bytesRead = inputStream.read(buffer)
                    if (bytesRead == -1) {
                        // Process exited or channel closed
                        if (!isDisconnecting.get()) {
                            Log.i(TAG, "End of stream reached for session [$id]")
                            setState(SessionState.DISCONNECTED)
                        }
                        break
                    }
                    if (bytesRead > 0) {
                        val chunk = buffer.copyOf(bytesRead)
                        // Track output (skip emulator feed — Java I/O loop handles this)
                        // Notify listeners
                        onOutputAvailable?.invoke(chunk)
                    }
                }
            } catch (e: java.io.IOException) {
                if (!isDisconnecting.get()) {
                    Log.w(TAG, "I/O error in reader thread", e)
                    errorMessage = e.message
                    setState(SessionState.ERROR)
                }
            } catch (e: Exception) {
                if (!isDisconnecting.get()) {
                    Log.e(TAG, "Unexpected error in I/O thread", e)
                    errorMessage = e.message
                    setState(SessionState.ERROR)
                }
            }
        }.apply {
            isDaemon = true
            name = "session-io-$id"
            start()
        }
    }

    /**
     * Releases all native resources: kills the local process, disconnects
     * the SSH session/channel, and interrupts the I/O thread.
     */
    private fun cleanup() {
        try {
            // Interrupt I/O thread
            ioThread?.interrupt()
            ioThread = null
        } catch (_: Exception) {
        }

        try {
            // Kill local process
            shellProcess?.destroyForcibly()
            shellProcess = null
        } catch (_: Exception) {
        }

        try {
            // Disconnect SSH channel
            sshChannel?.disconnect()
            sshChannel = null
        } catch (_: Exception) {
        }

        try {
            // Disconnect SSH session
            SshBridge.disconnect(sshSession)
            sshSession = null
        } catch (_: Exception) {
        }

        // Clean up Termux session
        try {
            termuxSession = null
        } catch (_: Exception) {
        }
        termuxSession = null
        termuxEmulator = null
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
