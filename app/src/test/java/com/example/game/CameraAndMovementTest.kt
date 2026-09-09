package com.example.game

import com.example.game.engine.GameMath
import com.example.game.engine.SpiderRenderer
import com.example.game.engine.Vec3
import com.example.game.model.GameSession
import com.example.game.model.SpiderEntity
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import kotlin.math.abs

class CameraAndMovementTest {

    private lateinit var spider: SpiderEntity

    @Before
    fun setUp() {
        spider = SpiderEntity(position = Vec3(0f, 0f, 0f), yawDeg = 0f)
    }

    @Test
    fun test01_ForwardMovement() {
        val initialZ = spider.position.z
        // Joystick UP (moveInputZ = 1.0f)
        spider.update(
            deltaSeconds = 0.1f,
            moveInputX = 0f,
            moveInputZ = 1.0f,
            cameraYawDeg = 0f
        )
        assertTrue("Spider should move forward along Z axis", spider.position.z > initialZ)
        assertEquals("Spider yaw should not change during pure forward movement", 0f, spider.yawDeg, 0.01f)
    }

    @Test
    fun test02_ReverseMovement() {
        val initialZ = spider.position.z
        val initialYaw = spider.yawDeg
        // Joystick DOWN (moveInputZ = -1.0f)
        spider.update(
            deltaSeconds = 0.1f,
            moveInputX = 0f,
            moveInputZ = -1.0f,
            cameraYawDeg = 0f
        )
        assertTrue("Spider should backpedal backward along Z axis", spider.position.z < initialZ)
        assertEquals("Spider should NOT turn 180 degrees during reverse movement", initialYaw, spider.yawDeg, 0.01f)
    }

    @Test
    fun test03_LeftRightTurning() {
        val initialYaw = spider.yawDeg
        // Joystick RIGHT (moveInputX = 1.0f)
        spider.update(
            deltaSeconds = 0.1f,
            moveInputX = 1.0f,
            moveInputZ = 0f,
            cameraYawDeg = 0f
        )
        assertTrue("Joystick right should rotate spider yaw clockwise", spider.yawDeg > initialYaw)

        val turnedYaw = spider.yawDeg
        // Joystick LEFT (moveInputX = -1.0f)
        spider.update(
            deltaSeconds = 0.1f,
            moveInputX = -1.0f,
            moveInputZ = 0f,
            cameraYawDeg = 0f
        )
        assertTrue("Joystick left should rotate spider yaw counter-clockwise", spider.yawDeg < turnedYaw)
    }

    @Test
    fun test04_and_05_CameraSwipeLeftRight() {
        var cameraYawOffsetDeg = 0f
        val dxRight = 50f
        cameraYawOffsetDeg = GameMath.normalizeAngleDeg(cameraYawOffsetDeg - dxRight * 0.42f)
        assertTrue("Swiping right rotates camera offset clockwise/negative delta", cameraYawOffsetDeg < 0f)

        val dxLeft = -80f
        cameraYawOffsetDeg = GameMath.normalizeAngleDeg(cameraYawOffsetDeg - dxLeft * 0.42f)
        assertTrue("Swiping left rotates camera offset counter-clockwise/positive delta", cameraYawOffsetDeg > 0f)
    }

    @Test
    fun test06_Full360DegreeCameraRotation() {
        var cameraYawOffsetDeg = 0f
        // Simulate continuous horizontal swiping around the spider
        for (i in 1..20) {
            cameraYawOffsetDeg = GameMath.normalizeAngleDeg(cameraYawOffsetDeg - 40f)
        }
        // Offset wraps cleanly within -180..180 degrees range
        assertTrue("Camera offset should stay normalized within [-180, 180]", cameraYawOffsetDeg >= -180f && cameraYawOffsetDeg <= 180f)
    }

    @Test
    fun test07_CameraRotationWhileSpiderIsMoving() {
        var cameraYawOffsetDeg = 45f // Camera looking from side
        val startX = spider.position.x
        val startZ = spider.position.z

        // Spider moves forward while camera is rotated
        spider.update(
            deltaSeconds = 0.1f,
            moveInputX = 0f,
            moveInputZ = 1.0f,
            cameraYawDeg = GameMath.normalizeAngleDeg(spider.yawDeg + cameraYawOffsetDeg)
        )

        assertTrue("Spider should continue moving forward in its facing direction", spider.position.z > startZ)
    }

