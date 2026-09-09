package com.example.game.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.game.model.PreyEntity
import com.example.game.model.SpiderEntity
import com.example.game.model.FrogEntity
import kotlin.math.*

/**
 * Directional D-Pad / Virtual Joystick with 4 directional arrows matching the reference UI.
 */
@Composable
fun VirtualJoystick(
    modifier: Modifier = Modifier,
    onMove: (x: Float, y: Float) -> Unit
) {
    var thumbOffset by remember { mutableStateOf(Offset.Zero) }
    val maxRadiusPx = 90f

    Box(
        modifier = modifier
            .size(130.dp)
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        Color(0x35000000),
                        Color(0x65000000)
                    )
                )
            )
            .border(1.5.dp, Color(0x66FFFFFF), CircleShape)
            .testTag("movement_joystick")
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        val center = Offset(size.width / 2f, size.height / 2f)
                        val delta = offset - center
                        val dist = delta.getDistance()
                        val clamped = if (dist > maxRadiusPx) delta * (maxRadiusPx / dist) else delta
                        thumbOffset = clamped
                        onMove(clamped.x / maxRadiusPx, -clamped.y / maxRadiusPx)
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        val next = thumbOffset + dragAmount
                        val dist = next.getDistance()
                        val clamped = if (dist > maxRadiusPx) next * (maxRadiusPx / dist) else next
                        thumbOffset = clamped
                        onMove(clamped.x / maxRadiusPx, -clamped.y / maxRadiusPx)
                    },
                    onDragEnd = {
                        thumbOffset = Offset.Zero
                        onMove(0f, 0f)
                    },
                    onDragCancel = {
                        thumbOffset = Offset.Zero
                        onMove(0f, 0f)
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        // 4 Directional Arrows (▲, ▼, ◀, ▶)
        Box(modifier = Modifier.fillMaxSize()) {
            Text(
                text = "▲",
                color = Color(0x99FFFFFF),
                fontSize = 11.sp,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 6.dp)
            )
            Text(
                text = "▼",
                color = Color(0x99FFFFFF),
                fontSize = 11.sp,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 6.dp)
            )
            Text(
                text = "◀",
                color = Color(0x99FFFFFF),
                fontSize = 11.sp,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 6.dp)
            )
            Text(
                text = "▶",
                color = Color(0x99FFFFFF),
                fontSize = 11.sp,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 6.dp)
            )
        }

        // Inner translucent track ring
        Box(
            modifier = Modifier
                .size(70.dp)
                .border(1.dp, Color(0x33FFFFFF), CircleShape)
        )

        // Draggable Center Thumb Knob
        Box(
            modifier = Modifier
                .offset { IntOffset(thumbOffset.x.roundToInt(), thumbOffset.y.roundToInt()) }
                .size(52.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xDD555555),
                            Color(0xBB222222)
                        )
                    )
                )
                .border(1.5.dp, Color(0xAAFFFFFF), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .background(Color(0x66FFFFFF), CircleShape)
            )
        }
    }
}

/**
 * Circular 3D Radar / Compass Minimap matching top right of reference image.
 */
