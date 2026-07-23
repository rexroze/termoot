package com.termoot.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.termoot.domain.model.Workspace
import com.termoot.domain.model.WorkspaceType
import com.termoot.ui.theme.TerminalRed
import com.termoot.ui.theme.workspaceAccentColor

/**
 * Polished workspace card for the list screen.
 *
 * Layout:
 * ┌──────────────────────────────────────┐
 * │ ▌ Workspace Name                   > │
 * │ ▌ ●  Local Shell                    │
 * │ ▌ Last accessed: 2h ago             │
 * └──────────────────────────────────────┘
 *
 * The coloured bar on the left uses [Workspace.colorIndex] to pick an accent.
 *
 * @param workspace  The workspace to display.
 * @param onTap      Called when the card is tapped (navigate to terminal).
 * @param onEdit     Called when "Edit" is chosen from the long-press menu.
 * @param onDelete   Called when "Delete" is chosen from the long-press menu.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WorkspaceCard(
    workspace: Workspace,
    onTap: (Workspace) -> Unit,
    onEdit: (Workspace) -> Unit,
    onDelete: (Workspace) -> Unit,
    modifier: Modifier = Modifier
) {
    val accentColor = workspaceAccentColor(workspace.colorIndex)
    var isPressed by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    val haptics = LocalHapticFeedback.current

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = tween(durationMillis = 100),
        label = "cardScale"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .combinedClickable(
                onClick = { onTap(workspace) },
                onLongClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    showMenu = true
                }
            ),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
        ) {
            /* ── Accent colour bar ── */
            Box(
                modifier = Modifier
                    .width(5.dp)
                    .fillMaxHeight()
                    .background(accentColor)
            )

            /* ── Content ── */
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 14.dp, end = 12.dp, top = 14.dp, bottom = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    // Name
                    Text(
                        text = workspace.name,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Type badge row
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val (icon, label) = workspaceTypeInfo(workspace)
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = accentColor
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Last-accessed relative time
                    Text(
                        text = "Last accessed ${relativeTime(workspace.lastAccessed)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                /* ── Trailing chevron ── */
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "Open",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        /* ── Long-press context menu ── */
        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false }
        ) {
            DropdownMenuItem(
                text = { Text("Edit") },
                onClick = {
                    showMenu = false
                    onEdit(workspace)
                },
                leadingIcon = {
                    Icon(Icons.Default.Edit, contentDescription = null)
                }
            )
            DropdownMenuItem(
                text = {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                },
                onClick = {
                    showMenu = false
                    onDelete(workspace)
                },
                leadingIcon = {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            )
        }
    }
}

/* ------------------------------------------------------------------ */
/*  helpers                                                            */
/* ------------------------------------------------------------------ */

/**
 * Returns a human-friendly relative time string for display.
 * e.g. "30m ago", "2h ago", "3d ago"
 */
internal fun relativeTime(timestamp: Long): String {
    val diff = System.currentTimeMillis() - timestamp
    return when {
        diff < 0             -> "just now"
        diff < 60_000        -> "just now"
        diff < 3_600_000     -> "${diff / 60_000}m ago"
        diff < 86_400_000    -> "${diff / 3_600_000}h ago"
        diff < 604_800_000   -> "${diff / 86_400_000}d ago"
        diff < 2_592_000_000 -> "${diff / 604_800_000}w ago"
        else                 -> "${diff / 2_592_000_000}mo ago"
    }
}

/**
 * Resolves the type-specific icon and label for a workspace.
 */
internal fun workspaceTypeInfo(workspace: Workspace): Pair<ImageVector, String> {
    return when (workspace.type) {
        WorkspaceType.LOCAL_SHELL -> Icons.Default.Terminal to "Local Shell"
        WorkspaceType.PROOT_DISTRO -> Icons.Default.Computer to (workspace.distroName ?: "Linux")
        WorkspaceType.SSH -> Icons.Default.Dns to buildString {
            if (!workspace.sshUser.isNullOrBlank()) append("${workspace.sshUser}@")
            append(workspace.sshHost ?: "host")
        }
    }
}
