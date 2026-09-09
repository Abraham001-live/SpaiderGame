package com.example.game.model

import com.example.game.engine.GameMath
import com.example.game.engine.Vec3
import kotlin.math.*

enum class FrogState(val label: String) {
    IDLE("Lurking in Foliage"),
    PATROL("Patrolling Rainforest"),
    NOTICE_SPIDER("Stalking Spider!"),
    CHASE("CHASING SPIDER!"),
    ATTACK("Apex Strike!"),
    SEARCH("Searching Area..."),
    STUNNED("ENTANGLED IN WEB!"),
    RETURN_TO_PATROL("Returning to Swamp")
}

class FrogEntity(
    var position: Vec3 = Vec3(0f, 0f, 25f),
    var yawDeg: Float = 180f
) {
    val scale: Float = 3.5f // Car-sized massive predator

    var state: FrogState = FrogState.IDLE
    var stunTimer: Float = 0f
    var maxStunDuration: Float = 6.0f

    var speed: Float = 0f
    var baseChaseSpeed: Float = 6.8f
    var detectionRadius: Float = 26f

    // Ground leap simulation
    var verticalOffsetY: Float = 0f
    var isLeaping: Boolean = false
    var leapTimer: Float = 0f

    // AI state variables
    private var stateTimer: Float = 4.0f
    private var targetYawDeg: Float = yawDeg
    private var searchTimer: Float = 0f
    private var noticeDelayTimer: Float = 0f
    private var lastKnownSpiderPos = Vec3(0f, 0f, 0f)

    var throatPulse: Float = 0f
    var attackCooldown: Float = 0f

    fun update(
        deltaSeconds: Float,
        spiderPos: Vec3,
        spiderIsAlive: Boolean,
        onAttackSpider: () -> Unit
    ) {
        throatPulse += deltaSeconds * 4.5f
        if (attackCooldown > 0f) attackCooldown -= deltaSeconds

        val distToSpider = position.distanceTo(spiderPos)

        // Handle Web Stun state
        if (state == FrogState.STUNNED) {
            stunTimer -= deltaSeconds
            speed = 0f
            verticalOffsetY = 0f
            if (stunTimer <= 0f) {
                // Recovers and searches or chases immediately
                state = if (distToSpider < 20f && spiderIsAlive) FrogState.CHASE else FrogState.SEARCH
                searchTimer = 4.0f
            }
            return
        }

        // State Machine Execution
        when (state) {
            FrogState.IDLE -> {
                stateTimer -= deltaSeconds
                if (distToSpider < detectionRadius && spiderIsAlive) {
                    noticeDelayTimer = 1.0f
                    state = FrogState.NOTICE_SPIDER
                } else if (stateTimer <= 0f) {
                    state = FrogState.PATROL
                    targetYawDeg = (Math.random() * 360f).toFloat()
                    stateTimer = 5.0f + Math.random().toFloat() * 4f
                }
            }

            FrogState.PATROL -> {
                stateTimer -= deltaSeconds
                if (distToSpider < detectionRadius && spiderIsAlive) {
                    noticeDelayTimer = 0.8f
                    state = FrogState.NOTICE_SPIDER
                } else if (stateTimer <= 0f) {
                    state = FrogState.IDLE
                    stateTimer = 3.0f + Math.random().toFloat() * 2f
                } else {
                    performHopMovement(deltaSeconds, 2.8f)
                }
            }

            FrogState.NOTICE_SPIDER -> {
                // Turn towards spider with menacing pause
                val dx = spiderPos.x - position.x
                val dz = spiderPos.z - position.z
                targetYawDeg = GameMath.radToDeg(atan2(dx, dz))
                yawDeg = GameMath.lerpAngleDeg(yawDeg, targetYawDeg, deltaSeconds * 6f)

                noticeDelayTimer -= deltaSeconds
                if (noticeDelayTimer <= 0f) {
                    state = FrogState.CHASE
                    lastKnownSpiderPos.set(spiderPos)
                }
            }

            FrogState.CHASE -> {
                if (!spiderIsAlive) {
                    state = FrogState.IDLE
                    return
                }

                lastKnownSpiderPos.set(spiderPos)
                val dx = spiderPos.x - position.x
                val dz = spiderPos.z - position.z
                targetYawDeg = GameMath.radToDeg(atan2(dx, dz))

                performHopMovement(deltaSeconds, baseChaseSpeed)

                // Check for attack range (huge tongue / lunge strike)
                if (distToSpider < 3.8f && attackCooldown <= 0f) {
                    state = FrogState.ATTACK
                    stateTimer = 0.8f
                    attackCooldown = 2.0f
                    onAttackSpider()
                } else if (distToSpider > 42f) {
                    // Spider managed to run far away
                    state = FrogState.SEARCH
                    searchTimer = 6.0f
                }
            }

            FrogState.ATTACK -> {
                stateTimer -= deltaSeconds
                verticalOffsetY = max(0f, sin(stateTimer * Math.PI.toFloat() / 0.8f) * 1.5f)
                if (stateTimer <= 0f) {
                    state = if (distToSpider < 25f && spiderIsAlive) FrogState.CHASE else FrogState.SEARCH
                }
            }

            FrogState.SEARCH -> {
                searchTimer -= deltaSeconds
                // Look around in different directions
                if (searchTimer % 1.5f < 0.1f) {
                    targetYawDeg = (yawDeg + 90f + Math.random().toFloat() * 60f)
                }
                yawDeg = GameMath.lerpAngleDeg(yawDeg, targetYawDeg, deltaSeconds * 3f)

                if (distToSpider < detectionRadius && spiderIsAlive) {
                    state = FrogState.CHASE
                } else if (searchTimer <= 0f) {
                    state = FrogState.RETURN_TO_PATROL
                    stateTimer = 6.0f
                }
            }

            FrogState.RETURN_TO_PATROL -> {
                stateTimer -= deltaSeconds
                performHopMovement(deltaSeconds, 2.5f)
                if (distToSpider < detectionRadius && spiderIsAlive) {
                    state = FrogState.NOTICE_SPIDER
                    noticeDelayTimer = 0.8f
                } else if (stateTimer <= 0f) {
                    state = FrogState.PATROL
                    stateTimer = 6.0f
                }
            }

            FrogState.STUNNED -> {
                // Handled above in stun check
            }
        }

        // Boundaries & Terrain Grounding
        position.x = position.x.coerceIn(-52f, 52f)
        position.z = position.z.coerceIn(-52f, 52f)
        position.y = GameMath.getTerrainHeight(position.x, position.z) + 0.5f
    }

    private fun performHopMovement(deltaSeconds: Float, moveSpeed: Float) {
        yawDeg = GameMath.lerpAngleDeg(yawDeg, targetYawDeg, deltaSeconds * 5f)

        leapTimer += deltaSeconds * (moveSpeed * 0.9f)
        val hopCycle = leapTimer % 1.8f

        if (hopCycle < 0.7f) {
            // In air hopping forward
            isLeaping = true
            val hopNorm = hopCycle / 0.7f
            verticalOffsetY = sin(hopNorm * Math.PI.toFloat()) * 1.8f

            val rad = GameMath.degToRad(yawDeg)
            val currentSpeed = moveSpeed * 1.5f
            position.x += sin(rad) * currentSpeed * deltaSeconds
            position.z += cos(rad) * currentSpeed * deltaSeconds
        } else {
            // On ground resting / preparing next hop
            isLeaping = false
            verticalOffsetY = 0f
        }
    }

    /**
     * Hit by Spider Web: Stuns the frog and gives spider escape window.
     */
    fun onHitByWeb(customDuration: Float = 6.0f) {
        state = FrogState.STUNNED
        maxStunDuration = customDuration
        stunTimer = customDuration
        isLeaping = false
        verticalOffsetY = 0f
    }
}
