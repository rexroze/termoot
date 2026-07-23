package com.termoot.ui.components

import android.graphics.Typeface
import android.util.Log
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import java.nio.CharBuffer
import java.nio.charset.CodingErrorAction
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.termoot.ui.theme.BackgroundDark
import com.termoot.ui.theme.TerminalRed
import com.termoot.ui.theme.TextSecondary

/**
 * Composable wrapper around Termux's [TerminalView].
 *
 * Renders an [AndroidView] hosting the native terminal widget with proper
 * dark theme colours.  If the Termux view fails to initialise (missing
 * native library, invalid shell, etc.) a user-friendly error is shown
 * instead of crashing.
 *
 * @param terminalSession  The Termux [TerminalSession] to attach.
 *                          Pass `null` to detach any previously attached session.
 * @param modifier         Modifier applied to the host layout.
 */
@Composable
fun TerminalView(
    terminalSession: com.termux.terminal.TerminalSession?,
    modifier: Modifier = Modifier,
    onWrite: ((ByteArray) -> Unit)? = null
) {
    val context = LocalContext.current
    var viewError by remember { mutableStateOf<String?>(null) }

    val terminalView = remember {
        try {
            com.termux.view.TerminalView(context, null).apply {
                id = View.generateViewId()
                setBackgroundColor(BackgroundDark.toArgb())
                setTextSize(12)
                setTypeface(Typeface.MONOSPACE)

                // For SSH sessions: intercept keystrokes so they go to the
                // JSch channel instead of the dead dummy PTY.
                if (onWrite != null) {
                    setTerminalViewClient(object : com.termux.view.TerminalViewClient {
                        override fun onScale(scale: Float): Float = scale
                        override fun onSingleTapUp(e: MotionEvent) {}
                        override fun shouldBackButtonBeMappedToEscape(): Boolean = false
                        override fun shouldEnforceCharBasedInput(): Boolean = false
                        override fun shouldUseCtrlSpaceWorkaround(): Boolean = false
                        override fun isTerminalViewSelected(): Boolean = false
                        override fun copyModeChanged(copyMode: Boolean) {}
                        override fun onKeyUp(keyCode: Int, e: KeyEvent): Boolean = false
                        override fun onLongPress(event: MotionEvent): Boolean = false
                        override fun readControlKey(): Boolean = false
                        override fun readAltKey(): Boolean = false
                        override fun readShiftKey(): Boolean = false
                        override fun readFnKey(): Boolean = false
                        override fun onEmulatorSet() {}
                        override fun logError(tag: String, message: String) {}
                        override fun logWarn(tag: String, message: String) {}
                        override fun logInfo(tag: String, message: String) {}
                        override fun logDebug(tag: String, message: String) {}
                        override fun logVerbose(tag: String, message: String) {}
                        override fun logStackTraceWithMessage(tag: String, message: String, e: Exception) {}
                        override fun logStackTrace(tag: String, e: Exception) {}

                        override fun onKeyDown(
                            keyCode: Int,
                            event: KeyEvent,
                            session: com.termux.terminal.TerminalSession
                        ): Boolean {
                            val bytes = keyEventToBytes(keyCode, event)
                            if (bytes != null) {
                                onWrite(bytes)
                                return true
                            }
                            return false
                        }

                        override fun onCodePoint(
                            codePoint: Int,
                            ctrlDown: Boolean,
                            session: com.termux.terminal.TerminalSession
                        ): Boolean {
                            val bytes = codePointToUtf8(codePoint)
                            if (bytes.isNotEmpty()) {
                                onWrite(bytes)
                            }
                            return true // always consume for SSH
                        }
                    })
                }
            }
        } catch (e: UnsatisfiedLinkError) {
            Log.e("TerminalView", "Native library missing", e)
            viewError = "Native terminal library not found"
            null
        } catch (e: Exception) {
            Log.e("TerminalView", "Failed to create Termux TerminalView", e)
            viewError = e.message ?: "Unknown error creating terminal view"
            null
        }
    }

    if (terminalView != null) {
        AndroidView(
            modifier = modifier
                .fillMaxSize()
                .background(BackgroundDark),
            factory = { terminalView },
            update = { view ->
                try {
                    view.attachSession(terminalSession)
                } catch (e: Throwable) {
                    Log.e("TerminalView", "Failed to attach session", e)
                    viewError = e.message ?: "Failed to attach terminal session"
                }
            }
        )

        DisposableEffect(Unit) {
            onDispose {
                try {
                    terminalView.attachSession(null)
                } catch (_: Exception) {}
            }
        }
    } else {
        // Show error when terminal view creation failed
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(BackgroundDark),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Terminal Error",
                    style = MaterialTheme.typography.titleMedium,
                    color = TerminalRed
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = viewError ?: "Could not initialise terminal view",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 32.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "The native terminal library (libtermux.so) may not be available on this device.",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 32.dp)
                )
            }
        }
    }
}

