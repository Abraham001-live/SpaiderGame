package com.example.game.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.game.model.GameSession

/**
 * Main Menu Screen matching the fiery "SPIDER HUNT" title poster and arcade buttons.
 */
@Composable
fun MainMenuScreen(
    session: GameSession,
    onPlayClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onLevelsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val glowScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowScale"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Background Poster Art
        Image(
            painter = painterResource(id = R.drawable.img_spider_hunt_menu),
            contentDescription = "Spider Hunt Main Menu",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // Dark Vignette & Gradient Overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0x99000000),
                            Color(0x33000000),
                            Color(0xDD000000)
                        )
                    )
                )
        )

        // Main Menu Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Title Header with Fiery Orange Glow
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 16.dp)
            ) {
                Text(
                    text = "SPIDER",
                    fontSize = 46.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFFFF9900),
                    letterSpacing = 4.sp,
                    modifier = Modifier.scale(glowScale),
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontFamily = FontFamily.SansSerif
                    )
                )
                Text(
                    text = "HUNT",
                    fontSize = 54.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFFFF4500),
                    letterSpacing = 6.sp,
                    modifier = Modifier.offset(y = (-14).dp),
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontFamily = FontFamily.SansSerif
                    )
                )
                Text(
                    text = "AMAZON ECOSYSTEM SIMULATOR",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xDDFFFFFF),
                    letterSpacing = 2.sp,
                    modifier = Modifier.offset(y = (-12).dp)
                )
            }

            // Central / Bottom Menu Buttons Stack
            Column(
                modifier = Modifier
                    .widthIn(max = 280.dp)
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // PLAY BUTTON (Fiery Orange Hero Pill)
                Button(
                    onClick = onPlayClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .shadow(12.dp, RoundedCornerShape(26.dp), spotColor = Color(0xFFFF6600))
                        .testTag("menu_play_button"),
                    shape = RoundedCornerShape(26.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFF6600)
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
                ) {
                    Text(
                        text = "PLAY",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        letterSpacing = 2.sp
                    )
                }

                // SETTINGS BUTTON (Glassmorphic)
                OutlinedButton(
                    onClick = onSettingsClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color(0x55000000))
                        .testTag("menu_settings_button"),
                    shape = RoundedCornerShape(24.dp),
                    border = androidx.compose.foundation.BorderStroke(1.2.dp, Color(0x66FFFFFF))
                ) {
                    Text(
                        text = "SETTINGS",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        letterSpacing = 1.5.sp
                    )
                }

                // LEVELS BUTTON (Glassmorphic)
                OutlinedButton(
                    onClick = onLevelsClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color(0x55000000))
                        .testTag("menu_levels_button"),
                    shape = RoundedCornerShape(24.dp),
                    border = androidx.compose.foundation.BorderStroke(1.2.dp, Color(0x66FFFFFF))
                ) {
                    Text(
                        text = "LEVELS",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        letterSpacing = 1.5.sp
                    )
                }

                // Selected Environment Tag
                Box(
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0x44000000))
                        .border(0.8.dp, Color(0x33FFFFFF), RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "Current: ${session.biome.title} • ${session.difficulty.title}",
                        color = Color(0xCCFFCC00),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}
