package com.termoot.ui.screens.editor

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.termoot.domain.model.WorkspaceType
import com.termoot.ui.theme.BackgroundDark
import com.termoot.ui.theme.BorderDark
import com.termoot.ui.theme.SurfaceDark
import com.termoot.ui.theme.SurfaceVariantDark
import com.termoot.ui.theme.TerminalBlue
import com.termoot.ui.theme.TerminalGreen
import com.termoot.ui.theme.TerminalRed
import com.termoot.ui.theme.TextDisabled
import com.termoot.ui.theme.TextSecondary
import com.termoot.ui.theme.WorkspaceAccentColors

// Pre-defined proot distros for the dropdown
private val KNOWN_DISTROS = listOf(
    "ubuntu", "debian", "archlinux", "kali", "fedora", "alpine",
    "manjaro", "void", "opensuse", "gentoo", "nixos", "devuan"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkspaceEditorScreen(
    workspaceId: String?,
    onNavigateBack: () -> Unit,
    viewModel: WorkspaceEditorViewModel = viewModel(
        factory = WorkspaceEditorViewModel.Factory(workspaceId)
    )
) {
    val saved by viewModel.saved.collectAsState(initial = false)
    var showDeleteDialog by remember { mutableStateOf(false) }

    // Navigate back when saved
    LaunchedEffect(saved) {
        if (saved) onNavigateBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (viewModel.isEditing) "Edit Workspace" else "New Workspace"
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BackgroundDark,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        containerColor = BackgroundDark
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            /* ── Name ── */
            SectionLabel("Name")
            OutlinedTextField(
                value = viewModel.name,
                onValueChange = viewModel::updateName,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("My Workspace", color = TextDisabled) },
                singleLine = true,
                isError = viewModel.nameError,
                supportingText = if (viewModel.nameError) {
                    { Text("Name is required") }
                } else null,
                shape = MaterialTheme.shapes.small,
                colors = editorFieldColors()
            )

            Spacer(modifier = Modifier.height(20.dp))

            /* ── Type selector ── */
            SectionLabel("Type")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    Triple(WorkspaceType.LOCAL_SHELL, "Local Shell", Icons.Default.Terminal),
                    Triple(WorkspaceType.PROOT_DISTRO, "Proot", Icons.Default.Computer),
                    Triple(WorkspaceType.SSH, "SSH", Icons.Default.Dns)
                ).forEach { (t, label, icon) ->
                    FilterChip(
                        selected = viewModel.type == t,
                        onClick = { viewModel.updateType(t) },
                        label = { Text(label) },
                        leadingIcon = {
                            Icon(
                                icon,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = TerminalGreen.copy(alpha = 0.12f),
                            selectedLabelColor = TerminalGreen,
                            selectedLeadingIconColor = TerminalGreen
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = viewModel.type == t,
                            borderColor = BorderDark,
                            selectedBorderColor = TerminalGreen.copy(alpha = 0.5f)
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            /* ── PROOT_DISTRO fields ── */
            AnimatedVisibility(
                visible = viewModel.type == WorkspaceType.PROOT_DISTRO,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column {
                    SectionLabel("Distribution")
                    DistroDropdown(
                        selected = viewModel.distroName,
                        onSelected = viewModel::updateDistro
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                }
            }

            /* ── SSH fields ── */
            AnimatedVisibility(
                visible = viewModel.type == WorkspaceType.SSH,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column {
                    SshFields(viewModel)
                    Spacer(modifier = Modifier.height(20.dp))
                }
            }

            /* ── Color picker ── */
            SectionLabel("Accent Color")
            Spacer(modifier = Modifier.height(8.dp))
            ColorPickerRow(
                selectedIndex = viewModel.colorIndex,
                onSelect = viewModel::updateColorIndex
            )

            Spacer(modifier = Modifier.height(32.dp))

            /* ── Save button ── */
            Button(
                onClick = viewModel::saveWorkspace,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = MaterialTheme.shapes.small,
                colors = ButtonDefaults.buttonColors(
                    containerColor = TerminalGreen,
                    contentColor = Color(0xFF0D1117)
                )
            ) {
                Text(
                    "Save Workspace",
                    style = MaterialTheme.typography.titleSmall
                )
            }

            /* ── Delete button (edit mode only) ── */
            if (viewModel.isEditing) {
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(
                    onClick = { showDeleteDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = MaterialTheme.shapes.small,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = TerminalRed
                    ),
                    border = BorderStroke(1.dp, TerminalRed.copy(alpha = 0.5f))
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Delete Workspace",
                        style = MaterialTheme.typography.titleSmall
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    /* ── Delete confirmation dialog ── */
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Workspace") },
            text = { Text("Delete \"${viewModel.name}\"? This cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        viewModel.deleteWorkspace()
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/* ════════════════════════════════════════════════════════════════ */
/*  Sub-components                                                   */
/* ════════════════════════════════════════════════════════════════ */

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = TextSecondary,
        modifier = Modifier.padding(bottom = 6.dp)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DistroDropdown(
    selected: String,
    onSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = selected,
            onValueChange = onSelected,
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
            placeholder = { Text("Select distro", color = TextDisabled) },
            singleLine = true,
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            shape = MaterialTheme.shapes.small,
            colors = editorFieldColors()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            KNOWN_DISTROS.forEach { distro ->
                DropdownMenuItem(
                    text = { Text(distro) },
                    onClick = {
                        onSelected(distro)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun SshFields(viewModel: WorkspaceEditorViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Host + Port row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = viewModel.sshHost,
                onValueChange = viewModel::updateSshHost,
                modifier = Modifier.weight(2f),
                label = { Text("Host") },
                placeholder = { Text("example.com", color = TextDisabled) },
                singleLine = true,
                shape = MaterialTheme.shapes.small,
                colors = editorFieldColors()
            )
            OutlinedTextField(
                value = viewModel.sshPort,
                onValueChange = viewModel::updateSshPort,
                modifier = Modifier.weight(1f),
                label = { Text("Port") },
                placeholder = { Text("22", color = TextDisabled) },
                singleLine = true,
                shape = MaterialTheme.shapes.small,
                colors = editorFieldColors(),
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    keyboardType = KeyboardType.Number
                )
            )
        }

        OutlinedTextField(
            value = viewModel.sshUser,
            onValueChange = viewModel::updateSshUser,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Username") },
            placeholder = { Text("root", color = TextDisabled) },
            singleLine = true,
            shape = MaterialTheme.shapes.small,
            colors = editorFieldColors()
        )

        // Auth method toggle
        val methods = listOf(
            WorkspaceEditorViewModel.SshAuthMethod.PASSWORD to "Password",
            WorkspaceEditorViewModel.SshAuthMethod.KEY to "Key file"
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            methods.forEach { (method, label) ->
                FilterChip(
                    selected = viewModel.sshAuthMethod == method,
                    onClick = { viewModel.updateSshAuthMethod(method) },
                    label = { Text(label) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = TerminalBlue.copy(alpha = 0.12f),
                        selectedLabelColor = TerminalBlue,
                        selectedLeadingIconColor = TerminalBlue
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = viewModel.sshAuthMethod == method,
                        borderColor = BorderDark,
                        selectedBorderColor = TerminalBlue.copy(alpha = 0.5f)
                    )
                )
            }
        }

        // Password field (conditional)
        AnimatedVisibility(
            visible = viewModel.sshAuthMethod == WorkspaceEditorViewModel.SshAuthMethod.PASSWORD,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            var visible by rememberSaveable { mutableStateOf(false) }
            OutlinedTextField(
                value = viewModel.sshPassword,
                onValueChange = viewModel::updateSshPassword,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Password") },
                singleLine = true,
                shape = MaterialTheme.shapes.small,
                colors = editorFieldColors(),
                visualTransformation = if (visible) VisualTransformation.None
                                       else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { visible = !visible }) {
                        Icon(
                            if (visible) Icons.Default.VisibilityOff
                            else Icons.Default.Visibility,
                            contentDescription = if (visible) "Hide password"
                                                 else "Show password"
                        )
                    }
                }
            )
        }

        // Key path field (conditional)
        AnimatedVisibility(
            visible = viewModel.sshAuthMethod == WorkspaceEditorViewModel.SshAuthMethod.KEY,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            OutlinedTextField(
                value = viewModel.sshKeyPath,
                onValueChange = viewModel::updateSshKeyPath,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Private Key Path") },
                placeholder = {
                    Text(
                        "/data/data/com.termoot/files/ssh/id_ed25519",
                        color = TextDisabled
                    )
                },
                singleLine = true,
                shape = MaterialTheme.shapes.small,
                colors = editorFieldColors()
            )
        }
    }
}

@Composable
private fun ColorPickerRow(
    selectedIndex: Int,
    onSelect: (Int) -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        WorkspaceAccentColors.forEachIndexed { index, color ->
            val isSelected = index == selectedIndex
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(color)
                    .then(
                        if (isSelected) {
                            Modifier.border(
                                2.dp,
                                MaterialTheme.colorScheme.onSurface,
                                CircleShape
                            )
                        } else Modifier
                    )
                    .clickable { onSelect(index) },
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = "Selected",
                        tint = Color(0xFF0D1117),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

/* ── Reusable text field colours ── */

@Composable
private fun editorFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = TerminalGreen,
    unfocusedBorderColor = SurfaceVariantDark,
    cursorColor = TerminalGreen,
    focusedTextColor = MaterialTheme.colorScheme.onSurface,
    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
    focusedLabelColor = TextSecondary,
    unfocusedLabelColor = TextDisabled,
    focusedContainerColor = SurfaceDark,
    unfocusedContainerColor = SurfaceDark,
    errorBorderColor = TerminalRed,
    errorCursorColor = TerminalRed,
    errorLabelColor = TerminalRed,
    errorContainerColor = SurfaceDark
)
