package com.termoot.ui.components

import android.graphics.Typeface
import android.util.TypedValue
import android.view.View
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.termoot.ui.theme.BackgroundDark

/**
 * Composable wrapper around Termux's [TerminalView].
 *
 * Renders an [AndroidView] hosting the native terminal widget with proper
 * dark theme colours.  The view is deliberately **not** clipped to a rounded
 * shape — terminal emulators should have sharp corners for an authentic feel.
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

    val terminalView = remember {
        com.termux.view.TerminalView(context).apply {
            id = View.generateViewId()
            setBackgroundColor(BackgroundDark.toArgb())
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            setTypeface(Typeface.MONOSPACE)
        }
    }

    AndroidView(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundDark),
        factory = { terminalView },
        update = { view ->
            view.attachSession(terminalSession)
        }
    )

    // Ensure the session is detached when this composable leaves composition
    DisposableEffect(Unit) {
        onDispose {
            terminalView.attachSession(null)
        }
    }
}
