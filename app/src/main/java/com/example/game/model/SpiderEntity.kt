package com.example.game.model

import com.example.game.engine.GameMath
import com.example.game.engine.Vec3
import kotlin.math.*

class SpiderEntity(
    var position: Vec3 = Vec3(0f, 0.4f, 0f),
    var yawDeg: Float = 0f
) {
    val scale: Float = 1.0f

    // Survival Attributes
    var maxHealth: Float = 100f
    var health: Float = 100f

    var maxFood: Float = 100f
    var food: Float = 85f

    var maxEnergy: Float = 100f
    var energy: Float = 100f

    var isAlive: Boolean = true

    // Locomotion & Physics
    var isJumping: Boolean = false
    var jumpVelocityY: Float = 0f
    var groundY: Float = 0.4f
    var walkAnimPhase: Float = 0f
    var isSprinting: Boolean = false

    // Action Cooldowns and Timers
    var spitCooldown: Float = 0f
    var webCooldown: Float = 0f
    var biteAnimTimer: Float = 0f
    var spitAnimTimer: Float = 0f
    var webAnimTimer: Float = 0f

    // 8 Legs procedural tripod gait offsets (angles in radians for 4 left, 4 right legs)
    val legAnglesLeft = FloatArray(4)
    val legAnglesRight = FloatArray(4)

    // Direction vector of the spider
    val forwardVector: Vec3
        get() {
            val rad = GameMath.degToRad(yawDeg)
            return Vec3(sin(rad), 0f, cos(rad))
        }

    fun update(
        deltaSeconds: Float,
        moveInputX: Float,
        moveInputZ: Float,
        cameraYawDeg: Float,
        foodDrainRate: Float = 1.2f,
        energyDrainMult: Float = 1.0f
    ) {
        if (!isAlive) return

        // Update action cooldowns
        if (spitCooldown > 0f) spitCooldown -= deltaSeconds
        if (webCooldown > 0f) webCooldown -= deltaSeconds
        if (biteAnimTimer > 0f) biteAnimTimer -= deltaSeconds
        if (spitAnimTimer > 0f) spitAnimTimer -= deltaSeconds
        if (webAnimTimer > 0f) webAnimTimer -= deltaSeconds

        // Food & Energy Drain Loop
        val foodDepletion = foodDrainRate * deltaSeconds
        food = (food - foodDepletion).coerceAtLeast(0f)

        // If food is low (< 25%), energy drains faster and recovers slower
        val isStarving = food <= 20f
        val starvationFactor = if (isStarving) 2.2f else 1.0f

        // Handle Direct Third-Person Movement Controls
        val isTurning = abs(moveInputX) > 0.05f
        val isMovingLinear = abs(moveInputZ) > 0.05f
        val isMoving = isTurning || isMovingLinear

        if (isMoving) {
            // 1. Turning: Left and Right controls turn the spider's facing direction
            if (isTurning) {
                val turnSpeedDeg = 120f
                yawDeg = GameMath.normalizeAngleDeg(yawDeg + moveInputX * turnSpeedDeg * deltaSeconds)
            }

            // 2. Forward / Reverse: Forward moves along facing direction; Backward reverses along facing direction without turning around
            if (isMovingLinear) {
                val baseSpeed = if (isSprinting && energy > 10f) 8.5f else 5.2f
                val moveSpeed = baseSpeed * moveInputZ // Positive for forward, negative for reverse

                val rad = GameMath.degToRad(yawDeg)
                val vx = sin(rad) * moveSpeed
                val vz = cos(rad) * moveSpeed

                position.x += vx * deltaSeconds
                position.z += vz * deltaSeconds

                // Energy drain when sprinting/moving
                if (isSprinting) {
                    energy = (energy - 12f * deltaSeconds * starvationFactor).coerceAtLeast(0f)
                    if (energy <= 2f) isSprinting = false
                } else {
                    energy = (energy - 1.5f * deltaSeconds * starvationFactor).coerceAtLeast(0f)
                }

                // Animate 8 legs with tripod gait
                walkAnimPhase += abs(moveSpeed) * deltaSeconds * 9f
            } else if (isTurning) {
                // Leg animation while turning on the spot
                walkAnimPhase += 3.5f * deltaSeconds * 9f
            }
        } else {
            // Idle energy recovery
            val recoveryRate = if (isStarving) 4f else 9f
            energy = (energy + recoveryRate * deltaSeconds).coerceAtMost(maxEnergy)
        }

        // Procedural Tripod Gait simulation for 8 legs
        for (i in 0..3) {
            val tripodOffset = if (i % 2 == 0) 0f else Math.PI.toFloat()
            legAnglesLeft[i] = sin(walkAnimPhase + tripodOffset + i * 0.4f) * 0.45f
            legAnglesRight[i] = sin(walkAnimPhase + tripodOffset + Math.PI.toFloat() + i * 0.4f) * 0.45f
        }

        // Dynamic Terrain Grounding
        val terrainY = GameMath.getTerrainHeight(position.x, position.z)
        groundY = terrainY + 0.35f

        // Jump physics
        if (isJumping) {
            position.y += jumpVelocityY * deltaSeconds
            jumpVelocityY -= 19.8f * deltaSeconds // gravity

            if (position.y <= groundY) {
                position.y = groundY
                isJumping = false
                jumpVelocityY = 0f
            }
        } else {
            // Smoothly conform Y position to ground surface
            position.y = groundY
        }

        // Starvation damage
        if (food <= 0f) {
            takeDamage(2.5f * deltaSeconds)
        }

        // Boundary Clamp
        position.x = position.x.coerceIn(-50f, 50f)
        position.z = position.z.coerceIn(-50f, 50f)
    }

    fun jump(): Boolean {
        if (!isJumping && energy >= 15f) {
            isJumping = true
            jumpVelocityY = 7.5f
            energy = (energy - 15f).coerceAtLeast(0f)
            return true
        }
        return false
    }

    /**
     * Mouth Attachment Sockets physically positioned on the spider's chelicerae fangs
     */
    val mouthSpitSocket: Vec3
        get() {
            val rad = GameMath.degToRad(yawDeg)
            return Vec3(
                position.x + sin(rad) * 1.15f,
                position.y + 0.32f,
                position.z + cos(rad) * 1.15f
            )
        }

    val mouthWebSocket: Vec3
        get() {
            val rad = GameMath.degToRad(yawDeg)
            return Vec3(
                position.x + sin(rad) * 1.22f,
                position.y + 0.32f,
                position.z + cos(rad) * 1.22f
            )
        }

    val eatInteractionSocket: Vec3
        get() {
            val rad = GameMath.degToRad(yawDeg)
            return Vec3(
                position.x + sin(rad) * 0.95f,
                position.y + 0.25f,
                position.z + cos(rad) * 0.95f
            )
        }

    fun canSpit(): Boolean = spitCooldown <= 0f && energy >= 8f

    fun onSpit(): Projectile? {
        if (!canSpit()) return null
        spitCooldown = 0.9f
        spitAnimTimer = 0.35f
        energy = (energy - 8f).coerceAtLeast(0f)

        val spawnPos = mouthSpitSocket
        val projVelocity = Vec3(
            forwardVector.x * 24f,
            1.2f,
            forwardVector.z * 24f
        )
        return Projectile(ProjectileType.SPIT, spawnPos, projVelocity)
    }

    fun canWeb(): Boolean = webCooldown <= 0f && energy >= 18f

    fun onWeb(): Projectile? {
        if (!canWeb()) return null
        webCooldown = 3.5f
        webAnimTimer = 0.5f
        energy = (energy - 18f).coerceAtLeast(0f)

        val spawnPos = mouthWebSocket
        val projVelocity = Vec3(
            forwardVector.x * 20f,
            2.0f,
            forwardVector.z * 20f
        )
        return Projectile(ProjectileType.WEB, spawnPos, projVelocity)
    }

    /**
     * Precision EAT Mechanic:
     * Spider must be in close range (< 2.8f) and facing the target within angle (< 50 deg).
     */
    fun findTargetToEat(preyList: List<PreyEntity>): PreyEntity? {
        for (prey in preyList) {
            if (!prey.isAlive) continue

            val dist = position.distanceTo(prey.position)
            if (dist > 2.8f) continue

            // Angle check: Prey must be in front of spider mouth
            val dx = prey.position.x - position.x
            val dz = prey.position.z - position.z
            val angleToPrey = GameMath.radToDeg(atan2(dx, dz))
            val angleDiff = abs(GameMath.angleDifference(angleToPrey, yawDeg))

            if (angleDiff <= 55f) {
                return prey
            }
        }
        return null
    }

    fun performEat(prey: PreyEntity): Boolean {
        biteAnimTimer = 0.65f
        prey.isAlive = false
        food = (food + prey.type.foodGain).coerceAtMost(maxFood)
        health = (health + 10f).coerceAtMost(maxHealth)
        return true
    }

    fun takeDamage(amount: Float) {
        health = (health - amount).coerceAtLeast(0f)
        if (health <= 0f) {
            isAlive = false
        }
    }

    fun heal(amount: Float) {
        health = (health + amount).coerceAtMost(maxHealth)
    }
}
