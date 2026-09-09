package com.example.game.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.example.game.engine.GameAudioEngine
import com.example.game.engine.SpiderRenderer
import com.example.game.model.*

/**
 * Root Game Screen with complete screen navigation (MAIN_MENU, LEVELS, SETTINGS, PLAYING)
 * and 3D OpenGL world integration.
 */
@Composable
fun SpiderGameScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val audioEngine = remember { GameAudioEngine() }
    val session = remember { GameSession() }

    var currentScreen by remember { mutableStateOf(AppScreen.MAIN_MENU) }
    var playState by remember { mutableStateOf(session.playState) }

    val renderer = remember {
        SpiderRenderer(
            context = context,
            audioEngine = audioEngine,
            session = session,
            onStateChanged = {
                playState = session.playState
            }
        )
    }

    var gameSurfaceView by remember { mutableStateOf<GameSurfaceView?>(null) }

    fun restartGame(newBiome: EnvironmentBiome? = null) {
        if (newBiome != null) {
            session.biome = newBiome
        }
        session.reset()
        renderer.spider.position.set(0f, 0.4f, 0f)
        renderer.spider.yawDeg = 0f
        renderer.spider.health = 100f
        renderer.spider.food = 85f
        renderer.spider.energy = 100f
        renderer.spider.isAlive = true

        renderer.frog.position.set(0f, 0.5f, 25f)
        renderer.frog.yawDeg = 180f
        renderer.frog.state = FrogState.IDLE
        renderer.frog.stunTimer = 0f

        renderer.projectiles.clear()
        renderer.respawnPrey()
        session.playState = PlayState.PLAYING
        playState = PlayState.PLAYING
    }

    AnimatedContent(
        targetState = currentScreen,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "screen_transition"
    ) { screen ->
        when (screen) {
            AppScreen.MAIN_MENU -> {
                MainMenuScreen(
                    session = session,
                    onPlayClick = {
                        restartGame()
                        currentScreen = AppScreen.PLAYING
                    },
                    onSettingsClick = {
                        currentScreen = AppScreen.SETTINGS
                    },
                    onLevelsClick = {
                        currentScreen = AppScreen.LEVELS
                    },
                    modifier = modifier
                )
            }

            AppScreen.LEVELS -> {
                LevelsScreen(
                    session = session,
                    onSelectBiome = { selectedBiome ->
                        restartGame(selectedBiome)
                        currentScreen = AppScreen.PLAYING
                    },
                    onBackClick = {
                        currentScreen = AppScreen.MAIN_MENU
                    },
                    modifier = modifier
                )
            }

            AppScreen.SETTINGS -> {
                SettingsScreen(
                    session = session,
                    audioEngine = audioEngine,
                    onBackClick = {
                        currentScreen = AppScreen.MAIN_MENU
                    },
                    modifier = modifier
                )
            }

            AppScreen.PLAYING -> {
                Box(modifier = modifier.fillMaxSize()) {
                    // 3D OpenGL Surface View Layer
                    AndroidView(
                        factory = { ctx ->
                            GameSurfaceView(ctx, renderer).also {
                                gameSurfaceView = it
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )

                    // Glassmorphic Survival HUD Layer matching the reference image
                    GameHud(
                        renderer = renderer,
                        onPauseClick = {
                            session.playState = PlayState.PAUSED
                            playState = PlayState.PAUSED
                        }
                    )

                    // Dialogs
                    when (playState) {
                        PlayState.PAUSED -> {
                            PauseDialog(
                                session = session,
                                onResume = {
                                    session.playState = PlayState.PLAYING
                                    playState = PlayState.PLAYING
                                },
                                onOpenSettings = {
                                    currentScreen = AppScreen.SETTINGS
                                },
                                onOpenLevels = {
                                    currentScreen = AppScreen.LEVELS
                                },
                                onMainMenu = {
                                    session.playState = PlayState.PLAYING
                                    playState = PlayState.PLAYING
                                    currentScreen = AppScreen.MAIN_MENU
                                }
                            )
                        }

                        PlayState.GAME_OVER -> {
                            GameOverDialog(
                                session = session,
                                onRestart = {
                                    restartGame()
                                },
                                onMainMenu = {
                                    session.playState = PlayState.PLAYING
                                    playState = PlayState.PLAYING
                                    currentScreen = AppScreen.MAIN_MENU
                                }
                            )
                        }

                        PlayState.VICTORY -> {
                            VictoryDialog(
                                session = session,
                                onNextMission = {
                                    session.advanceMission()
                                    restartGame()
                                },
                                onContinueFreeRoam = {
                                    session.playState = PlayState.PLAYING
                                    playState = PlayState.PLAYING
                                },
                                onMainMenu = {
                                    session.playState = PlayState.PLAYING
                                    playState = PlayState.PLAYING
                                    currentScreen = AppScreen.MAIN_MENU
                                }
                            )
                        }

                        PlayState.PLAYING -> {
                            // Active 3D gameplay
                        }
                    }
                }
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            gameSurfaceView?.onPause()
        }
    }
}
