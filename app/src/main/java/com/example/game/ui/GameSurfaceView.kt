package com.example.game.ui

import android.annotation.SuppressLint
import android.content.Context
import android.opengl.GLSurfaceView
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import com.example.game.engine.GameMath
import com.example.game.engine.SpiderRenderer
import kotlin.math.*

/**
 * Custom GLSurfaceView for SPIDER HUNT supporting 3D camera rotation, pinch-zoom, and snap gesture.
 */
@SuppressLint("ClickableViewAccessibility")
class GameSurfaceView(
    context: Context,
    val renderer: SpiderRenderer
) : GLSurfaceView(context) {

    private var previousX = 0f
    private var previousY = 0f
    private var activePointerId = MotionEvent.INVALID_POINTER_ID

    private val scaleDetector: ScaleGestureDetector
    private val gestureDetector: GestureDetector

    init {
        setEGLContextClientVersion(2)
        setRenderer(renderer)
        renderMode = RENDERMODE_CONTINUOUSLY

        scaleDetector = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                val scaleFactor = detector.scaleFactor
                // Invert for natural pinch-to-zoom (pinch-in zooms in / reduces distance)
                renderer.cameraDistance = (renderer.cameraDistance / scaleFactor)
                    .coerceIn(renderer.minCameraDistance, renderer.maxCameraDistance)
                return true
            }
        })

        gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
            override fun onDoubleTap(e: MotionEvent): Boolean {
                // Snap camera directly behind spider's facing angle
                renderer.cameraYawOffsetDeg = 0f
                renderer.cameraPitchDeg = 22f
                renderer.lastCameraTouchTimeMs = System.currentTimeMillis()
                return true
            }
        })
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        scaleDetector.onTouchEvent(event)
        gestureDetector.onTouchEvent(event)

        if (scaleDetector.isInProgress) return true

        val touchX = event.x
        val touchY = event.y
        val w = width.toFloat().coerceAtLeast(1f)
        val h = height.toFloat().coerceAtLeast(1f)

        // Ignore camera swipes if touch starts in virtual joystick area (bottom-left) or action buttons (bottom-right)
        val isJoystickArea = touchX < w * 0.38f && touchY > h * 0.50f
        val isButtonsArea = touchX > w * 0.62f && touchY > h * 0.50f

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                if (!isJoystickArea && !isButtonsArea) {
                    activePointerId = event.getPointerId(0)
                    previousX = touchX
                    previousY = touchY
                    renderer.isManualCameraActive = true
                    renderer.lastCameraTouchTimeMs = System.currentTimeMillis()
                }
            }

            MotionEvent.ACTION_MOVE -> {
                if (renderer.isManualCameraActive) {
                    val pointerIndex = event.findPointerIndex(activePointerId)
                    if (pointerIndex != -1) {
                        val x = event.getX(pointerIndex)
                        val y = event.getY(pointerIndex)

                        val dx = x - previousX
                        val dy = y - previousY

                        // Horizontal swipe orbits camera 360° around the spider independently
                        renderer.cameraYawOffsetDeg = GameMath.normalizeAngleDeg(renderer.cameraYawOffsetDeg - dx * 0.42f)
                        renderer.cameraPitchDeg = (renderer.cameraPitchDeg + dy * 0.32f).coerceIn(8f, 75f)
                        renderer.lastCameraTouchTimeMs = System.currentTimeMillis()

                        previousX = x
                        previousY = y
                    }
                }
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                activePointerId = MotionEvent.INVALID_POINTER_ID
                renderer.isManualCameraActive = false
                renderer.lastCameraTouchTimeMs = System.currentTimeMillis()
            }

            MotionEvent.ACTION_POINTER_UP -> {
                val pointerIndex = event.actionIndex
                val pointerId = event.getPointerId(pointerIndex)
                if (pointerId == activePointerId) {
                    val newPointerIndex = if (pointerIndex == 0) 1 else 0
                    previousX = event.getX(newPointerIndex)
                    previousY = event.getY(newPointerIndex)
                    activePointerId = event.getPointerId(newPointerIndex)
                }
            }
        }
        return true
    }
}
