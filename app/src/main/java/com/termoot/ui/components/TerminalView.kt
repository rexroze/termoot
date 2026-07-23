package com.termoot.ui.components

import android.graphics.Typeface
import android.util.Log
import android.view.View
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
    modifier: Modifier = Modifier
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
                } catch (e: Exception) {
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
