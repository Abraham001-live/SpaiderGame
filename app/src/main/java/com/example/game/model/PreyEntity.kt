package com.example.game.model

import com.example.game.engine.GameMath
import com.example.game.engine.Vec3
import kotlin.math.*

enum class PreyType(val displayName: String, val sizeScale: Float, val baseSpeed: Float, val foodGain: Float) {
    JUNGLE_RODENT("Amazon Agouti", 0.9f, 4.2f, 35f),
    FOREST_LIZARD("Jungle Iguana", 1.1f, 3.8f, 45f),
    GIANT_BEETLE("Titan Beetle", 0.7f, 2.6f, 25f)
}

enum class PreyState {
    WANDER,
    GRAZE,
    FLEE,
    SLOWED,
    PARALYZED
}

class PreyEntity(
    val id: Int,
    val type: PreyType,
    var position: Vec3 = Vec3(0f, 0f, 0f),
    var yawDeg: Float = 0f
) {
    var state: PreyState = PreyState.WANDER
    var isAlive: Boolean = true

    var speedModifier: Float = 1.0f
    var statusEffectTimer: Float = 0f

    private var stateTimer: Float = (2f + Math.random().toFloat() * 3f)
    private var targetYawDeg: Float = yawDeg

    // Procedural leg/walking animation phase
    var walkAnimPhase: Float = 0f

    fun update(deltaSeconds: Float, spiderPos: Vec3) {
        if (!isAlive) return

        // Update status effects from spit
        if (statusEffectTimer > 0f) {
            statusEffectTimer -= deltaSeconds
            if (statusEffectTimer <= 0f) {
                state = PreyState.WANDER
                speedModifier = 1.0f
            }
        }

        val distToSpider = position.distanceTo(spiderPos)

        // React to spider proximity if not paralyzed
        if (state != PreyState.PARALYZED) {
            if (distToSpider < 7.5f) {
                state = PreyState.FLEE
                // Face directly away from spider
                val dx = position.x - spiderPos.x
                val dz = position.z - spiderPos.z
                targetYawDeg = GameMath.radToDeg(atan2(dx, dz))
            } else if (state == PreyState.FLEE && distToSpider > 12f) {
                state = PreyState.WANDER
            }
        }

        // State behavior
        when (state) {
            PreyState.WANDER -> {
                stateTimer -= deltaSeconds
                if (stateTimer <= 0f) {
                    if (Math.random() < 0.4) {
                        state = PreyState.GRAZE
                        stateTimer = 2f + Math.random().toFloat() * 2.5f
                    } else {
                        targetYawDeg = (Math.random() * 360f).toFloat()
                        stateTimer = 2.5f + Math.random().toFloat() * 3f
                    }
                }
                moveForward(deltaSeconds, type.baseSpeed * 0.5f * speedModifier)
            }
            PreyState.GRAZE -> {
                stateTimer -= deltaSeconds
                if (stateTimer <= 0f) {
                    state = PreyState.WANDER
                    stateTimer = 3f
                }
            }
            PreyState.FLEE -> {
                moveForward(deltaSeconds, type.baseSpeed * 1.35f * speedModifier)
            }
            PreyState.SLOWED -> {
                moveForward(deltaSeconds, type.baseSpeed * 0.35f)
            }
            PreyState.PARALYZED -> {
                // Completely stopped
            }
        }

        // Clamp to jungle boundaries & Terrain Grounding
        position.x = position.x.coerceIn(-48f, 48f)
        position.z = position.z.coerceIn(-48f, 48f)
        position.y = GameMath.getTerrainHeight(position.x, position.z) + 0.35f
    }

    private fun moveForward(deltaSeconds: Float, speed: Float) {
        yawDeg = GameMath.lerpAngleDeg(yawDeg, targetYawDeg, deltaSeconds * 4.5f)
        val rad = GameMath.degToRad(yawDeg)
        val vx = sin(rad) * speed
        val vz = cos(rad) * speed

        position.x += vx * deltaSeconds
        position.z += vz * deltaSeconds

        walkAnimPhase += speed * deltaSeconds * 8f
    }

    /**
     * Hit by spit weapon: Small prey paralyzed, medium/large slowed.
     */
    fun onHitBySpit() {
        when (type) {
            PreyType.GIANT_BEETLE -> {
                state = PreyState.PARALYZED
                statusEffectTimer = 4.5f
                speedModifier = 0.0f
            }
            PreyType.JUNGLE_RODENT -> {
                state = PreyState.PARALYZED
                statusEffectTimer = 3.2f
                speedModifier = 0.0f
            }
            PreyType.FOREST_LIZARD -> {
                state = PreyState.SLOWED
                statusEffectTimer = 3.5f
                speedModifier = 0.35f
            }
        }
    }
}
