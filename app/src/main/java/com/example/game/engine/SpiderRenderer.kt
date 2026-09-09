package com.example.game.engine

import android.content.Context
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import com.example.game.model.*
import java.util.concurrent.CopyOnWriteArrayList
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.*

/**
 * High-performance 3D OpenGL ES 2.0 Renderer for SPIDER HUNT.
 */
class SpiderRenderer(
    private val context: Context,
    val audioEngine: GameAudioEngine,
    val session: GameSession,
    val onStateChanged: () -> Unit
) : GLSurfaceView.Renderer {

    // Camera settings
    var cameraYawOffsetDeg: Float = 0f
    var cameraPitchDeg: Float = 21f
    var cameraDistance: Float = 5.8f
    val minCameraDistance: Float = 3.5f
    val maxCameraDistance: Float = 14.0f

    var isManualCameraActive: Boolean = false
    var lastCameraTouchTimeMs: Long = System.currentTimeMillis()

    val cameraYawDeg: Float
        get() = GameMath.normalizeAngleDeg(spider.yawDeg + cameraYawOffsetDeg)

    private var smoothedCamX = 0f
    private var smoothedCamY = 5f
    private var smoothedCamZ = -7f
    private var isCamInitialized = false

    // Joystick / Controls input
    var moveInputX: Float = 0f
    var moveInputZ: Float = 0f

    // Entities
    val spider = SpiderEntity(position = Vec3(0f, 0.4f, 0f), yawDeg = 0f)
    val frog = FrogEntity(position = Vec3(0f, 0.5f, 24f), yawDeg = 180f)
    val preyList = CopyOnWriteArrayList<PreyEntity>()
    val projectiles = CopyOnWriteArrayList<Projectile>()

    // Rain / spore particle system
    private val rainParticleCount = 120
    private val rainParticlesX = FloatArray(rainParticleCount)
    private val rainParticlesY = FloatArray(rainParticleCount)
    private val rainParticlesZ = FloatArray(rainParticleCount)

    // Environment static objects (trees, rocks, logs)
    private val trees = ArrayList<Vec3>()
    private val rocks = ArrayList<Vec3>()

    // Shaders & Meshes
    private var programId: Int = 0

    // Uniform handles
    private var uMVPMatrixHandle: Int = 0
    private var uModelMatrixHandle: Int = 0
    private var uLightDirHandle: Int = 0
    private var uLightColorHandle: Int = 0
    private var uAmbientColorHandle: Int = 0
    private var uCameraPosHandle: Int = 0
    private var uFogColorHandle: Int = 0
    private var uFogDensityHandle: Int = 0
    private var uShininessHandle: Int = 0
    private var uOverrideColorHandle: Int = 0
    private var uUseOverrideColorHandle: Int = 0

    // Attribute handles
    private var aPositionHandle: Int = 0
    private var aNormalHandle: Int = 0
    private var aColorHandle: Int = 0

    // Matrices
    private val projectionMatrix = FloatArray(16)
    private val viewMatrix = FloatArray(16)
    private val mvpMatrix = FloatArray(16)
    private val modelMatrix = FloatArray(16)
    private val tempMatrix = FloatArray(16)

    // 3D Meshes
    private var terrainMesh: Mesh? = null
    private var waterMesh: Mesh? = null
    private var sphereMesh: Mesh? = null
    private var cylinderMesh: Mesh? = null
    private var boxMesh: Mesh? = null
    private var webMesh: Mesh? = null
    private var spitMesh: Mesh? = null
    private var fernMesh: Mesh? = null
    private var mushroomMesh: Mesh? = null
    private var sunShaftMesh: Mesh? = null
    private var spiderModel: SpiderCharacterModel? = null

    private var lastTimeNs: Long = 0L

    init {
        initEnvironment()
        respawnPrey()
        initRainParticles()
    }

    private fun initEnvironment() {
        // Scatter large jungle trees
        trees.clear()
        val treeCoords = arrayOf(
            floatArrayOf(-18f, 15f), floatArrayOf(16f, 18f), floatArrayOf(-22f, -14f),
            floatArrayOf(24f, -20f), floatArrayOf(-8f, -26f), floatArrayOf(12f, 32f),
            floatArrayOf(-30f, 25f), floatArrayOf(32f, 12f), floatArrayOf(-15f, 38f),
            floatArrayOf(5f, -34f), floatArrayOf(-35f, -10f), floatArrayOf(28f, -32f)
        )
        for (c in treeCoords) {
            trees.add(Vec3(c[0], 0f, c[1]))
        }

        // Scatter mossy rocks
        rocks.clear()
        val rockCoords = arrayOf(
            floatArrayOf(-6f, 8f), floatArrayOf(8f, -10f), floatArrayOf(-14f, -5f),
            floatArrayOf(19f, 6f), floatArrayOf(-10f, 22f), floatArrayOf(15f, -25f)
        )
        for (c in rockCoords) {
            rocks.add(Vec3(c[0], 0f, c[1]))
        }
    }

    fun respawnPrey() {
        preyList.clear()
        val count = session.difficulty.preyCount
        val types = PreyType.values()

        for (i in 0 until count) {
            val type = types[i % types.size]
            val angle = (Math.PI * 2 * i / count).toFloat() + (Math.random().toFloat() * 0.5f)
            val dist = 8f + Math.random().toFloat() * 28f
            val x = cos(angle) * dist
            val z = sin(angle) * dist
            val prey = PreyEntity(
                id = i + 1,
                type = type,
                position = Vec3(x, 0.35f, z),
                yawDeg = (Math.random() * 360f).toFloat()
            )
            preyList.add(prey)
        }
    }

    private fun initRainParticles() {
        for (i in 0 until rainParticleCount) {
            rainParticlesX[i] = (Math.random().toFloat() * 80f) - 40f
            rainParticlesY[i] = Math.random().toFloat() * 20f
            rainParticlesZ[i] = (Math.random().toFloat() * 80f) - 40f
        }
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)
        GLES20.glDepthFunc(GLES20.GL_LEQUAL)
        GLES20.glEnable(GLES20.GL_BLEND)
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)
        GLES20.glEnable(GLES20.GL_CULL_FACE)
        GLES20.glCullFace(GLES20.GL_BACK)

        programId = ShaderUtil.createProgram()

        // Get handles
        uMVPMatrixHandle = GLES20.glGetUniformLocation(programId, "uMVPMatrix")
        uModelMatrixHandle = GLES20.glGetUniformLocation(programId, "uModelMatrix")
        uLightDirHandle = GLES20.glGetUniformLocation(programId, "uLightDir")
        uLightColorHandle = GLES20.glGetUniformLocation(programId, "uLightColor")
        uAmbientColorHandle = GLES20.glGetUniformLocation(programId, "uAmbientColor")
        uCameraPosHandle = GLES20.glGetUniformLocation(programId, "uCameraPos")
        uFogColorHandle = GLES20.glGetUniformLocation(programId, "uFogColor")
        uFogDensityHandle = GLES20.glGetUniformLocation(programId, "uFogDensity")
        uShininessHandle = GLES20.glGetUniformLocation(programId, "uShininess")
        uOverrideColorHandle = GLES20.glGetUniformLocation(programId, "uOverrideColor")
        uUseOverrideColorHandle = GLES20.glGetUniformLocation(programId, "uUseOverrideColor")

        aPositionHandle = GLES20.glGetAttribLocation(programId, "aPosition")
        aNormalHandle = GLES20.glGetAttribLocation(programId, "aNormal")
        aColorHandle = GLES20.glGetAttribLocation(programId, "aColor")

        // Build 3D meshes
        terrainMesh = MeshBuilder.createTerrain(size = 130f, subdivisions = 36)
        waterMesh = MeshBuilder.createWaterPlane(size = 130f)
        sphereMesh = MeshBuilder.createSphere(radius = 1.0f, rings = 12, sectors = 12, r = 1f, g = 1f, b = 1f)
        cylinderMesh = MeshBuilder.createCylinder(radiusBottom = 0.5f, radiusTop = 0.5f, height = 1.0f, segments = 10, r = 1f, g = 1f, b = 1f)
        boxMesh = MeshBuilder.createBox(sx = 1f, sy = 1f, sz = 1f, r = 1f, g = 1f, b = 1f)
        webMesh = MeshBuilder.createWebMesh(radius = 2.4f)
        spitMesh = MeshBuilder.createSphere(radius = 0.4f, rings = 8, sectors = 8, r = 0.95f, g = 1f, b = 0.85f)
        fernMesh = MeshBuilder.createBroadleafFern(leafCount = 7, length = 2.8f, width = 0.85f)
        mushroomMesh = MeshBuilder.createMushroom(capRadius = 1.0f, height = 1.4f)
        sunShaftMesh = MeshBuilder.createVolumetricSunShaft(topRadius = 0.8f, bottomRadius = 6.0f, height = 22.0f)
        spiderModel = SpiderMeshBuilder.createSpiderModel()

        lastTimeNs = System.nanoTime()
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
        val ratio = width.toFloat() / height.toFloat().coerceAtLeast(1f)
        Matrix.perspectiveM(projectionMatrix, 0, 52f, ratio, 0.5f, 150f)
    }

    override fun onDrawFrame(gl: GL10?) {
        val now = System.nanoTime()
        val deltaSeconds = ((now - lastTimeNs) / 1_000_000_000.0f).coerceIn(0.001f, 0.06f)
        lastTimeNs = now

        updateGameLogic(deltaSeconds)

        // Clear color based on weather fog
        val weather = session.weather
        GLES20.glClearColor(weather.fogColor[0], weather.fogColor[1], weather.fogColor[2], 1.0f)
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)

        if (programId == 0) return
        GLES20.glUseProgram(programId)

        // Smoothly return manual camera rotation offset to 0 (directly behind spider)
        val nowMs = System.currentTimeMillis()
        val timeSinceLastTouchSec = (nowMs - lastCameraTouchTimeMs) / 1000f

        if (!isManualCameraActive && timeSinceLastTouchSec > 0.8f) {
            val isMoving = abs(moveInputX) > 0.05f || abs(moveInputZ) > 0.05f
            val returnSpeed = if (isMoving) 4.5f else 2.2f
            cameraYawOffsetDeg = GameMath.lerpAngleDeg(cameraYawOffsetDeg, 0f, deltaSeconds * returnSpeed)
        }

        // Compute 3D Camera Position
        val pitchRad = GameMath.degToRad(cameraPitchDeg.coerceIn(8f, 75f))
        val totalCameraYaw = cameraYawDeg
        val yawRad = GameMath.degToRad(totalCameraYaw)

        val targetX = spider.position.x
        val targetY = spider.position.y + 0.5f
        val targetZ = spider.position.z

        val idealCamX = targetX - sin(yawRad) * cos(pitchRad) * cameraDistance
        val idealCamY = (targetY + sin(pitchRad) * cameraDistance).coerceAtLeast(0.6f)
        val idealCamZ = targetZ - cos(yawRad) * cos(pitchRad) * cameraDistance

        if (!isCamInitialized) {
            smoothedCamX = idealCamX
            smoothedCamY = idealCamY
            smoothedCamZ = idealCamZ
            isCamInitialized = true
        } else {
            val followSpeed = (12f * deltaSeconds).coerceIn(0.1f, 1f)
            smoothedCamX += (idealCamX - smoothedCamX) * followSpeed
            smoothedCamY += (idealCamY - smoothedCamY) * followSpeed
            smoothedCamZ += (idealCamZ - smoothedCamZ) * followSpeed
        }

        val camX = smoothedCamX
        val camY = smoothedCamY
        val camZ = smoothedCamZ

        Matrix.setLookAtM(viewMatrix, 0, camX, camY, camZ, targetX, targetY, targetZ, 0f, 1f, 0f)

        // Set Lighting and Environment Uniforms
        GLES20.glUniform3f(uCameraPosHandle, camX, camY, camZ)
        GLES20.glUniform3f(uLightDirHandle, -0.4f, -0.85f, -0.35f)
        GLES20.glUniform3fv(uLightColorHandle, 1, weather.lightColor, 0)
        GLES20.glUniform3fv(uAmbientColorHandle, 1, weather.ambientColor, 0)
        GLES20.glUniform4fv(uFogColorHandle, 1, weather.fogColor, 0)
        GLES20.glUniform1f(uFogDensityHandle, weather.fogDensity)
        GLES20.glUniform1f(uShininessHandle, 32.0f)

        // Draw World
        drawTerrain()
        drawTreesAndRocks()
        drawPreyAnimals()
        drawSpider()
        drawFrog()
        drawProjectiles()
        drawRainAndSpores(camX, camY, camZ, deltaSeconds)
    }

    private fun updateGameLogic(deltaSeconds: Float) {
        if (session.playState != PlayState.PLAYING) return

        session.survivalTimeSeconds += deltaSeconds

        // Update Spider
        spider.update(
            deltaSeconds = deltaSeconds,
            moveInputX = moveInputX,
            moveInputZ = moveInputZ,
            cameraYawDeg = cameraYawDeg,
            foodDrainRate = session.difficulty.foodDrainRate
        )

        // Heartbeat / Frog Audio Proximity
        val distToFrog = spider.position.distanceTo(frog.position)
        if (distToFrog < 18f && frog.state == FrogState.CHASE && Math.random() < 0.05) {
            audioEngine.playHeartbeat()
        }

        // Update Frog
        frog.baseChaseSpeed = session.difficulty.frogChaseSpeed
        frog.detectionRadius = session.difficulty.detectionRadius
        frog.update(
            deltaSeconds = deltaSeconds,
            spiderPos = spider.position,
            spiderIsAlive = spider.isAlive,
            onAttackSpider = {
                spider.takeDamage(45f)
                audioEngine.playHit()
                audioEngine.playFrog()
            }
        )

        // Update Prey
        for (prey in preyList) {
            prey.update(deltaSeconds, spider.position)
        }

        // Respawn prey periodically if count is low
        val activePrey = preyList.count { it.isAlive }
        if (activePrey < session.difficulty.preyCount / 2 && Math.random() < 0.02) {
            val angle = (Math.random() * Math.PI * 2).toFloat()
            val dist = 18f + Math.random().toFloat() * 15f
            val px = (spider.position.x + cos(angle) * dist).coerceIn(-45f, 45f)
            val pz = (spider.position.z + sin(angle) * dist).coerceIn(-45f, 45f)
            val newPrey = PreyEntity(
                id = (Math.random() * 1000).toInt(),
                type = PreyType.values().random(),
                position = Vec3(px, 0.35f, pz),
                yawDeg = (Math.random() * 360f).toFloat()
            )
            preyList.add(newPrey)
        }

        // Update Projectiles & Collisions
        val iter = projectiles.iterator()
        while (iter.hasNext()) {
            val proj = iter.next()
            proj.update(deltaSeconds)

            if (proj.isExpired) {
                projectiles.remove(proj)
                continue
            }

            // Check collision with Frog
            val distToFrogCenter = proj.position.distanceTo(Vec3(frog.position.x, frog.position.y + 1.2f, frog.position.z))
            if (distToFrogCenter < (frog.scale * 0.9f + proj.radius)) {
                if (proj.type == ProjectileType.WEB) {
                    frog.onHitByWeb(session.difficulty.webStunSeconds)
                    session.websLandedCount++
                    audioEngine.playWeb()
                    proj.isExpired = true
                    projectiles.remove(proj)
                    continue
                }
            }

            // Check collision with Prey
            if (proj.type == ProjectileType.SPIT) {
                for (prey in preyList) {
                    if (!prey.isAlive) continue
                    if (proj.position.distanceTo(prey.position) < (0.9f * prey.type.sizeScale + proj.radius)) {
                        prey.onHitBySpit()
                        proj.isExpired = true
                        projectiles.remove(proj)
                        break
                    }
                }
            }
        }

        // Check Victory & Game Over conditions
        if (!spider.isAlive) {
            session.playState = PlayState.GAME_OVER
            onStateChanged()
        } else if (session.checkMissionCompletion()) {
            session.playState = PlayState.VICTORY
            onStateChanged()
        }
    }

    // Action Triggers
    fun executeEat(): Boolean {
        val target = spider.findTargetToEat(preyList)
        if (target != null) {
            spider.performEat(target)
            session.preyEatenCount++
            audioEngine.playEat()
            return true
        }
        return false
    }

    fun executeSpit(): Boolean {
        val proj = spider.onSpit()
        if (proj != null) {
            projectiles.add(proj)
            session.spitsFiredCount++
            audioEngine.playSpit()
            return true
        }
        return false
    }

    fun executeWeb(): Boolean {
        val proj = spider.onWeb()
        if (proj != null) {
            projectiles.add(proj)
            audioEngine.playWeb()
            return true
        }
        return false
    }

    fun executeJump(): Boolean {
        if (spider.jump()) {
            audioEngine.playJump()
            return true
        }
        return false
    }

    // 3D Drawing Routines
    private fun drawTerrain() {
        val mesh = terrainMesh ?: return
        Matrix.setIdentityM(modelMatrix, 0)
        val terrainTint = when (session.biome) {
            EnvironmentBiome.AMAZON_FOREST -> floatArrayOf(0.24f, 0.45f, 0.22f, 1f)
            EnvironmentBiome.HIGHLANDS -> floatArrayOf(0.48f, 0.44f, 0.32f, 1f)
            EnvironmentBiome.CITY -> floatArrayOf(0.28f, 0.28f, 0.30f, 1f)
        }
        drawMeshWithColor(mesh, modelMatrix, terrainTint)

        // Draw water stream for jungle/highlands
        if (session.biome != EnvironmentBiome.CITY) {
            val wMesh = waterMesh ?: return
            Matrix.setIdentityM(modelMatrix, 0)
            val waterTint = if (session.biome == EnvironmentBiome.AMAZON_FOREST) {
                floatArrayOf(0.12f, 0.35f, 0.45f, 0.85f)
            } else {
                floatArrayOf(0.18f, 0.42f, 0.62f, 0.85f)
            }
            drawMeshWithColor(wMesh, modelMatrix, waterTint)
        }
    }

    private fun drawTreesAndRocks() {
        val cyl = cylinderMesh ?: return
        val sph = sphereMesh ?: return
        val box = boxMesh ?: return

        when (session.biome) {
            EnvironmentBiome.AMAZON_FOREST -> {
                val fern = fernMesh
                val mushroom = mushroomMesh
                val sunShaft = sunShaftMesh

                // Volumetric Sunbeams filtering through gaps in the dense Amazon canopy
                if (sunShaft != null && session.weather != Weather.RAIN && session.weather != Weather.NIGHT) {
                    val sunPositions = arrayOf(
                        floatArrayOf(-12f, 18f), floatArrayOf(8f, -6f),
                        floatArrayOf(-25f, -18f), floatArrayOf(20f, 22f)
                    )
                    for (sun in sunPositions) {
                        Matrix.setIdentityM(modelMatrix, 0)
                        Matrix.translateM(modelMatrix, 0, sun[0], 0f, sun[1])
                        drawMesh(sunShaft, modelMatrix)
                    }
                }

                // Draw giant jungle trees with buttress root ferns and fungi
                for (tree in trees) {
                    // Trunk
                    Matrix.setIdentityM(modelMatrix, 0)
                    Matrix.translateM(modelMatrix, 0, tree.x, 5.0f, tree.z)
                    Matrix.scaleM(modelMatrix, 0, 1.6f, 10f, 1.6f)
                    drawMeshWithColor(cyl, modelMatrix, floatArrayOf(0.32f, 0.20f, 0.12f, 1f))

                    // Lush Foliage Canopy (Stacked Green Spheres)
                    Matrix.setIdentityM(modelMatrix, 0)
                    Matrix.translateM(modelMatrix, 0, tree.x, 10.5f, tree.z)
                    Matrix.scaleM(modelMatrix, 0, 5.5f, 3.8f, 5.5f)
                    drawMeshWithColor(sph, modelMatrix, floatArrayOf(0.12f, 0.42f, 0.16f, 1f))

                    Matrix.setIdentityM(modelMatrix, 0)
                    Matrix.translateM(modelMatrix, 0, tree.x + 0.8f, 12.2f, tree.z - 0.5f)
                    Matrix.scaleM(modelMatrix, 0, 4.2f, 3.0f, 4.2f)
                    drawMeshWithColor(sph, modelMatrix, floatArrayOf(0.18f, 0.52f, 0.22f, 1f))

                    // Broadleaf Fern Clusters around base of tree
                    if (fern != null) {
                        Matrix.setIdentityM(modelMatrix, 0)
                        Matrix.translateM(modelMatrix, 0, tree.x + 1.2f, 0.1f, tree.z + 0.8f)
                        drawMesh(fern, modelMatrix)
                    }

                    // Bioluminescent mushrooms on tree hollows
                    if (mushroom != null) {
                        Matrix.setIdentityM(modelMatrix, 0)
                        Matrix.translateM(modelMatrix, 0, tree.x - 0.9f, 0.1f, tree.z - 0.7f)
                        drawMesh(mushroom, modelMatrix)
                    }
                }

                // Mossy jungle boulders with fern cover
                for (rock in rocks) {
                    Matrix.setIdentityM(modelMatrix, 0)
                    Matrix.translateM(modelMatrix, 0, rock.x, 0.8f, rock.z)
                    Matrix.scaleM(modelMatrix, 0, 2.4f, 1.5f, 2.0f)
                    drawMeshWithColor(box, modelMatrix, floatArrayOf(0.38f, 0.42f, 0.35f, 1f))

                    if (fern != null) {
                        Matrix.setIdentityM(modelMatrix, 0)
                        Matrix.translateM(modelMatrix, 0, rock.x - 1.1f, 0.1f, rock.z + 0.9f)
                        drawMesh(fern, modelMatrix)
                    }
                }
            }

            EnvironmentBiome.HIGHLANDS -> {
                // Alpine mountain conifers & craggy cliffs
                for (tree in trees) {
                    // Slim trunk
                    Matrix.setIdentityM(modelMatrix, 0)
                    Matrix.translateM(modelMatrix, 0, tree.x, 4.0f, tree.z)
                    Matrix.scaleM(modelMatrix, 0, 1.1f, 8f, 1.1f)
                    drawMeshWithColor(cyl, modelMatrix, floatArrayOf(0.28f, 0.22f, 0.18f, 1f))

                    // Conical pine tiers
                    Matrix.setIdentityM(modelMatrix, 0)
                    Matrix.translateM(modelMatrix, 0, tree.x, 7.5f, tree.z)
                    Matrix.scaleM(modelMatrix, 0, 4.2f, 3.5f, 4.2f)
                    drawMeshWithColor(sph, modelMatrix, floatArrayOf(0.14f, 0.32f, 0.20f, 1f))

                    Matrix.setIdentityM(modelMatrix, 0)
                    Matrix.translateM(modelMatrix, 0, tree.x, 10.5f, tree.z)
                    Matrix.scaleM(modelMatrix, 0, 2.8f, 3.0f, 2.8f)
                    drawMeshWithColor(sph, modelMatrix, floatArrayOf(0.18f, 0.38f, 0.24f, 1f))
                }

                // Alpine slate rocks
                for (rock in rocks) {
                    Matrix.setIdentityM(modelMatrix, 0)
                    Matrix.translateM(modelMatrix, 0, rock.x, 1.2f, rock.z)
                    Matrix.scaleM(modelMatrix, 0, 3.2f, 2.4f, 2.8f)
                    drawMeshWithColor(box, modelMatrix, floatArrayOf(0.45f, 0.46f, 0.48f, 1f))
                }
            }

            EnvironmentBiome.CITY -> {
                // Skyscraper towers and urban architecture
                for (tree in trees) {
                    // Concrete Skyscraper Building Block
                    val buildingHeight = 16f + (abs(tree.x) % 8f) * 2f
                    Matrix.setIdentityM(modelMatrix, 0)
                    Matrix.translateM(modelMatrix, 0, tree.x, buildingHeight * 0.5f, tree.z)
                    Matrix.scaleM(modelMatrix, 0, 6.5f, buildingHeight, 6.5f)
                    drawMeshWithColor(box, modelMatrix, floatArrayOf(0.22f, 0.24f, 0.28f, 1f))

                    // Roof parapet / glowing antenna
                    Matrix.setIdentityM(modelMatrix, 0)
                    Matrix.translateM(modelMatrix, 0, tree.x, buildingHeight + 1.2f, tree.z)
                    Matrix.scaleM(modelMatrix, 0, 0.5f, 2.4f, 0.5f)
                    drawMeshWithColor(cyl, modelMatrix, floatArrayOf(0.9f, 0.3f, 0.2f, 1f))
                }

                // Street concrete barriers / vehicles
                for (rock in rocks) {
                    Matrix.setIdentityM(modelMatrix, 0)
                    Matrix.translateM(modelMatrix, 0, rock.x, 0.6f, rock.z)
                    Matrix.scaleM(modelMatrix, 0, 2.2f, 1.2f, 1.4f)
                    drawMeshWithColor(box, modelMatrix, floatArrayOf(0.85f, 0.85f, 0.30f, 1f))
                }
            }
        }
    }

    /**
     * Draws the realistic 3D Jumping Spider character model based on user reference images.
     * Features:
     * - Sculpted Cephalothorax & egg-shaped Abdomen with dark dorsal chevrons
     * - Dense multi-layered fur shells & hair-card bristles catching light for a fuzzy silhouette
     * - 6 glossy black 3D eye spheres with specular catchlights
     * - Chelicerae fangs with vertical red stripes & plush pedipalps
     * - 8 multi-segment articulated legs with Coxa, Femur, Tibia, and dark Tarsus claw tips
     */
    private fun drawSpider() {
        val model = spiderModel ?: return

        val spPos = spider.position
        val yaw = spider.yawDeg

        // Base transform for spider center
        val baseTransform = TransformMatrix()
            .translate(spPos.x, spPos.y, spPos.z)
            .rotate(yaw, 0f, 1f, 0f)

        // 1. Cephalothorax (Sculpted Head with Ocular Turret)
        val headT = baseTransform.copy().translate(0f, 0.28f, 0.35f)
        drawMesh(model.cephalothoraxMesh, headT.matrix)

        // 2. Abdomen (Anatomical Egg-shape with Dark Dorsal Chevrons)
        val abdomenT = baseTransform.copy().translate(0f, 0.48f, -0.78f)
        drawMesh(model.abdomenMesh, abdomenT.matrix)

        // 3. Fur Shells & Hair Bristling (Fuzzy jumping-spider silhouette)
        val furT = baseTransform.copy().translate(0f, 0.35f, -0.20f)
        drawMesh(model.furShellMesh, furT.matrix)

        // 4. Eye Group (6 Glossy Black 3D Eye Spheres + Specular Catchlights)
        val eyeT = baseTransform.copy().translate(0f, 0.28f, 0.35f)
        drawMesh(model.eyeGroupMesh, eyeT.matrix)

        // 5. Mouth & Chelicerae Fangs (With Red Vertical Stripes & Bite Animation)
        val biteOffset = if (spider.biteAnimTimer > 0f) sin(spider.biteAnimTimer * 20f) * 0.12f else 0f
        val mouthT = baseTransform.copy().translate(0f, 0.16f + biteOffset, 0.35f)
        drawMesh(model.mouthGroupMesh, mouthT.matrix)

        // 6. Pedipalps (Left & Right plush furry front limbs)
        val pedipalpLT = baseTransform.copy().translate(-0.35f, 0.20f, 0.88f)
        val pedipalpRT = baseTransform.copy().translate(0.35f, 0.20f, 0.88f)
        drawMesh(model.pedipalpMesh, pedipalpLT.matrix)
        drawMesh(model.pedipalpMesh, pedipalpRT.matrix)

        // 7. Eight Multi-Segment Articulated Legs (Femur, Tibia, Tarsus with Black Claw Tips)
        val legBaseAngles = floatArrayOf(42f, 78f, 112f, 142f)

        for (i in 0..3) {
            val baseAngleDeg = legBaseAngles[i]
            val swingLeft = spider.legAnglesLeft[i]
            val swingRight = spider.legAnglesRight[i]

            // --- LEFT LEG ---
            val lAngle = baseAngleDeg + GameMath.radToDeg(swingLeft)
            val lRad = GameMath.degToRad(lAngle)
            val rootLX = -0.58f * sin(lRad)
            val rootLZ = -0.1f + 0.6f * cos(lRad)

            // Femur (Upper leg)
            val femurLT = baseTransform.copy()
                .translate(rootLX, 0.38f, rootLZ)
                .rotate(-lAngle - 20f, 0f, 1f, 0f)
                .rotate(-35f + swingLeft * 15f, 0f, 0f, 1f)
            drawMesh(model.legFemurMesh, femurLT.matrix)

            // Tibia (Lower leg)
            val tibiaLT = baseTransform.copy()
                .translate(rootLX - 0.95f * sin(lRad), 0.58f, rootLZ + 0.75f * cos(lRad))
                .rotate(-lAngle - 10f, 0f, 1f, 0f)
                .rotate(48f, 0f, 0f, 1f)
            drawMesh(model.legTibiaMesh, tibiaLT.matrix)

            // Tarsus (Black claw tip)
            val tarsusLT = baseTransform.copy()
                .translate(rootLX - 1.52f * sin(lRad), 0.12f, rootLZ + 1.20f * cos(lRad))
                .rotate(-lAngle - 10f, 0f, 1f, 0f)
                .rotate(65f, 0f, 0f, 1f)
            drawMesh(model.legTarsusMesh, tarsusLT.matrix)

            // --- RIGHT LEG ---
            val rAngle = baseAngleDeg + GameMath.radToDeg(swingRight)
            val rRad = GameMath.degToRad(rAngle)
            val rootRX = 0.58f * sin(rRad)
            val rootRZ = -0.1f + 0.6f * cos(rRad)

            // Femur (Upper leg Right)
            val femurRT = baseTransform.copy()
                .translate(rootRX, 0.38f, rootRZ)
                .rotate(rAngle + 20f, 0f, 1f, 0f)
                .rotate(35f - swingRight * 15f, 0f, 0f, 1f)
                .rotate(180f, 0f, 1f, 0f)
            drawMesh(model.legFemurMesh, femurRT.matrix)

            // Tibia (Lower leg Right)
            val tibiaRT = baseTransform.copy()
                .translate(rootRX + 0.95f * sin(rRad), 0.58f, rootRZ + 0.75f * cos(rRad))
                .rotate(rAngle + 10f, 0f, 1f, 0f)
                .rotate(-48f, 0f, 0f, 1f)
                .rotate(180f, 0f, 1f, 0f)
            drawMesh(model.legTibiaMesh, tibiaRT.matrix)

            // Tarsus (Black claw tip Right)
            val tarsusRT = baseTransform.copy()
                .translate(rootRX + 1.52f * sin(rRad), 0.12f, rootRZ + 1.20f * cos(rRad))
                .rotate(rAngle + 10f, 0f, 1f, 0f)
                .rotate(-65f, 0f, 0f, 1f)
                .rotate(180f, 0f, 1f, 0f)
            drawMesh(model.legTarsusMesh, tarsusRT.matrix)
        }
    }

    /**
     * Draws the Giant Car-Sized Apex Frog Predator.
     */
    private fun drawFrog() {
        val sph = sphereMesh ?: return
        val wMesh = webMesh ?: return

        val fPos = frog.position
        val scale = frog.scale
        val yaw = frog.yawDeg
        val leapY = frog.verticalOffsetY

        val frogTransform = TransformMatrix()
            .translate(fPos.x, fPos.y + leapY, fPos.z)
            .rotate(yaw, 0f, 1f, 0f)
            .scale(scale, scale, scale)

        // Body color: Toxic Rainforest Green / Olive with Dark Spots
        val frogGreen = floatArrayOf(0.22f, 0.58f, 0.18f, 1f)
        val frogBelly = floatArrayOf(0.72f, 0.78f, 0.35f, 1f)

        // 1. Massive Frog Torso
        val bodyT = frogTransform.copy().translate(0f, 0.65f, 0f).scale(1.2f, 0.85f, 1.4f)
        drawMeshWithColor(sph, bodyT.matrix, frogGreen)

        // 2. Pulsating Throat Sac
        val throatSize = 0.55f + sin(frog.throatPulse) * 0.08f
        val throatT = frogTransform.copy().translate(0f, 0.35f, 0.7f).scale(0.85f, throatSize, 0.85f)
        drawMeshWithColor(sph, throatT.matrix, frogBelly)

        // 3. Huge Amber Predatory Eyes
        val eyeLeft = frogTransform.copy().translate(-0.55f, 1.25f, 0.65f).scale(0.32f, 0.32f, 0.32f)
        val eyeRight = frogTransform.copy().translate(0.55f, 1.25f, 0.65f).scale(0.32f, 0.32f, 0.32f)
        drawMeshWithColor(sph, eyeLeft.matrix, floatArrayOf(0.95f, 0.75f, 0.05f, 1f))
        drawMeshWithColor(sph, eyeRight.matrix, floatArrayOf(0.95f, 0.75f, 0.05f, 1f))

        // Pupil Slits
        val pupilL = frogTransform.copy().translate(-0.55f, 1.28f, 0.95f).scale(0.08f, 0.22f, 0.08f)
        val pupilR = frogTransform.copy().translate(0.55f, 1.28f, 0.95f).scale(0.08f, 0.22f, 0.08f)
        drawMeshWithColor(sph, pupilL.matrix, floatArrayOf(0.02f, 0.02f, 0.02f, 1f))
        drawMeshWithColor(sph, pupilR.matrix, floatArrayOf(0.02f, 0.02f, 0.02f, 1f))

        // 4. Large Hind Legs
        val hindLegL = frogTransform.copy().translate(-0.95f, 0.5f, -0.65f).scale(0.55f, 0.7f, 1.1f)
        val hindLegR = frogTransform.copy().translate(0.95f, 0.5f, -0.65f).scale(0.55f, 0.7f, 1.1f)
        drawMeshWithColor(sph, hindLegL.matrix, frogGreen)
        drawMeshWithColor(sph, hindLegR.matrix, frogGreen)

        // 5. If Webbed/Stunned: Render Sticky Mesh over Frog
        if (frog.state == FrogState.STUNNED) {
            val webT = TransformMatrix()
                .translate(fPos.x, fPos.y + 1.2f, fPos.z)
                .rotate(yaw, 0f, 1f, 0f)
                .scale(2.2f, 2.2f, 2.2f)
            drawMeshWithColor(wMesh, webT.matrix, floatArrayOf(0.98f, 0.98f, 1f, 0.88f))
        }
    }

    /**
     * Draws huntable animal-sized prey (Agouti, Lizard, Beetle).
     */
    private fun drawPreyAnimals() {
        val sph = sphereMesh ?: return
        val box = boxMesh ?: return

        for (prey in preyList) {
            if (!prey.isAlive) continue

            val pPos = prey.position
            val scale = prey.type.sizeScale
            val yaw = prey.yawDeg

            val preyTransform = TransformMatrix()
                .translate(pPos.x, pPos.y, pPos.z)
                .rotate(yaw, 0f, 1f, 0f)
                .scale(scale, scale, scale)

            when (prey.type) {
                PreyType.JUNGLE_RODENT -> {
                    // Furry Agouti rodent (warm brown body + head + ears)
                    val bodyT = preyTransform.copy().translate(0f, 0.35f, 0f).scale(0.55f, 0.42f, 0.85f)
                    val headT = preyTransform.copy().translate(0f, 0.5f, 0.65f).scale(0.35f, 0.32f, 0.38f)
                    drawMeshWithColor(sph, bodyT.matrix, floatArrayOf(0.52f, 0.35f, 0.18f, 1f))
                    drawMeshWithColor(sph, headT.matrix, floatArrayOf(0.48f, 0.30f, 0.14f, 1f))
                }
                PreyType.FOREST_LIZARD -> {
                    // Green Iguana with tail
                    val bodyT = preyTransform.copy().translate(0f, 0.25f, 0f).scale(0.45f, 0.25f, 1.15f)
                    val headT = preyTransform.copy().translate(0f, 0.32f, 0.9f).scale(0.35f, 0.22f, 0.42f)
                    drawMeshWithColor(sph, bodyT.matrix, floatArrayOf(0.28f, 0.68f, 0.25f, 1f))
                    drawMeshWithColor(sph, headT.matrix, floatArrayOf(0.35f, 0.75f, 0.30f, 1f))
                }
                PreyType.GIANT_BEETLE -> {
                    // Dark iridescence chitin beetle
                    val bodyT = preyTransform.copy().translate(0f, 0.28f, 0f).scale(0.6f, 0.35f, 0.75f)
                    drawMeshWithColor(box, bodyT.matrix, floatArrayOf(0.12f, 0.15f, 0.22f, 1f))
                }
            }

            // Status Effect Visuals (Paralyzed or Slowed sparkles)
            if (prey.state == PreyState.PARALYZED) {
                val auraT = preyTransform.copy().translate(0f, 0.55f, 0f).scale(0.7f, 0.7f, 0.7f)
                drawMeshWithColor(sph, auraT.matrix, floatArrayOf(0.9f, 0.95f, 1f, 0.45f))
            }
        }
    }

    /**
     * Draws Projectiles (Venom Spit and Expanding Web Net).
     */
    private fun drawProjectiles() {
        val sMesh = spitMesh ?: return
        val wMesh = webMesh ?: return

        for (proj in projectiles) {
            if (proj.isExpired) continue

            Matrix.setIdentityM(modelMatrix, 0)
            Matrix.translateM(modelMatrix, 0, proj.position.x, proj.position.y, proj.position.z)

            if (proj.type == ProjectileType.SPIT) {
                Matrix.scaleM(modelMatrix, 0, 0.55f, 0.55f, 0.55f)
                drawMeshWithColor(sMesh, modelMatrix, floatArrayOf(0.95f, 1f, 0.88f, 0.95f))
            } else {
                val scale = (1.5f + (3.5f - proj.lifeTimeSeconds) * 0.8f).coerceAtMost(2.8f)
                Matrix.scaleM(modelMatrix, 0, scale, scale, scale)
                drawMeshWithColor(wMesh, modelMatrix, floatArrayOf(1f, 1f, 1f, 0.85f))
            }
        }
    }

    /**
     * Raindrops, spores and ambient jungle particles.
     */
    private fun drawRainAndSpores(camX: Float, camY: Float, camZ: Float, deltaSeconds: Float) {
        val sph = sphereMesh ?: return
        val isRain = session.weather == Weather.RAIN

        for (i in 0 until rainParticleCount) {
            // Drop physics
            val fallSpeed = if (isRain) 28f else 1.8f
            rainParticlesY[i] -= fallSpeed * deltaSeconds
            if (rainParticlesY[i] < 0f) {
                rainParticlesY[i] = 18f
                rainParticlesX[i] = camX + (Math.random().toFloat() * 60f) - 30f
                rainParticlesZ[i] = camZ + (Math.random().toFloat() * 60f) - 30f
            }

            Matrix.setIdentityM(modelMatrix, 0)
            Matrix.translateM(modelMatrix, 0, rainParticlesX[i], rainParticlesY[i], rainParticlesZ[i])
            if (isRain) {
                Matrix.scaleM(modelMatrix, 0, 0.04f, 0.35f, 0.04f)
                drawMeshWithColor(sph, modelMatrix, floatArrayOf(0.7f, 0.85f, 1.0f, 0.6f))
            } else {
                Matrix.scaleM(modelMatrix, 0, 0.08f, 0.08f, 0.08f)
                drawMeshWithColor(sph, modelMatrix, floatArrayOf(0.85f, 0.95f, 0.65f, 0.5f))
            }
        }
    }

    private fun drawMesh(mesh: Mesh, modelM: FloatArray) {
        Matrix.multiplyMM(mvpMatrix, 0, viewMatrix, 0, modelM, 0)
        Matrix.multiplyMM(tempMatrix, 0, projectionMatrix, 0, mvpMatrix, 0)
        System.arraycopy(tempMatrix, 0, mvpMatrix, 0, 16)

        GLES20.glUniformMatrix4fv(uMVPMatrixHandle, 1, false, mvpMatrix, 0)
        GLES20.glUniformMatrix4fv(uModelMatrixHandle, 1, false, modelM, 0)
        GLES20.glUniform1f(uUseOverrideColorHandle, 0.0f)

        mesh.vertexBuffer.position(0)
        GLES20.glVertexAttribPointer(aPositionHandle, 3, GLES20.GL_FLOAT, false, MeshBuilder.VERTEX_STRIDE, mesh.vertexBuffer)
        GLES20.glEnableVertexAttribArray(aPositionHandle)

        mesh.vertexBuffer.position(3)
        GLES20.glVertexAttribPointer(aNormalHandle, 3, GLES20.GL_FLOAT, false, MeshBuilder.VERTEX_STRIDE, mesh.vertexBuffer)
        GLES20.glEnableVertexAttribArray(aNormalHandle)

        mesh.vertexBuffer.position(6)
        GLES20.glVertexAttribPointer(aColorHandle, 4, GLES20.GL_FLOAT, false, MeshBuilder.VERTEX_STRIDE, mesh.vertexBuffer)
        GLES20.glEnableVertexAttribArray(aColorHandle)

        mesh.indexBuffer.position(0)
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, mesh.indexCount, GLES20.GL_UNSIGNED_SHORT, mesh.indexBuffer)

        GLES20.glDisableVertexAttribArray(aPositionHandle)
        GLES20.glDisableVertexAttribArray(aNormalHandle)
        GLES20.glDisableVertexAttribArray(aColorHandle)
    }

    private fun drawMeshWithColor(mesh: Mesh, modelM: FloatArray, overrideColor: FloatArray) {
        Matrix.multiplyMM(mvpMatrix, 0, viewMatrix, 0, modelM, 0)
        Matrix.multiplyMM(tempMatrix, 0, projectionMatrix, 0, mvpMatrix, 0)
        System.arraycopy(tempMatrix, 0, mvpMatrix, 0, 16)

        GLES20.glUniformMatrix4fv(uMVPMatrixHandle, 1, false, mvpMatrix, 0)
        GLES20.glUniformMatrix4fv(uModelMatrixHandle, 1, false, modelM, 0)
        GLES20.glUniform1f(uUseOverrideColorHandle, 1.0f)
        GLES20.glUniform4fv(uOverrideColorHandle, 1, overrideColor, 0)

        mesh.vertexBuffer.position(0)
        GLES20.glVertexAttribPointer(aPositionHandle, 3, GLES20.GL_FLOAT, false, MeshBuilder.VERTEX_STRIDE, mesh.vertexBuffer)
        GLES20.glEnableVertexAttribArray(aPositionHandle)

        mesh.vertexBuffer.position(3)
        GLES20.glVertexAttribPointer(aNormalHandle, 3, GLES20.GL_FLOAT, false, MeshBuilder.VERTEX_STRIDE, mesh.vertexBuffer)
        GLES20.glEnableVertexAttribArray(aNormalHandle)

        mesh.vertexBuffer.position(6)
        GLES20.glVertexAttribPointer(aColorHandle, 4, GLES20.GL_FLOAT, false, MeshBuilder.VERTEX_STRIDE, mesh.vertexBuffer)
        GLES20.glEnableVertexAttribArray(aColorHandle)

        mesh.indexBuffer.position(0)
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, mesh.indexCount, GLES20.GL_UNSIGNED_SHORT, mesh.indexBuffer)

        GLES20.glDisableVertexAttribArray(aPositionHandle)
        GLES20.glDisableVertexAttribArray(aNormalHandle)
        GLES20.glDisableVertexAttribArray(aColorHandle)
    }
}
