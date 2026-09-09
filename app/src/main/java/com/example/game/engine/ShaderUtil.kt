package com.example.game.engine

import android.opengl.GLES20
import android.util.Log

/**
 * OpenGL Shader loader and program management.
 */
object ShaderUtil {
    private const val TAG = "ShaderUtil"

    private const val VERTEX_SHADER_CODE = """
        uniform mat4 uMVPMatrix;
        uniform mat4 uModelMatrix;
        uniform vec3 uLightDir;
        uniform vec4 uOverrideColor;
        uniform float uUseOverrideColor;

        attribute vec4 aPosition;
        attribute vec3 aNormal;
        attribute vec4 aColor;

        varying vec4 vColor;
        varying vec3 vNormal;
        varying vec3 vFragPos;
        varying float vDepth;

        void main() {
            vec4 worldPos = uModelMatrix * aPosition;
            vFragPos = vec3(worldPos);
            vNormal = mat3(uModelMatrix) * aNormal;
            
            vec4 baseCol = (uUseOverrideColor > 0.5) ? uOverrideColor : aColor;
            vColor = baseCol;
            
            vec4 clipPos = uMVPMatrix * aPosition;
            vDepth = clipPos.z / clipPos.w;
            gl_Position = clipPos;
        }
    """

    private const val FRAGMENT_SHADER_CODE = """
        precision mediump float;

        uniform vec3 uLightDir;
        uniform vec3 uLightColor;
        uniform vec3 uAmbientColor;
        uniform vec3 uCameraPos;
        uniform vec4 uFogColor;
        uniform float uFogDensity;
        uniform float uShininess;

        varying vec4 vColor;
        varying vec3 vNormal;
        varying vec3 vFragPos;
        varying float vDepth;

        void main() {
            vec3 norm = normalize(vNormal);
            vec3 lightDir = normalize(-uLightDir);
            
            // Diffuse
            float diff = max(dot(norm, lightDir), 0.0);
            vec3 diffuse = diff * uLightColor;
            
            // Ambient
            vec3 ambient = uAmbientColor;
            
            // Specular
            vec3 viewDir = normalize(uCameraPos - vFragPos);
            vec3 reflectDir = reflect(-lightDir, norm);
            float spec = pow(max(dot(viewDir, reflectDir), 0.0), uShininess);
            vec3 specular = 0.25 * spec * uLightColor;
            
            vec3 lighting = (ambient + diffuse + specular);
            vec4 resultColor = vec4(vColor.rgb * lighting, vColor.a);
            
            // Distance fog
            float dist = length(uCameraPos - vFragPos);
            float fogFactor = clamp((dist - 15.0) / (75.0 / (uFogDensity + 0.001)), 0.0, 1.0);
            vec4 finalColor = mix(resultColor, uFogColor, fogFactor * uFogColor.a);
            
            gl_FragColor = finalColor;
        }
    """

    fun createProgram(): Int {
        val vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, VERTEX_SHADER_CODE)
        val fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, FRAGMENT_SHADER_CODE)

        val program = GLES20.glCreateProgram()
        GLES20.glAttachShader(program, vertexShader)
        GLES20.glAttachShader(program, fragmentShader)
        GLES20.glLinkProgram(program)

        val linkStatus = IntArray(1)
        GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linkStatus, 0)
        if (linkStatus[0] != GLES20.GL_TRUE) {
            val error = GLES20.glGetProgramInfoLog(program)
            GLES20.glDeleteProgram(program)
            Log.e(TAG, "Could not link program: $error")
            return 0
        }

        return program
    }

    fun loadTextureFromResource(context: android.content.Context, resourceId: Int): Int {
        val textureObjectIds = IntArray(1)
        GLES20.glGenTextures(1, textureObjectIds, 0)
        if (textureObjectIds[0] == 0) {
            Log.w(TAG, "Could not generate a new OpenGL texture object.")
            return 0
        }

        val options = android.graphics.BitmapFactory.Options().apply {
            inScaled = false
        }
        val bitmap = android.graphics.BitmapFactory.decodeResource(context.resources, resourceId, options)
        if (bitmap == null) {
            Log.w(TAG, "Resource ID $resourceId could not be decoded.")
            GLES20.glDeleteTextures(1, textureObjectIds, 0)
            return 0
        }

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureObjectIds[0])
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR_MIPMAP_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
        
        android.opengl.GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0)
        GLES20.glGenerateMipmap(GLES20.GL_TEXTURE_2D)

        bitmap.recycle()
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)

        return textureObjectIds[0]
    }

    private fun loadShader(type: Int, shaderCode: String): Int {
        val shader = GLES20.glCreateShader(type)
        GLES20.glShaderSource(shader, shaderCode)
        GLES20.glCompileShader(shader)

        val compiled = IntArray(1)
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0)
        if (compiled[0] == 0) {
            val error = GLES20.glGetShaderInfoLog(shader)
            GLES20.glDeleteShader(shader)
            Log.e(TAG, "Could not compile shader $type: $error")
            return 0
        }

        return shader
    }
}
