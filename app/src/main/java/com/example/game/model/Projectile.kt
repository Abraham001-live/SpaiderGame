package com.example.game.model

import com.example.game.engine.Vec3

enum class ProjectileType {
    SPIT,
    WEB
}

/**
 * 3D Projectile shot by the spider (Venom Spit or Web Mesh).
 */
class Projectile(
    val type: ProjectileType,
    val position: Vec3,
    val velocity: Vec3,
    val radius: Float = if (type == ProjectileType.WEB) 1.8f else 0.45f,
    var lifeTimeSeconds: Float = if (type == ProjectileType.WEB) 3.5f else 2.5f
) {
    var isExpired: Boolean = false

    fun update(deltaSeconds: Float) {
        if (isExpired) return

        position.x += velocity.x * deltaSeconds
        position.y += velocity.y * deltaSeconds
        position.z += velocity.z * deltaSeconds

        // Slight gravity drop
        if (type == ProjectileType.WEB) {
            velocity.y -= 2.5f * deltaSeconds
        } else {
            velocity.y -= 1.0f * deltaSeconds
        }

        lifeTimeSeconds -= deltaSeconds
        if (lifeTimeSeconds <= 0f || position.y < -0.2f) {
            isExpired = true
        }
    }
}
