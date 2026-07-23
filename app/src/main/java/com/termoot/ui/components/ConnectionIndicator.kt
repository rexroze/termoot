package com.termoot.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.termoot.domain.model.SessionState
import com.termoot.ui.theme.TerminalGreen
import com.termoot.ui.theme.TerminalRed
import com.termoot.ui.theme.TerminalYellow
import com.termoot.ui.theme.TextDisabled

/**
 * Small pulsing indicator dot that reflects a terminal session's [SessionState].
 *
 * @param state        current connection state
 * @param showLabel   whether to render a text label next to the dot
 * @param dotSize     diameter of the indicator dot
 */
@Composable
fun ConnectionIndicator(
    state: SessionState,
    showLabel: Boolean = false,
    modifier: Modifier = Modifier,
    dotSize: androidx.compose.ui.unit.Dp = 8.dp
) {
    val dotColor = when (state) {
        SessionState.CONNECTED   -> TerminalGreen
        SessionState.CONNECTING  -> TerminalYellow
        SessionState.DISCONNECTED -> TextDisabled
        SessionState.ERROR       -> TerminalRed
    }

    // Pulse animation only for CONNECTED state
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    val alpha = if (state == SessionState.CONNECTED) pulseAlpha else 1f

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(dotSize)
                .clip(CircleShape)
                .background(dotColor.copy(alpha = alpha))
        )

        if (showLabel) {
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = when (state) {
                    SessionState.CONNECTED   -> "Connected"
                    SessionState.CONNECTING  -> "Connecting…"
                    SessionState.DISCONNECTED -> "Disconnected"
                    SessionState.ERROR       -> "Error"
                },
                style = MaterialTheme.typography.labelSmall,
                color = dotColor
            )
        }
    }
}