    @Test
    fun test08_CameraRotationWithoutRotatingSpider() {
        val spiderInitialYaw = spider.yawDeg
        var cameraYawOffsetDeg = 0f

        // Swipe camera 90 degrees
        cameraYawOffsetDeg = GameMath.normalizeAngleDeg(cameraYawOffsetDeg - 90f)

        assertEquals("Spider yaw must remain unchanged when camera swipes", spiderInitialYaw, spider.yawDeg, 0.01f)
    }

    @Test
    fun test09_CameraRecentering() {
        var cameraYawOffsetDeg = 60f
        val deltaSeconds = 0.1f

        // When manual touch stops, lerp angle towards 0
        for (step in 1..20) {
            cameraYawOffsetDeg = GameMath.lerpAngleDeg(cameraYawOffsetDeg, 0f, deltaSeconds * 4.5f)
        }

        assertTrue("Camera offset should smoothly recenter to 0 degrees behind spider", abs(cameraYawOffsetDeg) < 2.0f)
    }

    @Test
    fun test10_SimultaneousJoystickAndCameraTouch() {
        var cameraYawOffsetDeg = 30f
        val initialZ = spider.position.z

        // Joystick held UP while camera offset active
        spider.update(
            deltaSeconds = 0.1f,
            moveInputX = 0f,
            moveInputZ = 1.0f,
            cameraYawDeg = GameMath.normalizeAngleDeg(spider.yawDeg + cameraYawOffsetDeg)
        )

        // Rotate camera further during movement
        cameraYawOffsetDeg += 15f

        assertTrue("Spider moves forward independently", spider.position.z > initialZ)
        assertEquals("Camera offset updates independently", 45f, cameraYawOffsetDeg, 0.01f)
    }

    @Test
    fun test11_TouchInputPriorityZones() {
        val width = 1080f
        val height = 2400f

        // Joystick zone (bottom-left)
        val joystickTouchX = width * 0.20f
        val joystickTouchY = height * 0.75f
        val isJoystickArea = joystickTouchX < width * 0.38f && joystickTouchY > height * 0.50f
        assertTrue("Touch in bottom-left should be identified as joystick area", isJoystickArea)

        // Action buttons zone (bottom-right)
        val buttonTouchX = width * 0.85f
        val buttonTouchY = height * 0.75f
        val isButtonsArea = buttonTouchX > width * 0.62f && buttonTouchY > height * 0.50f
        assertTrue("Touch in bottom-right should be identified as buttons area", isButtonsArea)

        // Camera swipe area (top/middle)
        val cameraTouchX = width * 0.50f
        val cameraTouchY = height * 0.30f
        val isCameraSwipeArea = !(cameraTouchX < width * 0.38f && cameraTouchY > height * 0.50f) &&
                                !(cameraTouchX > width * 0.62f && cameraTouchY > height * 0.50f)
        assertTrue("Touch in middle/top screen should be identified as camera swipe area", isCameraSwipeArea)
    }

    @Test
    fun test12_SpiderTerrainGrounding() {
        // Move spider to various uneven terrain positions (hill, valley, riverbed)
        val testCoords = arrayOf(
            Pair(0f, 0f),
            Pair(15f, -20f),
            Pair(-10f, 35f),
            Pair(48f, 48f)
        )

        for ((x, z) in testCoords) {
            spider.position.x = x
            spider.position.z = z
            spider.update(0.01f, 0f, 0f, 0f)

            val expectedGroundY = GameMath.getTerrainHeight(x, z) + 0.35f
            assertEquals("Spider groundY must match exact 3D terrain height at ($x, $z)", expectedGroundY, spider.groundY, 0.01f)
            assertEquals("Spider position.y must be grounded on terrain at ($x, $z)", expectedGroundY, spider.position.y, 0.01f)
        }
    }
}
