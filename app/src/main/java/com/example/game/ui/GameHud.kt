package com.example.game.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.game.engine.SpiderRenderer
import com.example.game.model.*
import kotlinx.coroutines.delay

/**
 * Main Game HUD overlay for SPIDER HUNT matching the reference UI layout.
 */
@Composable
fun GameHud(
    renderer: SpiderRenderer,
    onPauseClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val spider = renderer.spider
    val frog = renderer.frog
    val session = renderer.session

    // Continuous tick for UI updates
    var tick by remember { mutableStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(50)
            tick++
        }
    }

    val spiderHealth = spider.health
    val spiderFood = spider.food
    val spiderEnergy = spider.energy
    val targetPrey = spider.findTargetToEat(renderer.preyList)

    val distToFrog = spider.position.distanceTo(frog.position)
    val isFrogNear = distToFrog < 28f
    val isFrogChasing = frog.state == FrogState.CHASE || frog.state == FrogState.ATTACK

    var showWeatherPicker by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // ==========================================
        // 1. TOP-LEFT: SURVIVAL BARS (Health, Food, Energy)
        // ==========================================
        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 14.dp, top = 10.dp)
                .width(160.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // HEALTH
            SurvivalBar(
                icon = "❤️",
                label = "HEALTH",
                current = spiderHealth,
                max = spider.maxHealth,
                colorGradient = listOf(Color(0xFFFF2244), Color(0xFFFF5566)),
                modifier = Modifier.fillMaxWidth()
            )

            // FOOD
            SurvivalBar(
                icon = "🍗",
                label = "FOOD",
                current = spiderFood,
                max = spider.maxFood,
                colorGradient = listOf(Color(0xFFFF7700), Color(0xFFFFB300)),
                modifier = Modifier.fillMaxWidth()
            )

            // ENERGY
            SurvivalBar(
                icon = "⚡",
                label = "ENERGY",
                current = spiderEnergy,
                max = spider.maxEnergy,
                colorGradient = listOf(Color(0xFF00C8FF), Color(0xFF00FF99)),
                modifier = Modifier.fillMaxWidth()
            )
        }

        // ==========================================
        // 2. TOP-RIGHT: CIRCULAR RADAR & CURRENT MISSION BOX
        // ==========================================
        Column(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(end = 14.dp, top = 8.dp),
            horizontalAlignment = Alignment.End
        ) {
            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Quick Pause Button
                IconButton(
                    onClick = onPauseClick,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(0x55000000))
                        .border(1.dp, Color(0x44FFFFFF), CircleShape)
                        .testTag("pause_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Pause,
                        contentDescription = "Pause",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Circular Radar Minimap
                MinimapRadar(
                    spider = spider,
                    frog = frog,
                    preyList = renderer.preyList
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Current Mission Box
            Box(
                modifier = Modifier
                    .width(130.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0x66000000))
                    .border(0.8.dp, Color(0x44FFFFFF), RoundedCornerShape(10.dp))
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            ) {
                Column {
                    Text(
                        text = "Current Mission",
                        color = Color(0xAAFFFFFF),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Hunt ${session.currentMission.targetPreyCount} Prey",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "(${session.preyEatenCount}/${session.currentMission.targetPreyCount})",
                        color = Color(0xFFFF9900),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // ==========================================
        // 3. TOP-CENTER: THREAT WARNING & WEATHER
        // ==========================================
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Frog Threat Status Pill
            if (isFrogNear || frog.state == FrogState.STUNNED) {
                val threatBg = if (frog.state == FrogState.STUNNED) Color(0xCC0088FF) else if (isFrogChasing) Color(0xCCDD0000) else Color(0xCCFF8800)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(threatBg)
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (frog.state == FrogState.STUNNED) "🕸️ FROG STUNNED (${frog.stunTimer.toInt()}s)" else "🐸 ${frog.state.label} (${distToFrog.toInt()}m)",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Weather Pill Switcher
            Box(
                modifier = Modifier
                    .padding(top = 4.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0x33000000))
                    .clickable { showWeatherPicker = !showWeatherPicker }
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "${session.weather.icon} ${session.weather.title}",
                    color = Color(0xCCFFFFFF),
                    fontSize = 10.sp
                )
            }

            AnimatedVisibility(visible = showWeatherPicker) {
                Row(
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xEE111111))
                        .border(1.dp, Color(0x44FFA500), RoundedCornerShape(10.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Weather.values().forEach { w ->
                        TextButton(
                            onClick = {
                                session.weather = w
                                showWeatherPicker = false
                            },
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("${w.icon} ${w.title.split(" ").first()}", color = if (session.weather == w) Color(0xFFFF9900) else Color.White, fontSize = 10.sp)
                        }
                    }
                }
            }
        }

        // ==========================================
        // 4. PRECISION EAT TARGET LOCK
        // ==========================================
        if (targetPrey != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(y = (-30).dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xEEFF5500))
                    .border(2.dp, Color.White, RoundedCornerShape(12.dp))
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "🎯 IN BITE RANGE: ${targetPrey.type.displayName.uppercase()}! (PRESS EAT)",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
        }

        // ==========================================
        // 5. BOTTOM-LEFT: DIRECTIONAL JOYPAD
        // ==========================================
        VirtualJoystick(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 18.dp, bottom = 22.dp),
            onMove = { x, y ->
                renderer.moveInputX = x
                renderer.moveInputZ = y
            }
        )

        // ==========================================
        // 6. BOTTOM-RIGHT: 4 CIRCULAR ACTION BUTTONS (EAT, SPIT, WEB, JUMP)
        // ==========================================
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 18.dp, bottom = 18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Row 1: EAT & SPIT
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                // EAT BUTTON
                ActionButton(
                    label = "EAT",
                    icon = "🍗",
                    primaryColor = Color(0xFFFF6600),
                    isEnabled = true,
                    isHighlighted = targetPrey != null,
                    onClick = { renderer.executeEat() }
                )

                // SPIT BUTTON
                ActionButton(
                    label = "SPIT",
                    icon = "⚪",
                    primaryColor = Color(0xFF00CC88),
                    cooldownFraction = spider.spitCooldown / 0.9f,
                    isEnabled = spider.canSpit(),
                    onClick = { renderer.executeSpit() }
                )
            }

            // Row 2: WEB & JUMP
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                // WEB BUTTON
                ActionButton(
                    label = "WEB",
                    icon = "🕸️",
                    primaryColor = Color(0xFF0099FF),
                    cooldownFraction = spider.webCooldown / 3.5f,
                    isEnabled = spider.canWeb(),
                    onClick = { renderer.executeWeb() }
                )

                // JUMP BUTTON
                ActionButton(
                    label = "JUMP",
                    icon = "🕷️",
                    primaryColor = Color(0xFFFFBB00),
                    isEnabled = !spider.isJumping && spider.energy >= 15f,
                    onClick = { renderer.executeJump() }
                )
            }
        }

        // ==========================================
        // 7. BOTTOM-CENTER: HINT / CAMERA GUIDE
        // ==========================================
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 8.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0x44000000))
                .padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Text(
                text = "Drag to orbit 360° • Pinch to zoom • Double tap to snap behind",
                color = Color(0xBBFFFFFF),
                fontSize = 9.sp
            )
        }
    }
}
