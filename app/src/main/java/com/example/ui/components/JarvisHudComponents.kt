package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.StopCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.AbyssalVoid
import com.example.ui.theme.ActiveEmerald
import com.example.ui.theme.ArcCyan
import com.example.ui.theme.CyberGlassBorder
import com.example.ui.theme.CyberNavy
import com.example.ui.theme.CyberSlate
import com.example.ui.theme.EmergencyCrimson
import com.example.ui.theme.EmergencyCrimsonDark
import com.example.ui.theme.IceBlueText
import com.example.ui.theme.MutedSteelText
import com.example.ui.theme.SapphireCore

@Composable
fun JarvisTopHudBar(
    isEmergencyStopped: Boolean,
    isAccessibilityConnected: Boolean,
    isWakeModeActive: Boolean,
    isThinking: Boolean,
    onEmergencyStop: () -> Unit,
    onResumeSystem: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        color = CyberNavy.copy(alpha = 0.92f),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(
            1.dp,
            if (isEmergencyStopped) EmergencyCrimson else CyberGlassBorder
        ),
        tonalElevation = 6.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                listOf(
                                    if (isEmergencyStopped) EmergencyCrimson else ArcCyan,
                                    CyberSlate
                                )
                            )
                        )
                        .border(1.dp, if (isEmergencyStopped) EmergencyCrimson else ArcCyan, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Shield,
                        contentDescription = "JARVIS VIP Core",
                        tint = IceBlueText,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "JARVIS VIP",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = IceBlueText,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        StatusMicroChip(
                            label = if (isEmergencyStopped) "HALTED" else if (isThinking) "GEMINI" else "ONLINE",
                            color = when {
                                isEmergencyStopped -> EmergencyCrimson
                                isThinking -> SapphireCore
                                else -> ActiveEmerald
                            }
                        )
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isAccessibilityConnected) "A11y: ON" else "A11y: OFF",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isAccessibilityConnected) ActiveEmerald else MutedSteelText
                        )
                        Text(
                            text = "•",
                            style = MaterialTheme.typography.labelSmall,
                            color = MutedSteelText
                        )
                        Text(
                            text = if (isWakeModeActive) "Wake: Hey JARVIS" else "Wake: Tap",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isWakeModeActive) ArcCyan else MutedSteelText,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            if (isEmergencyStopped) {
                Button(
                    onClick = onResumeSystem,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ActiveEmerald,
                        contentColor = AbyssalVoid
                    ),
                    shape = RoundedCornerShape(14.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("resume_system_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = "Resume system",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "RESUME",
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            } else {
                Button(
                    onClick = onEmergencyStop,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EmergencyCrimson,
                        contentColor = IceBlueText
                    ),
                    shape = RoundedCornerShape(14.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("emergency_stop_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.StopCircle,
                        contentDescription = "Emergency Stop all actions",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "STOP",
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }
}

@Composable
fun StatusMicroChip(
    label: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = color.copy(alpha = 0.16f),
        shape = RoundedCornerShape(6.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.6f))
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@Composable
fun ArcReactorCoreOrb(
    isListening: Boolean,
    isThinking: Boolean,
    isSpeaking: Boolean,
    isEmergencyStopped: Boolean,
    rmsLevel: Float,
    onClick: () -> Unit,
    orbSize: Dp = 112.dp,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "jarvis_core_anim")
    val outerAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (isThinking) 2400 else 9000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "outer_angle"
    )
    val innerAngle by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (isSpeaking || isListening) 2600 else 6500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "inner_angle"
    )
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.88f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    val coreColor by animateColorAsState(
        targetValue = when {
            isEmergencyStopped -> EmergencyCrimson
            isListening -> ActiveEmerald
            isSpeaking -> SapphireCore
            else -> ArcCyan
        },
        label = "core_color"
    )

    val dynamicBoost = if (isListening) (rmsLevel * 0.16f) else 0f

    Box(
        modifier = modifier
            .size(orbSize)
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .testTag("arc_core_orb"),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val radius = size.minDimension / 2f
            val center = Offset(size.width / 2f, size.height / 2f)

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        coreColor.copy(alpha = 0.35f + dynamicBoost),
                        coreColor.copy(alpha = 0.08f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = radius
                )
            )

            rotate(outerAngle, pivot = center) {
                val strokeW = 3.dp.toPx()
                val arcRadius = radius * 0.86f
                val topLeft = Offset(center.x - arcRadius, center.y - arcRadius)
                val arcSize = Size(arcRadius * 2, arcRadius * 2)
                for (i in 0 until 3) {
                    drawArc(
                        color = coreColor.copy(alpha = 0.85f),
                        startAngle = i * 120f + 12f,
                        sweepAngle = 78f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeW, cap = StrokeCap.Round)
                    )
                }
            }

            rotate(innerAngle, pivot = center) {
                val strokeW = 2.5.dp.toPx()
                val arcRadius = radius * 0.66f * (pulse + dynamicBoost * 0.4f).coerceAtMost(1.08f)
                val topLeft = Offset(center.x - arcRadius, center.y - arcRadius)
                val arcSize = Size(arcRadius * 2, arcRadius * 2)
                for (i in 0 until 4) {
                    drawArc(
                        color = IceBlueText.copy(alpha = 0.7f),
                        startAngle = i * 90f + 10f,
                        sweepAngle = 52f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeW, cap = StrokeCap.Round)
                    )
                }
            }

            drawCircle(
                color = if (isEmergencyStopped) EmergencyCrimsonDark else CyberNavy,
                radius = radius * 0.42f,
                center = center
            )
            drawCircle(
                color = coreColor,
                radius = radius * 0.42f,
                center = center,
                style = Stroke(width = 2.dp.toPx())
            )
        }

        Icon(
            imageVector = if (isEmergencyStopped) Icons.Filled.StopCircle else Icons.Filled.GraphicEq,
            contentDescription = if (isListening) "Listening" else "Activate JARVIS Voice",
            tint = coreColor,
            modifier = Modifier.size(28.dp)
        )
    }
}
