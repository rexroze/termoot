package com.termoot.ui.screens.terminal

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.termoot.domain.model.Session
import com.termoot.domain.model.SessionState
import com.termoot.ui.components.ConnectionIndicator
import com.termoot.ui.components.TerminalView
import com.termoot.ui.theme.BackgroundDark
import com.termoot.ui.theme.SurfaceDark
import com.termoot.ui.theme.SurfaceVariantDark
import com.termoot.ui.theme.TerminalGreen
import com.termoot.ui.theme.TerminalRed
import com.termoot.ui.theme.TextDisabled
import com.termoot.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TerminalScreen(
    workspaceId: String,
    onNavigateBack: () -> Unit,
    viewModel: TerminalViewModel = viewModel(
        factory = TerminalViewModel.Factory(workspaceId)
    )
) {
    val workspace by viewModel.workspace.collectAsState()
    val sessions by viewModel.sessions.collectAsState()
    val activeSessionId by viewModel.activeSessionId.collectAsState()
    var showKeySheet by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TerminalTopBar(
                workspaceName = workspace?.name ?: "Terminal",
                onNavigateBack = onNavigateBack,
                sessions = sessions,
                activeSessionId = activeSessionId,
                onTabSelected = viewModel::setActiveSession,
                onTabClosed = viewModel::closeSession,
                onNewTab = {
                    workspace?.let { viewModel.openSession(it) }
                }
            )
        },
        bottomBar = {
            TerminalBottomToolbar(
                canSendKeys = sessions.isNotEmpty(),
                onSendKeys = { showKeySheet = true },
                canCopy = false,
                canPaste = false,
                isConnected = sessions.any { it.isActive && it.state == SessionState.CONNECTED },
                onDisconnect = {}
            )
        },
        containerColor = BackgroundDark
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(BackgroundDark)
        ) {
            if (sessions.isEmpty()) {
                /* ── Empty state ── */
                EmptyTerminalState(
                    workspaceName = workspace?.name ?: "Workspace",
                    onConnect = {
                        workspace?.let { viewModel.openSession(it) }
                    }
                )
            } else {
                /* ── Terminal content ── */
                val activeSession = sessions.find { it.id == activeSessionId }
                Column(modifier = Modifier.fillMaxSize()) {
                    // Connection status row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(SurfaceDark)
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        activeSession?.let { session ->
                            ConnectionIndicator(
                                state = session.state,
                                showLabel = true,
                                dotSize = 6.dp
                            )
                        }
                        Spacer(modifier = Modifier.weight(1f))
                        activeSession?.let { session ->
                            Text(
                                text = session.name,
                                style = MaterialTheme.typography.labelMedium,
                                color = TextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // The terminal view — sharp corners, full remaining space
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(BackgroundDark)
                    ) {
                        // For the MVP stub: show a placeholder when we don't have
                        // a real Termux TerminalSession. The real integration will
                        // pass an actual com.termux.terminal.TerminalSession here.
                        if (activeSession?.state == SessionState.CONNECTED) {
                            TerminalView(
                                terminalSession = null, // TODO: pass real Termux session
                                modifier = Modifier.fillMaxSize()
                            )
                        } else if (activeSession?.state == SessionState.ERROR) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "Connection Error",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = TerminalRed
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    activeSession.errorMessage?.let { msg ->
                                        Text(
                                            text = msg,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TextSecondary,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.padding(horizontal = 32.dp)
                                        )
                                    }
                                }
                            }
                        } else {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (activeSession?.state == SessionState.CONNECTING)
                                            "Connecting\u2026" else "Initializing\u2026",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    /* ── Send-keys bottom sheet ── */
    if (showKeySheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showKeySheet = false },
            sheetState = sheetState,
            containerColor = SurfaceDark,
            shape = MaterialTheme.shapes.large
        ) {
            KeySendSheet(
                onKey = { /* TODO: send key to terminal */ },
                onDismiss = { showKeySheet = false }
            )
        }
    }
}

