package com.example.game.model

enum class Difficulty(
    val title: String,
    val description: String,
    val preyCount: Int,
    val frogChaseSpeed: Float,
    val webStunSeconds: Float,
    val foodDrainRate: Float,
    val detectionRadius: Float
) {
    EASY(
        title = "Survivor",
        description = "Abundant prey, slow frog detection, long web stun.",
        preyCount = 10,
        frogChaseSpeed = 5.2f,
        webStunSeconds = 7.5f,
        foodDrainRate = 0.8f,
        detectionRadius = 18f
    ),
    MEDIUM(
        title = "Hunter",
        description = "Balanced predator ecosystem with active frog patrols.",
        preyCount = 7,
        frogChaseSpeed = 6.8f,
        webStunSeconds = 5.5f,
        foodDrainRate = 1.2f,
        detectionRadius = 26f
    ),
    HARD(
        title = "Predator",
        description = "Aggressive giant frog, scarcer prey, rapid energy drain.",
        preyCount = 5,
        frogChaseSpeed = 8.2f,
        webStunSeconds = 3.8f,
        foodDrainRate = 1.8f,
        detectionRadius = 34f
    ),
    NIGHTMARE(
        title = "Ecosystem",
        description = "Ruthless food scarcity and relentless car-sized apex frog.",
        preyCount = 3,
        frogChaseSpeed = 9.5f,
        webStunSeconds = 2.8f,
        foodDrainRate = 2.4f,
        detectionRadius = 42f
    )
}

enum class Weather(
    val title: String,
    val icon: String,
    val fogDensity: Float,
    val lightColor: FloatArray,
    val ambientColor: FloatArray,
    val fogColor: FloatArray
) {
    MORNING(
        title = "Morning Rays",
        icon = "☀️",
        fogDensity = 0.45f,
        lightColor = floatArrayOf(1.0f, 0.92f, 0.78f),
        ambientColor = floatArrayOf(0.42f, 0.45f, 0.38f),
        fogColor = floatArrayOf(0.85f, 0.82f, 0.72f, 0.6f)
    ),
    AFTERNOON(
        title = "Amazon Sun",
        icon = "🌤️",
        fogDensity = 0.25f,
        lightColor = floatArrayOf(1.0f, 1.0f, 0.95f),
        ambientColor = floatArrayOf(0.5f, 0.55f, 0.48f),
        fogColor = floatArrayOf(0.65f, 0.78f, 0.72f, 0.45f)
    ),
    MIST(
        title = "Jungle Mist",
        icon = "🌫️",
        fogDensity = 0.85f,
        lightColor = floatArrayOf(0.8f, 0.85f, 0.9f),
        ambientColor = floatArrayOf(0.35f, 0.42f, 0.45f),
        fogColor = floatArrayOf(0.68f, 0.78f, 0.82f, 0.85f)
    ),
    RAIN(
        title = "Rainstorm",
        icon = "🌧️",
        fogDensity = 0.75f,
        lightColor = floatArrayOf(0.65f, 0.72f, 0.8f),
        ambientColor = floatArrayOf(0.28f, 0.34f, 0.38f),
        fogColor = floatArrayOf(0.42f, 0.52f, 0.58f, 0.8f)
    ),
    NIGHT(
        title = "Deep Night",
        icon = "🌙",
        fogDensity = 0.65f,
        lightColor = floatArrayOf(0.35f, 0.45f, 0.75f),
        ambientColor = floatArrayOf(0.12f, 0.16f, 0.25f),
        fogColor = floatArrayOf(0.06f, 0.08f, 0.15f, 0.9f)
    )
}

data class Mission(
    val id: Int,
    val title: String,
    val description: String,
    val targetPreyCount: Int,
    val targetEscapeCount: Int,
    val targetSurvivalTime: Float
)

