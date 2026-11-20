package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.DashboardToolEntity
import com.example.data.local.LanguageMode
import com.example.data.local.ToolActionType
import com.example.ui.components.StatusMicroChip
import com.example.ui.theme.AbyssalVoid
import com.example.ui.theme.ActiveEmerald
import com.example.ui.theme.ArcCyan
import com.example.ui.theme.CyberGlassBorder
import com.example.ui.theme.CyberNavy
import com.example.ui.theme.CyberSlate
import com.example.ui.theme.EmergencyCrimson
import com.example.ui.theme.IceBlueText
import com.example.ui.theme.MutedSteelText

@Composable
fun DashboardScreen(
    tools: List<DashboardToolEntity>,
    languageMode: LanguageMode,
    isTorchOn: Boolean,
    isAutoScrolling: Boolean,
    onLaunchTool: (DashboardToolEntity) -> Unit,
    onAddToolClick: () -> Unit,
    onRemoveTool: (DashboardToolEntity) -> Unit,
    onRestoreDefaults: () -> Unit,
    modifier: Modifier = Modifier
) {
    var editMode by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "VIP COMMAND DECK",
                    style = MaterialTheme.typography.titleLarge,
                    color = ArcCyan
                )
                Text(
                    text = if (languageMode == LanguageMode.HINDI) {
                        "डैशबोर्ड से टूल जोड़ें, हटाएं या लॉन्च करें"
                    } else {
                        "Add, remove, or launch smart phone tools"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MutedSteelText
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = editMode,
                    onClick = { editMode = !editMode },
                    label = { Text(if (editMode) "Done" else "Edit") },
                    leadingIcon = {
                        Icon(
                            Icons.Filled.Edit,
                            contentDescription = "Edit dashboard tools",
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    modifier = Modifier.testTag("edit_tools_button")
                )

                Button(
                    onClick = onAddToolClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ArcCyan,
                        contentColor = AbyssalVoid
                    ),
                    shape = RoundedCornerShape(14.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("add_tool_button")
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Add tool", modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Add", style = MaterialTheme.typography.labelLarge)
                }
            }
        }

        if (tools.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "No tools on dashboard",
                        style = MaterialTheme.typography.titleMedium,
                        color = IceBlueText
                    )
                    OutlinedButton(onClick = onRestoreDefaults) {
                        Icon(Icons.Filled.Refresh, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Restore Default VIP Tools")
                    }
                }
            }
        } else {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val columns = when {
                    maxWidth >= 840.dp -> 4
                    maxWidth >= 560.dp -> 3
                    else -> 2
                }
                LazyVerticalGrid(
                    columns = GridCells.Fixed(columns),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(tools, key = { it.id }) { tool ->
                        DashboardToolCard(
                            tool = tool,
                            showHindi = languageMode == LanguageMode.HINDI,
                            editMode = editMode,
                            isActiveHighlight = (tool.actionType == ToolActionType.FLASHLIGHT_TOGGLE.name && isTorchOn) ||
                                (tool.actionType == ToolActionType.AUTO_SCROLL.name && isAutoScrolling),
                            onClick = { onLaunchTool(tool) },
                            onDelete = { onRemoveTool(tool) }
                        )
                    }
                    if (editMode) {
                        item {
                            Surface(
                                onClick = onRestoreDefaults,
                                color = CyberSlate,
                                shape = RoundedCornerShape(18.dp),
                                border = BorderStroke(1.dp, ArcCyan.copy(alpha = 0.5f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(132.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.Center,
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        Icons.Filled.Refresh,
                                        contentDescription = "Restore default tools",
                                        tint = ArcCyan
                                    )
                                    Spacer(Modifier.height(6.dp))
                                    Text(
                                        text = "Restore Defaults",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = ArcCyan
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DashboardToolCard(
    tool: DashboardToolEntity,
    showHindi: Boolean,
    editMode: Boolean,
    isActiveHighlight: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        onClick = { if (!editMode) onClick() },
        color = if (isActiveHighlight) ActiveEmerald.copy(alpha = 0.14f) else CyberNavy,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(
            1.dp,
            when {
                editMode -> EmergencyCrimson.copy(alpha = 0.7f)
                isActiveHighlight -> ActiveEmerald
                else -> CyberGlassBorder
            }
        ),
        tonalElevation = 4.dp,
        modifier = Modifier
            .fillMaxWidth()
            .height(138.dp)
            .testTag("tool_card_${tool.id}")
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Surface(
                        color = CyberSlate,
                        shape = CircleShape,
                        border = BorderStroke(1.dp, ArcCyan.copy(alpha = 0.45f)),
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = iconForTool(tool.actionType),
                                contentDescription = tool.title,
                                tint = if (isActiveHighlight) ActiveEmerald else ArcCyan,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    if (editMode) {
                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("remove_tool_${tool.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "Remove ${tool.title}",
                                tint = EmergencyCrimson
                            )
                        }
                    } else if (tool.requiresConfirmation) {
                        StatusMicroChip(label = "CONFIRM", color = ArcCyan)
                    }
                }

                Column {
                    Text(
                        text = if (showHindi) tool.titleHi else tool.title,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = IceBlueText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (showHindi) tool.subtitleHi else tool.subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MutedSteelText,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

private fun iconForTool(actionType: String): ImageVector {
    return when (actionType) {
        ToolActionType.WHATSAPP_COMPOSE.name -> Icons.Filled.Chat
        ToolActionType.PHONE_CALL.name -> Icons.Filled.Call
        ToolActionType.CAMERA_CONTROL.name -> Icons.Filled.CameraAlt
        ToolActionType.FLASHLIGHT_TOGGLE.name -> Icons.Filled.FlashlightOn
        ToolActionType.YOUTUBE_SEARCH.name -> Icons.Filled.PlayCircle
        ToolActionType.WEB_SEARCH.name -> Icons.Filled.Search
        ToolActionType.INSTAGRAM_UPLOAD.name -> Icons.Filled.PhotoCamera
        ToolActionType.FACEBOOK_UPLOAD.name, ToolActionType.SOCIAL_POST_AI.name -> Icons.Filled.Share
        ToolActionType.VOICE_TYPING.name -> Icons.Filled.Mic
        ToolActionType.AUTO_SCROLL.name -> Icons.Filled.SwapVert
        ToolActionType.CUSTOM_URL.name -> Icons.Filled.Link
        else -> Icons.Filled.Bolt
    }
}