/* ════════════════════════════════════════════════════════════════ */
/*  Top bar with session tabs                                      */
/* ════════════════════════════════════════════════════════════════ */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TerminalTopBar(
    workspaceName: String,
    onNavigateBack: () -> Unit,
    sessions: List<Session>,
    activeSessionId: String?,
    onTabSelected: (String) -> Unit,
    onTabClosed: (String) -> Unit,
    onNewTab: () -> Unit
) {
    Column {
        TopAppBar(
            title = {
                Text(
                    text = workspaceName,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back to workspaces"
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = BackgroundDark,
                titleContentColor = MaterialTheme.colorScheme.onSurface,
                navigationIconContentColor = MaterialTheme.colorScheme.onSurface
            )
        )

        // Tab bar — only when we have sessions
        if (sessions.isNotEmpty()) {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SurfaceDark)
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items(sessions, key = { it.id }) { session ->
                    SessionTab(
                        session = session,
                        isActive = session.id == activeSessionId,
                        onSelect = { onTabSelected(session.id) },
                        onClose = { onTabClosed(session.id) }
                    )
                }

                // "New tab" plus button
                item {
                    IconButton(
                        onClick = onNewTab,
                        modifier = Modifier
                            .padding(start = 4.dp)
                            .size(36.dp)
                    ) {
                        Icon(
                            Icons.Default.Terminal,
                            contentDescription = "New session",
                            tint = TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SessionTab(
    session: Session,
    isActive: Boolean,
    onSelect: () -> Unit,
    onClose: () -> Unit
) {
    val bgColor by animateColorAsState(
        targetValue = if (isActive) SurfaceVariantDark else Color.Transparent,
        animationSpec = tween(200),
        label = "tabBg"
    )
    val borderAlpha by animateFloatAsState(
        targetValue = if (isActive) 1f else 0f,
        animationSpec = tween(200),
        label = "tabBorder"
    )

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
            .background(bgColor)
            .clickable(onClick = onSelect)
            .padding(start = 12.dp, end = 4.dp, top = 8.dp, bottom = 8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Session type indicator
            Icon(
                Icons.Default.Terminal,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = if (isActive) TerminalGreen else TextDisabled
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = session.name,
                style = MaterialTheme.typography.labelMedium,
                color = if (isActive) MaterialTheme.colorScheme.onSurface else TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.widthIn(max = 100.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            IconButton(
                onClick = onClose,
                modifier = Modifier.size(20.dp)
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Close tab",
                    tint = TextDisabled,
                    modifier = Modifier.size(14.dp)
                )
            }
        }

        // Active indicator (green bottom border)
        if (isActive) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .align(Alignment.BottomCenter)
                    .background(TerminalGreen)
            )
        }
    }
}

/* ════════════════════════════════════════════════════════════════ */
/*  Bottom toolbar                                                 */
/* ════════════════════════════════════════════════════════════════ */

@Composable
private fun TerminalBottomToolbar(
    canSendKeys: Boolean,
    onSendKeys: () -> Unit,
    canCopy: Boolean,
    canPaste: Boolean,
    isConnected: Boolean,
    onDisconnect: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceDark)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        ToolbarButton(
            icon = Icons.Default.Keyboard,
            label = "Keys",
            enabled = canSendKeys,
            onClick = onSendKeys
        )
        ToolbarButton(
            icon = Icons.Default.ContentCopy,
            label = "Copy",
            enabled = canCopy,
            onClick = { /* TODO */ }
        )
        ToolbarButton(
            icon = Icons.Default.ContentPaste,
            label = "Paste",
            enabled = canPaste,
            onClick = { /* TODO */ }
        )
        ToolbarButton(
            icon = Icons.Default.PowerSettingsNew,
            label = if (isConnected) "Disconnect" else "Reconnect",
            enabled = false,
            onClick = onDisconnect,
            tint = if (isConnected) TerminalGreen else TextDisabled
        )
    }
}

@Composable
private fun ToolbarButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
    tint: Color = if (enabled) MaterialTheme.colorScheme.onSurface else TextDisabled
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Icon(
            icon,
            contentDescription = label,
            tint = tint,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = tint
        )
    }
}

/* ════════════════════════════════════════════════════════════════ */
/*  Key-send bottom sheet                                          */
/* ════════════════════════════════════════════════════════════════ */

@Composable
private fun KeySendSheet(
    onKey: (String) -> Unit,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        Text(
            text = "Send Keys",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Modifier keys row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("Ctrl", "Alt", "Shift", "Meta", "Tab", "Esc").forEach { key ->
                androidx.compose.material3.SuggestionChip(
                    onClick = { onKey(key) },
                    label = { Text(key, style = MaterialTheme.typography.labelMedium) },
                    shape = MaterialTheme.shapes.small
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Arrow keys
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            ArrowKeyButton(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Left", onKey)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                ArrowKeyButton(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "PgUp", onKey)
                ArrowKeyButton(Icons.AutoMirrored.Filled.KeyboardArrowRight, "PgDn", onKey)
            }
            ArrowKeyButton(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Right", onKey)
        }

        Spacer(modifier = Modifier.height(8.dp))
        ArrowKeyButton(Icons.Default.KeyboardArrowDown, "Down", onKey)

        Spacer(modifier = Modifier.height(24.dp))

        TextButton(
            onClick = onDismiss,
            modifier = Modifier.align(Alignment.End)
        ) {
            Text("Done")
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun ArrowKeyButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onKey: (String) -> Unit
) {
    IconButton(onClick = { onKey(label) }) {
        Icon(icon, contentDescription = label, tint = MaterialTheme.colorScheme.onSurface)
    }
}

/* ════════════════════════════════════════════════════════════════ */
/*  Empty state                                                     */
/* ════════════════════════════════════════════════════════════════ */

@Composable
private fun EmptyTerminalState(
    workspaceName: String,
    onConnect: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Default.Terminal,
                contentDescription = null,
                modifier = Modifier.size(72.dp),
                tint = TextDisabled
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "No active session",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Connect to $workspaceName to get started",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(24.dp))
            TextButton(onClick = onConnect) {
                Icon(
                    Icons.Default.PowerSettingsNew,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Connect", color = TerminalGreen)
            }
        }
    }
}