// ── Input helpers for SSH keyboard interception ─────────────────

private fun codePointToUtf8(codePoint: Int): ByteArray {
    if (codePoint !in 0x00..0x10FFFF) return ByteArray(0)
    if (codePoint in 0xD800..0xDFFF) return ByteArray(0) // surrogate range
    val chars = Character.toChars(codePoint)
    val utf8 = Charsets.UTF_8.newEncoder()
        .onMalformedInput(CodingErrorAction.REPLACE)
        .onUnmappableCharacter(CodingErrorAction.REPLACE)
        .encode(CharBuffer.wrap(chars))
    val bytes = ByteArray(utf8.remaining())
    utf8.get(bytes)
    return bytes
}

private fun keyEventToBytes(keyCode: Int, event: KeyEvent): ByteArray? {
    if (event.action != KeyEvent.ACTION_DOWN) return null
    return when (keyCode) {
        KeyEvent.KEYCODE_ENTER -> byteArrayOf(0x0D)
        KeyEvent.KEYCODE_DEL -> byteArrayOf(0x7F)
        KeyEvent.KEYCODE_TAB -> byteArrayOf(0x09)
        KeyEvent.KEYCODE_ESCAPE -> byteArrayOf(0x1B)
        KeyEvent.KEYCODE_DPAD_UP -> "\u001B[A".toByteArray()
        KeyEvent.KEYCODE_DPAD_DOWN -> "\u001B[B".toByteArray()
        KeyEvent.KEYCODE_DPAD_LEFT -> "\u001B[D".toByteArray()
        KeyEvent.KEYCODE_DPAD_RIGHT -> "\u001B[C".toByteArray()
        KeyEvent.KEYCODE_PAGE_UP -> "\u001B[5~".toByteArray()
        KeyEvent.KEYCODE_PAGE_DOWN -> "\u001B[6~".toByteArray()
        KeyEvent.KEYCODE_MOVE_HOME -> "\u001B[H".toByteArray()
        KeyEvent.KEYCODE_MOVE_END -> "\u001B[F".toByteArray()
        KeyEvent.KEYCODE_INSERT -> "\u001B[2~".toByteArray()
        KeyEvent.KEYCODE_FORWARD_DEL -> "\u001B[3~".toByteArray()
        KeyEvent.KEYCODE_F1 -> "\u001BOP".toByteArray()
        KeyEvent.KEYCODE_F2 -> "\u001BOQ".toByteArray()
        KeyEvent.KEYCODE_F3 -> "\u001BOR".toByteArray()
        KeyEvent.KEYCODE_F4 -> "\u001BOS".toByteArray()
        KeyEvent.KEYCODE_F5 -> "\u001B[15~".toByteArray()
        KeyEvent.KEYCODE_F6 -> "\u001B[17~".toByteArray()
        KeyEvent.KEYCODE_F7 -> "\u001B[18~".toByteArray()
        KeyEvent.KEYCODE_F8 -> "\u001B[19~".toByteArray()
        KeyEvent.KEYCODE_F9 -> "\u001B[20~".toByteArray()
        KeyEvent.KEYCODE_F10 -> "\u001B[21~".toByteArray()
        KeyEvent.KEYCODE_F11 -> "\u001B[23~".toByteArray()
        KeyEvent.KEYCODE_F12 -> "\u001B[24~".toByteArray()
        else -> null // regular chars handled by onCodePoint
    }
}