val AMAZON_MISSIONS = listOf(
    Mission(
        id = 1,
        title = "Mission 01 — First Hunt",
        description = "Track and eat 3 prey animals to sustain your giant spider.",
        targetPreyCount = 3,
        targetEscapeCount = 0,
        targetSurvivalTime = 0f
    ),
    Mission(
        id = 2,
        title = "Mission 02 — The Giant Frog",
        description = "A car-sized frog has entered the territory. Trap it with WEB at least once!",
        targetPreyCount = 2,
        targetEscapeCount = 1,
        targetSurvivalTime = 0f
    ),
    Mission(
        id = 3,
        title = "Mission 03 — Forest Predator",
        description = "Hunt 5 prey and survive for at least 90 seconds.",
        targetPreyCount = 5,
        targetEscapeCount = 2,
        targetSurvivalTime = 90f
    ),
    Mission(
        id = 4,
        title = "Mission 04 — Apex Jungle Survivor",
        description = "Master hunting and evasion. Consume 8 prey and outsmart the apex frog predator.",
        targetPreyCount = 8,
        targetEscapeCount = 3,
        targetSurvivalTime = 150f
    )
)

enum class EnvironmentBiome(
    val title: String,
    val subtitle: String,
    val description: String,
    val drawableRes: String,
    val terrainSize: Float,
    val predatorName: String,
    val predatorScale: Float
) {
    AMAZON_FOREST(
        title = "Amazon Forest",
        subtitle = "Tropical Rainforest",
        description = "Lush tropical rainforest with giant canopy trees, creek streams, and the apex car-sized predator frog.",
        drawableRes = "img_level_amazon",
        terrainSize = 130f,
        predatorName = "Giant Goliath Frog",
        predatorScale = 3.5f
    ),
    HIGHLANDS(
        title = "Highlands",
        subtitle = "Mountain Plateau",
        description = "Vast alpine plateau with rocky cliffs, grassy mountain slopes, and high-altitude hunting grounds.",
        drawableRes = "img_level_highlands",
        terrainSize = 150f,
        predatorName = "Mountain Toad",
        predatorScale = 3.2f
    ),
    CITY(
        title = "City",
        subtitle = "Concrete Metropolis",
        description = "Urban canyon filled with towering skyscrapers, asphalt streets, and overhead helicopters.",
        drawableRes = "img_level_city",
        terrainSize = 120f,
        predatorName = "Mutant Sewer Frog",
        predatorScale = 3.8f
    )
}

enum class AppScreen {
    MAIN_MENU,
    LEVELS,
    SETTINGS,
    PLAYING
}

enum class PlayState {
    PLAYING,
    PAUSED,
    VICTORY,
    GAME_OVER
}

class GameSession(
    var biome: EnvironmentBiome = EnvironmentBiome.AMAZON_FOREST,
    var difficulty: Difficulty = Difficulty.MEDIUM,
    var weather: Weather = Weather.AFTERNOON,
    var currentMissionIndex: Int = 0
) {
    var playState: PlayState = PlayState.PLAYING
    var survivalTimeSeconds: Float = 0f
    var preyEatenCount: Int = 0
    var frogEscapesCount: Int = 0
    var websLandedCount: Int = 0
    var spitsFiredCount: Int = 0

    // Settings
    var soundVolume: Float = 1.0f
    var isMuted: Boolean = false
    var invertCameraPitch: Boolean = false
    var cameraSensitivity: Float = 1.0f

    val currentMission: Mission
        get() = AMAZON_MISSIONS[currentMissionIndex.coerceIn(0, AMAZON_MISSIONS.size - 1)]

    fun checkMissionCompletion(): Boolean {
        val m = currentMission
        val preyDone = preyEatenCount >= m.targetPreyCount
        val escapeDone = websLandedCount >= m.targetEscapeCount
        val timeDone = survivalTimeSeconds >= m.targetSurvivalTime
        return preyDone && escapeDone && timeDone
    }

    fun advanceMission(): Boolean {
        if (currentMissionIndex < AMAZON_MISSIONS.size - 1) {
            currentMissionIndex++
            return true
        }
        return false
    }

    fun reset() {
        playState = PlayState.PLAYING
        survivalTimeSeconds = 0f
        preyEatenCount = 0
        frogEscapesCount = 0
        websLandedCount = 0
        spitsFiredCount = 0
    }
}
