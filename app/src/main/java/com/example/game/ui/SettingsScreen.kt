package com.example.game.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.example.game.engine.GameAudioEngine
import com.example.game.model.*

/**
 * Settings Screen for audio, camera sensitivity, difficulty, and weather controls.
 */
@Composable
fun SettingsScreen(
    session: GameSession,
    audioEngine: GameAudioEngine,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var volume by remember { mutableFloatStateOf(session.soundVolume) }
    var isMuted by remember { mutableStateOf(session.isMuted) }
    var cameraSensitivity by remember { mutableFloatStateOf(session.cameraSensitivity) }
    var invertPitch by remember { mutableStateOf(session.invertCameraPitch) }
    var selectedDifficulty by remember { mutableStateOf(session.difficulty) }
    var selectedWeather by remember { mutableStateOf(session.weather) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0D0B08))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier.testTag("settings_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Column {
                    Text(
                        text = "GAME SETTINGS",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFF9900)
                    )
                    Text(
                        text = "Customize controls, sound, and ecosystem",
                        fontSize = 12.sp,
                        color = Color(0xAAFFFFFF)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Section 1: Audio Settings
                item {
                    SettingsSection(title = "🔊 AUDIO & SFX") {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Mute All Audio", color = Color.White, fontSize = 14.sp)
                            Switch(
                                checked = isMuted,
                                onCheckedChange = {
                                    isMuted = it
                                    session.isMuted = it
                                    audioEngine.setMuted(it)
                                }
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Master Volume: ${(volume * 100).toInt()}%",
                            color = Color(0xCCFFFFFF),
                            fontSize = 13.sp
                        )
                        Slider(
                            value = volume,
                            onValueChange = {
                                volume = it
                                session.soundVolume = it
                            },
                            valueRange = 0f..1f,
                            enabled = !isMuted,
                            colors = SliderDefaults.colors(
                                thumbColor = Color(0xFFFF7700),
                                activeTrackColor = Color(0xFFFF9900)
                            )
                        )
                    }
                }

                // Section 2: Camera & Controls
                item {
                    SettingsSection(title = "🎮 CONTROLS & CAMERA") {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Invert Camera Pitch (Y-Axis)", color = Color.White, fontSize = 14.sp)
                            Switch(
                                checked = invertPitch,
                                onCheckedChange = {
                                    invertPitch = it
                                    session.invertCameraPitch = it
                                }
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Camera Orbit Sensitivity: ${"%.1f".format(cameraSensitivity)}x",
                            color = Color(0xCCFFFFFF),
                            fontSize = 13.sp
                        )
                        Slider(
                            value = cameraSensitivity,
                            onValueChange = {
                                cameraSensitivity = it
                                session.cameraSensitivity = it
                            },
                            valueRange = 0.5f..2.5f,
                            colors = SliderDefaults.colors(
                                thumbColor = Color(0xFFFF7700),
                                activeTrackColor = Color(0xFFFF9900)
                            )
                        )
                    }
                }

                // Section 3: Ecosystem Difficulty
                item {
                    SettingsSection(title = "🌿 ECOSYSTEM DIFFICULTY") {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Difficulty.values().forEach { diff ->
                                val isSelected = selectedDifficulty == diff
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isSelected) Color(0x44FF7700) else Color(0x15FFFFFF))
                                        .border(
                                            width = if (isSelected) 1.5.dp else 0.5.dp,
                                            color = if (isSelected) Color(0xFFFF7700) else Color(0x22FFFFFF),
                                            shape = RoundedCornerShape(10.dp)
                                        )
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = diff.title,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = if (isSelected) Color(0xFFFF9900) else Color.White
                                        )
                                        Text(
                                            text = diff.description,
                                            fontSize = 11.sp,
                                            color = Color(0xAAFFFFFF)
                                        )
                                    }

                                    RadioButton(
                                        selected = isSelected,
                                        onClick = {
                                            selectedDifficulty = diff
                                            session.difficulty = diff
                                        },
                                        colors = RadioButtonDefaults.colors(selectedColor = Color(0xFFFF7700))
                                    )
                                }
                            }
                        }
                    }
                }

                // Section 4: Atmosphere & Weather
                item {
                    SettingsSection(title = "🌦️ ATMOSPHERE & WEATHER") {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Weather.values().forEach { w ->
                                val isSelected = selectedWeather == w
                                Button(
                                    onClick = {
                                        selectedWeather = w
                                        session.weather = w
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isSelected) Color(0xFFFF6600) else Color(0x22FFFFFF)
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(w.icon, fontSize = 16.sp)
                                        Text(
                                            text = w.title.split(" ").first(),
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(20.dp))
                }
            }
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF16120E)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x22FFFFFF))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFF9900)
            )
            Spacer(modifier = Modifier.height(10.dp))
            content()
        }
    }
}
