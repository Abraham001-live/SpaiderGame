package com.example.game.engine

import android.opengl.Matrix
import kotlin.math.*

/**
 * 3D Vector with comprehensive mathematical operations.
 */
data class Vec3(var x: Float = 0f, var y: Float = 0f, var z: Float = 0f) {
    fun set(nx: Float, ny: Float, nz: Float): Vec3 {
        x = nx
        y = ny
        z = nz
        return this
    }

    fun set(other: Vec3): Vec3 = set(other.x, other.y, other.z)

    fun add(other: Vec3): Vec3 = Vec3(x + other.x, y + other.y, z + other.z)
    fun sub(other: Vec3): Vec3 = Vec3(x - other.x, y - other.y, z - other.z)
    fun mul(factor: Float): Vec3 = Vec3(x * factor, y * factor, z * factor)

    fun length(): Float = sqrt(x * x + y * y + z * z)
    fun lengthSquared(): Float = x * x + y * y + z * z

    fun normalized(): Vec3 {
        val len = length()
        return if (len > 0.0001f) Vec3(x / len, y / len, z / len) else Vec3(0f, 0f, 0f)
    }

    fun distanceTo(other: Vec3): Float = sub(other).length()
    fun distanceSquaredTo(other: Vec3): Float = sub(other).lengthSquared()

    fun dot(other: Vec3): Float = x * other.x + y * other.y + z * other.z

    fun cross(other: Vec3): Vec3 = Vec3(
        y * other.z - z * other.y,
        z * other.x - x * other.z,
        x * other.y - y * other.x
    )

    fun lerp(target: Vec3, t: Float): Vec3 {
        val clampedT = t.coerceIn(0f, 1f)
        return Vec3(
            x + (target.x - x) * clampedT,
            y + (target.y - y) * clampedT,
            z + (target.z - z) * clampedT
        )
    }

    companion object {
        val ZERO get() = Vec3(0f, 0f, 0f)
        val UP get() = Vec3(0f, 1f, 0f)
        val FORWARD get() = Vec3(0f, 0f, 1f)
        val RIGHT get() = Vec3(1f, 0f, 0f)
    }
}

/**
 * 4x4 Transformation Matrix helper wrapping android.opengl.Matrix.
 */
class TransformMatrix {
    val matrix = FloatArray(16)

    init {
        identity()
    }

    fun identity(): TransformMatrix {
        Matrix.setIdentityM(matrix, 0)
        return this
    }

    fun translate(x: Float, y: Float, z: Float): TransformMatrix {
        Matrix.translateM(matrix, 0, x, y, z)
        return this
    }

    fun rotate(angleDeg: Float, x: Float, y: Float, z: Float): TransformMatrix {
        Matrix.rotateM(matrix, 0, angleDeg, x, y, z)
        return this
    }

    fun scale(sx: Float, sy: Float, sz: Float): TransformMatrix {
        Matrix.scaleM(matrix, 0, sx, sy, sz)
        return this
    }

    fun multiply(other: TransformMatrix): TransformMatrix {
        val temp = FloatArray(16)
        Matrix.multiplyMM(temp, 0, matrix, 0, other.matrix, 0)
        System.arraycopy(temp, 0, matrix, 0, 16)
        return this
    }

    fun copy(): TransformMatrix {
        val copy = TransformMatrix()
        System.arraycopy(matrix, 0, copy.matrix, 0, 16)
        return copy
    }
}

/**
 * Mathematical utilities for 3D calculations, angles, and physics.
 */
object GameMath {
    fun degToRad(deg: Float): Float = (deg * Math.PI / 180.0).toFloat()
    fun radToDeg(rad: Float): Float = (rad * 180.0 / Math.PI).toFloat()

    fun normalizeAngleDeg(angle: Float): Float {
        var a = angle % 360f
        if (a > 180f) a -= 360f
        if (a < -180f) a += 360f
        return a
    }

    fun angleDifference(targetDeg: Float, currentDeg: Float): Float {
        var diff = (targetDeg - currentDeg) % 360f
        if (diff > 180f) diff -= 360f
        if (diff < -180f) diff += 360f
        return diff
    }

    fun lerpAngleDeg(current: Float, target: Float, t: Float): Float {
        val diff = angleDifference(target, current)
        return normalizeAngleDeg(current + diff * t.coerceIn(0f, 1f))
    }

    /**
     * Exact 3D terrain height calculation for physical grounding on uneven rainforest terrain,
     * riverbeds, and crags.
     */
    fun getTerrainHeight(x: Float, z: Float): Float {
        val riverOffset = sin(z * 0.12f) * 6f + cos(z * 0.05f) * 3f
        val distToRiver = abs(x - riverOffset)

        var y = sin(x * 0.08f) * cos(z * 0.08f) * 1.4f +
                sin(x * 0.22f + z * 0.18f) * 0.5f +
                cos(x * 0.4f - z * 0.3f) * 0.18f

        if (distToRiver < 7.0f) {
            val riverFactor = (1f - (distToRiver / 7.0f)).pow(1.8f)
            y -= riverFactor * 2.2f
        }

        val distFromCenter = sqrt(x * x + z * z)
        if (distFromCenter > 45f) {
            val cliffFactor = ((distFromCenter - 45f) / 25f).coerceIn(0f, 1f)
            y += cliffFactor * 6.5f
        }

        return y
    }
}