@Composable
fun MinimapRadar(
    spider: SpiderEntity,
    frog: FrogEntity,
    preyList: List<PreyEntity>,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "sweep")
    val sweepAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sweepAngle"
    )

    Box(
        modifier = modifier
            .size(92.dp)
            .clip(CircleShape)
            .background(Color(0x660B1F14))
            .border(1.5.dp, Color(0x8800FF88), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val radius = size.width / 2f - 4f
            val maxRadarDist = 45f

            // Radar concentric rings
            drawCircle(
                color = Color(0x3300FF88),
                radius = radius * 0.5f,
                center = center,
                style = Stroke(width = 1f)
            )
            drawCircle(
                color = Color(0x4400FF88),
                radius = radius,
                center = center,
                style = Stroke(width = 1f)
            )

            // Crosshair lines
            drawLine(
                color = Color(0x2200FF88),
                start = Offset(center.x, 4f),
                end = Offset(center.x, size.height - 4f),
                strokeWidth = 1f
            )
            drawLine(
                color = Color(0x2200FF88),
                start = Offset(4f, center.y),
                end = Offset(size.width - 4f, center.y),
                strokeWidth = 1f
            )

            // Radar Sweep Line
            val rad = Math.toRadians(sweepAngle.toDouble())
            val sweepX = center.x + (radius * cos(rad)).toFloat()
            val sweepY = center.y + (radius * sin(rad)).toFloat()
            drawLine(
                color = Color(0x6600FF88),
                start = center,
                end = Offset(sweepX, sweepY),
                strokeWidth = 1.5f
            )

            // Draw Prey Dots (Green/Yellow)
            for (prey in preyList) {
                if (!prey.isAlive) continue
                val dx = prey.position.x - spider.position.x
                val dz = prey.position.z - spider.position.z
                val dist = sqrt(dx * dx + dz * dz)
                if (dist < maxRadarDist) {
                    val nx = (dx / maxRadarDist) * radius
                    val nz = (dz / maxRadarDist) * radius
                    val px = center.x + nx
                    val py = center.y + nz
                    drawCircle(
                        color = Color(0xFF00FF66),
                        radius = 2.5f,
                        center = Offset(px, py)
                    )
                }
            }

            // Draw Frog Dot (Red)
            val fdx = frog.position.x - spider.position.x
            val fdz = frog.position.z - spider.position.z
            val fDist = sqrt(fdx * fdx + fdz * fdz)
            if (fDist < maxRadarDist) {
                val fnx = (fdx / maxRadarDist) * radius
                val fnz = (fdz / maxRadarDist) * radius
                val fx = center.x + fnx
                val fy = center.y + fnz
                drawCircle(
                    color = Color(0xFFFF2222),
                    radius = 4.5f,
                    center = Offset(fx, fy)
                )
            }

            // Center Spider Marker (Cyan / Blue cone)
            val spiderYawRad = Math.toRadians(-spider.yawDeg.toDouble() + 180.0)
            val coneLen = 7f
            val tipX = center.x + (coneLen * sin(spiderYawRad)).toFloat()
            val tipY = center.y - (coneLen * cos(spiderYawRad)).toFloat()

            val path = Path().apply {
                moveTo(tipX, tipY)
                lineTo(
                    center.x + (4f * sin(spiderYawRad + 2.4)).toFloat(),
                    center.y - (4f * cos(spiderYawRad + 2.4)).toFloat()
                )
                lineTo(
                    center.x + (4f * sin(spiderYawRad - 2.4)).toFloat(),
                    center.y - (4f * cos(spiderYawRad - 2.4)).toFloat()
                )
                close()
            }
            drawPath(path = path, color = Color(0xFF00D2FF))
        }

        // Compass Cardinal Direction Labels (N, S, E, W)
        Box(modifier = Modifier.fillMaxSize()) {
            Text(
                text = "N",
                color = Color(0xCCFFFFFF),
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 1.dp)
            )
            Text(
                text = "S",
                color = Color(0xCCFFFFFF),
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 1.dp)
            )
            Text(
                text = "W",
                color = Color(0xCCFFFFFF),
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.CenterStart).padding(start = 2.dp)
            )
            Text(
                text = "E",
                color = Color(0xCCFFFFFF),
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.CenterEnd).padding(end = 2.dp)
            )
        }
    }
}

/**
 * Circular Action Button matching the 4 circular buttons in the reference screenshot:
 * EAT, SPIT, WEB, JUMP.
 */
@Composable
fun ActionButton(
    label: String,
    icon: String,
    primaryColor: Color,
    cooldownFraction: Float = 0f,
    isEnabled: Boolean = true,
    isHighlighted: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = if (isHighlighted) 1.15f else 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .size(54.dp)
                .scale(if (isHighlighted) pulseScale else 1f)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = if (isHighlighted) {
                            listOf(Color(0xFFFFA500), Color(0xCCFF4500))
                        } else {
                            listOf(Color(0x66222222), Color(0xCC000000))
                        }
                    )
                )
                .border(
                    width = if (isHighlighted) 2.5.dp else 1.2.dp,
                    color = if (isHighlighted) Color.White else Color(0x88FFFFFF),
                    shape = CircleShape
                )
                .clickable(enabled = isEnabled && cooldownFraction <= 0f) { onClick() }
                .testTag("action_button_${label.lowercase()}"),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = icon,
                fontSize = 22.sp
            )

            // Cooldown overlay sweep
            if (cooldownFraction > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0x99000000)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${(cooldownFraction * 4f).roundToInt()}s",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(3.dp))

        // Label Underneath (e.g. EAT, SPIT, WEB, JUMP)
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = if (isEnabled) Color.White else Color(0x88FFFFFF),
            letterSpacing = 1.sp
        )
    }
}

/**
 * Survival Bar (HEALTH, FOOD, ENERGY) matching top-left of screenshot.
 */
@Composable
fun SurvivalBar(
    icon: String,
    label: String,
    current: Float,
    max: Float,
    colorGradient: List<Color>,
    modifier: Modifier = Modifier
) {
    val progress = (current / max).coerceIn(0f, 1f)

    Column(modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = icon, fontSize = 11.sp)
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = label,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
            Text(
                text = "${current.toInt()}/${max.toInt()}",
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xEEFFFFFF)
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(Color(0x55000000))
                .border(0.5.dp, Color(0x33FFFFFF), RoundedCornerShape(3.dp))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(progress)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Brush.horizontalGradient(colorGradient))
            )
        }
    }
}
