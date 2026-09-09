package com.example.game.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.game.model.GameSession

/**
 * Pause Menu matching the reference UI (PAUSED, RESUME, SETTINGS, LEVELS, MAIN MENU).
 */
@Composable
fun PauseDialog(
    session: GameSession,
    onResume: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenLevels: () -> Unit,
    onMainMenu: () -> Unit
) {
    Dialog(onDismissRequest = onResume) {
        Card(
            modifier = Modifier
                .width(260.dp)
                .testTag("pause_dialog"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xF0120E0A)),
            border = androidx.compose.foundation.BorderStroke(1.2.dp, Color(0x44FFFFFF))
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "PAUSED",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    letterSpacing = 2.sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                // RESUME BUTTON
                Button(
                    onClick = onResume,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF6600)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("RESUME", fontWeight = FontWeight.Bold, fontSize = 13.sp, letterSpacing = 1.sp)
                }

                // SETTINGS BUTTON
                OutlinedButton(
                    onClick = onOpenSettings,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x66FFFFFF))
                ) {
                    Text("SETTINGS", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, letterSpacing = 1.sp)
                }

                // LEVELS BUTTON
                OutlinedButton(
                    onClick = onOpenLevels,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x66FFFFFF))
                ) {
                    Text("LEVELS", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, letterSpacing = 1.sp)
                }

                // MAIN MENU BUTTON
                OutlinedButton(
                    onClick = onMainMenu,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x66FFFFFF))
                ) {
                    Text("MAIN MENU", color = Color(0xFFFF9900), fontWeight = FontWeight.SemiBold, fontSize = 13.sp, letterSpacing = 1.sp)
                }
            }
        }
    }
}

/**
 * Game Over Dialog (Caught by Frog or Starvation).
 */
@Composable
fun GameOverDialog(
    session: GameSession,
    onRestart: () -> Unit,
    onMainMenu: () -> Unit
) {
    Dialog(onDismissRequest = {}) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("game_over_dialog"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xF8200808)),
            border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFFF3333))
        ) {
            Column(
                modifier = Modifier
                    .padding(22.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "☠️ THE JUNGLE CLAIMED YOU",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFF4444),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "The giant predator frog caught up, or your energy ran out in the harsh biome.",
                    color = Color(0xCCFFFFFF),
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Stats Grid
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0x33000000))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    StatRow("🍖 Prey Eaten:", "${session.preyEatenCount}")
                    StatRow("🕸️ Webs Landed on Frog:", "${session.websLandedCount}")
                    StatRow("⏱️ Survival Time:", "${session.survivalTimeSeconds.toInt()}s")
                    StatRow("🗺️ Environment:", session.biome.title)
                    StatRow("🎚️ Difficulty:", session.difficulty.title)
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = onRestart,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF4444)),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("TRY AGAIN 🕷️", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = onMainMenu,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("MAIN MENU", color = Color.White, fontSize = 12.sp)
                }
            }
        }
    }
}

/**
 * Victory / Mission Complete Dialog.
 */
@Composable
fun VictoryDialog(
    session: GameSession,
    onNextMission: () -> Unit,
    onContinueFreeRoam: () -> Unit,
    onMainMenu: () -> Unit
) {
    Dialog(onDismissRequest = {}) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("victory_dialog"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xF80A2210)),
            border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF00FF88))
        ) {
            Column(
                modifier = Modifier
                    .padding(22.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "🏆 MISSION ACCOMPLISHED!",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF00FF88),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "${session.currentMission.title} Completed Successfully!",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(14.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0x33000000))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    StatRow("🍖 Total Prey Hunted:", "${session.preyEatenCount}")
                    StatRow("🕸️ Frog Stuns:", "${session.websLandedCount}")
                    StatRow("⏱️ Survival Time:", "${session.survivalTimeSeconds.toInt()}s")
                    StatRow("🗺️ Environment:", session.biome.title)
                }

                Spacer(modifier = Modifier.height(18.dp))

                Button(
                    onClick = onNextMission,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00DD77)),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("NEXT MISSION 🌿", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.Black)
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onContinueFreeRoam,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("FREE ROAM", color = Color.White, fontSize = 11.sp)
                    }

                    OutlinedButton(
                        onClick = onMainMenu,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("MAIN MENU", color = Color.White, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun StatRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = Color(0xCCFFFFFF), fontSize = 12.sp)
        Text(text = value, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
    }
}
