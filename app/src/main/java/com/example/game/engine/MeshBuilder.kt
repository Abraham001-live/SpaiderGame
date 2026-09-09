package com.example.game.engine

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer
import kotlin.math.*

/**
 * Represents a GPU-ready 3D mesh with positions, normals, and vertex colors.
 */
class Mesh(
    val vertexBuffer: FloatBuffer,
    val indexBuffer: ShortBuffer,
    val indexCount: Int,
    val baseColor: FloatArray = floatArrayOf(1f, 1f, 1f, 1f)
)

/**
 * High-fidelity 3D procedural mesh generator for Amazon rainforest foliage,
 * giant trees, buttress roots, broadleaf ferns, mushrooms, rock arches, and realistic terrain.
 */
object MeshBuilder {
    private const val FLOAT_SIZE = 4
    private const val SHORT_SIZE = 2

    const val VERTEX_STRIDE = 10 * FLOAT_SIZE
    const val POSITION_OFFSET = 0
    const val NORMAL_OFFSET = 3 * FLOAT_SIZE
    const val COLOR_OFFSET = 6 * FLOAT_SIZE

    /**
     * Builds a UV sphere mesh for creature anatomy, eyes, and particle droplets.
     */
    fun createSphere(radius: Float, rings: Int = 12, sectors: Int = 12, r: Float, g: Float, b: Float, a: Float = 1f): Mesh {
        val vertexList = ArrayList<Float>()
        val indexList = ArrayList<Short>()

        val R = 1f / (rings - 1).toFloat()
        val S = 1f / (sectors - 1).toFloat()

        for (rIdx in 0 until rings) {
            val v = rIdx * R
            val phi = v * Math.PI.toFloat()
            for (sIdx in 0 until sectors) {
                val u = sIdx * S
                val theta = u * 2f * Math.PI.toFloat()

                val nx = sin(phi) * cos(theta)
                val ny = cos(phi)
                val nz = sin(phi) * sin(theta)

                val x = nx * radius
                val y = ny * radius
                val z = nz * radius

                vertexList.add(x)
                vertexList.add(y)
                vertexList.add(z)
                vertexList.add(nx)
                vertexList.add(ny)
                vertexList.add(nz)
                vertexList.add(r)
                vertexList.add(g)
                vertexList.add(b)
                vertexList.add(a)
            }
        }

        for (rIdx in 0 until rings - 1) {
            for (sIdx in 0 until sectors - 1) {
                val i0 = (rIdx * sectors + sIdx).toShort()
                val i1 = (rIdx * sectors + (sIdx + 1)).toShort()
                val i2 = ((rIdx + 1) * sectors + (sIdx + 1)).toShort()
                val i3 = ((rIdx + 1) * sectors + sIdx).toShort()

                indexList.add(i0)
                indexList.add(i1)
                indexList.add(i2)

                indexList.add(i0)
                indexList.add(i2)
                indexList.add(i3)
            }
        }

        return createMeshFromData(vertexList, indexList, floatArrayOf(r, g, b, a))
    }

    /**
     * Builds an ellipsoid (stretched sphere) for spider cephalothorax, abdomen, frog body.
     */
    fun createEllipsoid(rx: Float, ry: Float, rz: Float, rings: Int = 12, sectors: Int = 12, r: Float, g: Float, b: Float, a: Float = 1f): Mesh {
        val vertexList = ArrayList<Float>()
        val indexList = ArrayList<Short>()

        val R = 1f / (rings - 1).toFloat()
        val S = 1f / (sectors - 1).toFloat()

        for (rIdx in 0 until rings) {
            val v = rIdx * R
            val phi = v * Math.PI.toFloat()
            for (sIdx in 0 until sectors) {
                val u = sIdx * S
                val theta = u * 2f * Math.PI.toFloat()

                val nx = sin(phi) * cos(theta)
                val ny = cos(phi)
                val nz = sin(phi) * sin(theta)

                val x = nx * rx
                val y = ny * ry
                val z = nz * rz

                val normLen = sqrt((nx / (rx * rx)).pow(2) + (ny / (ry * ry)).pow(2) + (nz / (rz * rz)).pow(2)).coerceAtLeast(0.0001f)
                val nnx = (nx / (rx * rx)) / normLen
                val nny = (ny / (ry * ry)) / normLen
                val nnz = (nz / (rz * rz)) / normLen

                vertexList.add(x)
                vertexList.add(y)
                vertexList.add(z)
                vertexList.add(nnx)
                vertexList.add(nny)
                vertexList.add(nnz)
                vertexList.add(r)
                vertexList.add(g)
                vertexList.add(b)
                vertexList.add(a)
            }
        }

        for (rIdx in 0 until rings - 1) {
            for (sIdx in 0 until sectors - 1) {
                val i0 = (rIdx * sectors + sIdx).toShort()
                val i1 = (rIdx * sectors + (sIdx + 1)).toShort()
                val i2 = ((rIdx + 1) * sectors + (sIdx + 1)).toShort()
                val i3 = ((rIdx + 1) * sectors + sIdx).toShort()

                indexList.add(i0)
                indexList.add(i1)
                indexList.add(i2)

                indexList.add(i0)
                indexList.add(i2)
                indexList.add(i3)
            }
        }

        return createMeshFromData(vertexList, indexList, floatArrayOf(r, g, b, a))
    }

    /**
     * Builds a cylinder or tapered segment for spider legs, vines, and tree trunks.
     */
    fun createCylinder(radiusBottom: Float, radiusTop: Float, height: Float, segments: Int = 10, r: Float, g: Float, b: Float, a: Float = 1f): Mesh {
        val vertexList = ArrayList<Float>()
        val indexList = ArrayList<Short>()

        val halfH = height * 0.5f

        for (i in 0..segments) {
            val angle = i * 2f * Math.PI.toFloat() / segments
            val cosA = cos(angle)
            val sinA = sin(angle)

            // Bottom vertex
            vertexList.add(cosA * radiusBottom)
            vertexList.add(-halfH)
            vertexList.add(sinA * radiusBottom)
            vertexList.add(cosA)
            vertexList.add(0f)
            vertexList.add(sinA)
            vertexList.add(r)
            vertexList.add(g)
            vertexList.add(b)
            vertexList.add(a)

            // Top vertex
            vertexList.add(cosA * radiusTop)
            vertexList.add(halfH)
            vertexList.add(sinA * radiusTop)
            vertexList.add(cosA)
            vertexList.add(0f)
            vertexList.add(sinA)
            vertexList.add(r)
            vertexList.add(g)
            vertexList.add(b)
            vertexList.add(a)
        }

        for (i in 0 until segments) {
            val b0 = (i * 2).toShort()
            val t0 = (i * 2 + 1).toShort()
            val b1 = ((i + 1) * 2).toShort()
            val t1 = ((i + 1) * 2 + 1).toShort()

            indexList.add(b0)
            indexList.add(t0)
            indexList.add(b1)

            indexList.add(b1)
            indexList.add(t0)
            indexList.add(t1)
        }

        return createMeshFromData(vertexList, indexList, floatArrayOf(r, g, b, a))
    }

    /**
     * Builds a 3D box for boulders, obstacles, fallen logs, and city architecture.
     */
    fun createBox(sx: Float, sy: Float, sz: Float, r: Float, g: Float, b: Float, a: Float = 1f): Mesh {
        val hx = sx * 0.5f
        val hy = sy * 0.5f
        val hz = sz * 0.5f

        val vertexList = ArrayList<Float>()
        val indexList = ArrayList<Short>()

        fun addFace(p0: FloatArray, p1: FloatArray, p2: FloatArray, p3: FloatArray, norm: FloatArray) {
            val base = (vertexList.size / 10).toShort()
            val pts = arrayOf(p0, p1, p2, p3)
            for (p in pts) {
                vertexList.add(p[0])
                vertexList.add(p[1])
                vertexList.add(p[2])
                vertexList.add(norm[0])
                vertexList.add(norm[1])
                vertexList.add(norm[2])
                vertexList.add(r)
                vertexList.add(g)
                vertexList.add(b)
                vertexList.add(a)
            }
            indexList.add(base)
            indexList.add((base + 1).toShort())
            indexList.add((base + 2).toShort())

            indexList.add(base)
            indexList.add((base + 2).toShort())
            indexList.add((base + 3).toShort())
        }

        addFace(floatArrayOf(-hx, -hy, hz), floatArrayOf(hx, -hy, hz), floatArrayOf(hx, hy, hz), floatArrayOf(-hx, hy, hz), floatArrayOf(0f, 0f, 1f))
        addFace(floatArrayOf(hx, -hy, -hz), floatArrayOf(-hx, -hy, -hz), floatArrayOf(-hx, hy, -hz), floatArrayOf(hx, hy, -hz), floatArrayOf(0f, 0f, -1f))
        addFace(floatArrayOf(hx, -hy, hz), floatArrayOf(hx, -hy, -hz), floatArrayOf(hx, hy, -hz), floatArrayOf(hx, hy, hz), floatArrayOf(1f, 0f, 0f))
        addFace(floatArrayOf(-hx, -hy, -hz), floatArrayOf(-hx, -hy, hz), floatArrayOf(-hx, hy, hz), floatArrayOf(-hx, hy, -hz), floatArrayOf(-1f, 0f, 0f))
        addFace(floatArrayOf(-hx, hy, hz), floatArrayOf(hx, hy, hz), floatArrayOf(hx, hy, -hz), floatArrayOf(-hx, hy, -hz), floatArrayOf(0f, 1f, 0f))
        addFace(floatArrayOf(-hx, -hy, -hz), floatArrayOf(hx, -hy, -hz), floatArrayOf(hx, -hy, hz), floatArrayOf(-hx, -hy, hz), floatArrayOf(0f, -1f, 0f))

        return createMeshFromData(vertexList, indexList, floatArrayOf(r, g, b, a))
    }

    /**
     * Builds detailed Broadleaf Jungle Fern frond cluster for foreground and midground cover.
     */
    fun createBroadleafFern(leafCount: Int = 8, length: Float = 3.2f, width: Float = 0.9f): Mesh {
        val vertexList = ArrayList<Float>()
        val indexList = ArrayList<Short>()

        for (leaf in 0 until leafCount) {
            val leafAngle = (leaf * 2f * Math.PI.toFloat() / leafCount) + (leaf * 0.12f)
            val cosL = cos(leafAngle)
            val sinL = sin(leafAngle)
            val perpX = -sinL
            val perpZ = cosL

            val segments = 4
            for (seg in 0..segments) {
                val t = seg.toFloat() / segments
                val segLen = t * length
                val segWidth = sin(t * Math.PI.toFloat()) * width * 0.5f

                // Parabolic arch curving downward toward tips
                val segHeight = sin(t * Math.PI.toFloat() * 0.5f) * 1.2f - (t * t * 0.8f)

                val centerX = cosL * segLen
                val centerZ = sinL * segLen
                val centerY = segHeight.coerceAtLeast(0f)

                // Left leaf edge
                vertexList.add(centerX + perpX * segWidth)
                vertexList.add(centerY)
                vertexList.add(centerZ + perpZ * segWidth)
                vertexList.add(0f); vertexList.add(1f); vertexList.add(0f)
                vertexList.add(0.12f); vertexList.add(0.48f + t * 0.15f); vertexList.add(0.14f); vertexList.add(1f)

                // Right leaf edge
                vertexList.add(centerX - perpX * segWidth)
                vertexList.add(centerY)
                vertexList.add(centerZ - perpZ * segWidth)
                vertexList.add(0f); vertexList.add(1f); vertexList.add(0f)
                vertexList.add(0.09f); vertexList.add(0.42f + t * 0.12f); vertexList.add(0.11f); vertexList.add(1f)
            }

            val leafBase = (leaf * (segments + 1) * 2).toShort()
            for (seg in 0 until segments) {
                val v0 = (leafBase + seg * 2).toShort()
                val v1 = (leafBase + seg * 2 + 1).toShort()
                val v2 = (leafBase + (seg + 1) * 2).toShort()
                val v3 = (leafBase + (seg + 1) * 2 + 1).toShort()

                indexList.add(v0); indexList.add(v1); indexList.add(v2)
                indexList.add(v2); indexList.add(v1); indexList.add(v3)
            }
        }

        return createMeshFromData(vertexList, indexList, floatArrayOf(0.15f, 0.52f, 0.18f, 1f))
    }

    /**
     * Builds an exotic Jungle Mushroom with a wide rounded cap and bioluminescent rim.
     */
    fun createMushroom(capRadius: Float = 1.2f, height: Float = 1.6f): Mesh {
        val vertexList = ArrayList<Float>()
        val indexList = ArrayList<Short>()

        // Stalk (Cylinder)
        val stalkSegments = 8
        for (i in 0..stalkSegments) {
            val angle = i * 2f * Math.PI.toFloat() / stalkSegments
            val cosA = cos(angle)
            val sinA = sin(angle)

            // Bottom
            vertexList.add(cosA * 0.18f); vertexList.add(0f); vertexList.add(sinA * 0.18f)
            vertexList.add(cosA); vertexList.add(0f); vertexList.add(sinA)
            vertexList.add(0.85f); vertexList.add(0.82f); vertexList.add(0.72f); vertexList.add(1f)

            // Top
            vertexList.add(cosA * 0.14f); vertexList.add(height); vertexList.add(sinA * 0.14f)
            vertexList.add(cosA); vertexList.add(0f); vertexList.add(sinA)
            vertexList.add(0.92f); vertexList.add(0.90f); vertexList.add(0.80f); vertexList.add(1f)
        }

        for (i in 0 until stalkSegments) {
            val b0 = (i * 2).toShort()
            val t0 = (i * 2 + 1).toShort()
            val b1 = ((i + 1) * 2).toShort()
            val t1 = ((i + 1) * 2 + 1).toShort()

            indexList.add(b0); indexList.add(t0); indexList.add(b1)
            indexList.add(b1); indexList.add(t0); indexList.add(t1)
        }

        // Cap Dome
        val capBase = (vertexList.size / 10).toShort()
        val capRings = 6
        val capSectors = 10
        for (r in 0 until capRings) {
            val phi = (r.toFloat() / (capRings - 1)) * 0.5f * Math.PI.toFloat()
            for (s in 0 until capSectors) {
                val theta = (s.toFloat() / capSectors) * 2f * Math.PI.toFloat()

                val nx = sin(phi) * cos(theta)
                val ny = cos(phi)
                val nz = sin(phi) * sin(theta)

                val x = nx * capRadius
                val y = height + (ny * capRadius * 0.6f)
                val z = nz * capRadius

                // Bioluminescent orange/cyan cap with speckled top
                val isBioluminescent = r > 3
                val cr = if (isBioluminescent) 0.15f else 0.95f
                val cg = if (isBioluminescent) 0.90f else 0.45f
                val cb = if (isBioluminescent) 0.85f else 0.15f

                vertexList.add(x); vertexList.add(y); vertexList.add(z)
                vertexList.add(nx); vertexList.add(ny); vertexList.add(nz)
                vertexList.add(cr); vertexList.add(cg); vertexList.add(cb); vertexList.add(1f)
            }
        }

        for (r in 0 until capRings - 1) {
            for (s in 0 until capSectors) {
                val nextS = (s + 1) % capSectors
                val i0 = (capBase + r * capSectors + s).toShort()
                val i1 = (capBase + r * capSectors + nextS).toShort()
                val i2 = (capBase + (r + 1) * capSectors + nextS).toShort()
                val i3 = (capBase + (r + 1) * capSectors + s).toShort()

                indexList.add(i0); indexList.add(i1); indexList.add(i2)
                indexList.add(i0); indexList.add(i2); indexList.add(i3)
            }
        }

        return createMeshFromData(vertexList, indexList, floatArrayOf(0.9f, 0.5f, 0.2f, 1f))
    }

    /**
     * Builds a Volumetric Godray Sunbeam Cone filtering through forest canopy.
     */
    fun createVolumetricSunShaft(topRadius: Float = 0.8f, bottomRadius: Float = 6.0f, height: Float = 22.0f): Mesh {
        val vertexList = ArrayList<Float>()
        val indexList = ArrayList<Short>()
        val segments = 12

        for (i in 0..segments) {
            val angle = i * 2f * Math.PI.toFloat() / segments
            val cosA = cos(angle)
            val sinA = sin(angle)

            // Top (high intensity soft golden white)
            vertexList.add(cosA * topRadius)
            vertexList.add(height)
            vertexList.add(sinA * topRadius)
            vertexList.add(0f); vertexList.add(-1f); vertexList.add(0f)
            vertexList.add(1.0f); vertexList.add(0.95f); vertexList.add(0.75f); vertexList.add(0.45f)

            // Bottom (faded ground beam)
            vertexList.add(cosA * bottomRadius)
            vertexList.add(0f)
            vertexList.add(sinA * bottomRadius)
            vertexList.add(0f); vertexList.add(-1f); vertexList.add(0f)
            vertexList.add(1.0f); vertexList.add(0.92f); vertexList.add(0.70f); vertexList.add(0.04f)
        }

        for (i in 0 until segments) {
            val t0 = (i * 2).toShort()
            val b0 = (i * 2 + 1).toShort()
            val t1 = ((i + 1) * 2).toShort()
            val b1 = ((i + 1) * 2 + 1).toShort()

            indexList.add(t0); indexList.add(b0); indexList.add(b1)
            indexList.add(t0); indexList.add(b1); indexList.add(t1)
        }

        return createMeshFromData(vertexList, indexList, floatArrayOf(1f, 0.95f, 0.75f, 0.35f))
    }

    /**
     * Builds realistic multi-octave Amazon terrain grid with riverbeds, elevation hills, and mud banks.
     */
    fun createTerrain(size: Float = 140f, subdivisions: Int = 44): Mesh {
        val vertexList = ArrayList<Float>()
        val indexList = ArrayList<Short>()

        val step = size / subdivisions
        val half = size * 0.5f

        for (zIdx in 0..subdivisions) {
            val z = -half + zIdx * step
            for (xIdx in 0..subdivisions) {
                val x = -half + xIdx * step

                // Procedural Amazon ground height with meandering creek and lush ridges
                val riverOffset = sin(z * 0.12f) * 6f + cos(z * 0.05f) * 3f
                val distToRiver = abs(x - riverOffset)

                var y = sin(x * 0.08f) * cos(z * 0.08f) * 1.4f +
                        sin(x * 0.22f + z * 0.18f) * 0.5f +
                        cos(x * 0.4f - z * 0.3f) * 0.18f

                // Deep riverbed depression
                if (distToRiver < 7.0f) {
                    val riverFactor = (1f - (distToRiver / 7.0f)).pow(1.8f)
                    y -= riverFactor * 2.2f
                }

                // Rocky hill outcrops on edges
                val distFromCenter = sqrt(x * x + z * z)
                if (distFromCenter > 45f) {
                    val cliffFactor = ((distFromCenter - 45f) / 25f).coerceIn(0f, 1f)
                    y += cliffFactor * 6.5f
                }

                // Normal estimation
                val nx = -0.1f * cos(x * 0.08f) * cos(z * 0.08f) - (if (distToRiver < 7f) (x - riverOffset) * 0.15f else 0f)
                val ny = 1.0f
                val nz = 0.1f * sin(x * 0.08f) * sin(z * 0.08f)
                val normLen = sqrt(nx * nx + ny * ny + nz * nz)

                // Realistic color gradient: Wet mud along river, rich moss & loam across floor, stone on hills
                val isRiverbank = distToRiver < 6.5f
                val isHill = y > 2.5f

                val r: Float
                val g: Float
                val b: Float

                if (isRiverbank) {
                    // Dark wet Amazonian mud
                    r = 0.18f + sin(x * 0.3f) * 0.03f
                    g = 0.16f + cos(z * 0.3f) * 0.03f
                    b = 0.11f
                } else if (isHill) {
                    // Rocky crag stone
                    r = 0.38f + cos(x * 0.2f) * 0.04f
                    g = 0.40f + sin(z * 0.2f) * 0.04f
                    b = 0.36f
                } else {
                    // Lush tropical rainforest floor with fallen leaf specks
                    r = 0.20f + sin(x * 0.15f) * 0.04f
                    g = 0.42f + cos(z * 0.15f) * 0.08f
                    b = 0.16f
                }

                vertexList.add(x)
                vertexList.add(y)
                vertexList.add(z)
                vertexList.add(nx / normLen)
                vertexList.add(ny / normLen)
                vertexList.add(nz / normLen)
                vertexList.add(r)
                vertexList.add(g)
                vertexList.add(b)
                vertexList.add(1f)
            }
        }

        val rowVertices = subdivisions + 1
        for (zIdx in 0 until subdivisions) {
            for (xIdx in 0 until subdivisions) {
                val i0 = (zIdx * rowVertices + xIdx).toShort()
                val i1 = (zIdx * rowVertices + (xIdx + 1)).toShort()
                val i2 = ((zIdx + 1) * rowVertices + (xIdx + 1)).toShort()
                val i3 = ((zIdx + 1) * rowVertices + xIdx).toShort()

                indexList.add(i0); indexList.add(i1); indexList.add(i2)
                indexList.add(i0); indexList.add(i2); indexList.add(i3)
            }
        }

        return createMeshFromData(vertexList, indexList, floatArrayOf(0.2f, 0.4f, 0.18f, 1f))
    }

    /**
     * Builds water surface plane with multi-segment vertices for realistic river ripples and light glints.
     */
    fun createWaterPlane(size: Float = 140f, subdivisions: Int = 16): Mesh {
        val vertexList = ArrayList<Float>()
        val indexList = ArrayList<Short>()

        val step = size / subdivisions
        val half = size * 0.5f

        for (zIdx in 0..subdivisions) {
            val z = -half + zIdx * step
            for (xIdx in 0..subdivisions) {
                val x = -half + xIdx * step

                vertexList.add(x)
                vertexList.add(-0.55f) // Water line
                vertexList.add(z)
                vertexList.add(0f)
                vertexList.add(1f)
                vertexList.add(0f)
                vertexList.add(0.10f) // Emerald freshwater river
                vertexList.add(0.48f)
                vertexList.add(0.52f)
                vertexList.add(0.78f)
            }
        }

        val rowVertices = subdivisions + 1
        for (zIdx in 0 until subdivisions) {
            for (xIdx in 0 until subdivisions) {
                val i0 = (zIdx * rowVertices + xIdx).toShort()
                val i1 = (zIdx * rowVertices + (xIdx + 1)).toShort()
                val i2 = ((zIdx + 1) * rowVertices + (xIdx + 1)).toShort()
                val i3 = ((zIdx + 1) * rowVertices + xIdx).toShort()

                indexList.add(i0); indexList.add(i1); indexList.add(i2)
                indexList.add(i0); indexList.add(i2); indexList.add(i3)
            }
        }

        return createMeshFromData(vertexList, indexList, floatArrayOf(0.12f, 0.48f, 0.52f, 0.78f))
    }

    /**
     * Builds a geometric Web Mesh for projectile attack and stunning the Frog.
     */
    fun createWebMesh(radius: Float = 2.8f, spokes: Int = 14, rings: Int = 5): Mesh {
        val vertexList = ArrayList<Float>()
        val indexList = ArrayList<Short>()

        // Center hub
        vertexList.add(0f); vertexList.add(0f); vertexList.add(0f)
        vertexList.add(0f); vertexList.add(1f); vertexList.add(0f)
        vertexList.add(0.95f); vertexList.add(0.98f); vertexList.add(1f); vertexList.add(0.9f)

        for (ring in 1..rings) {
            val r = (ring.toFloat() / rings) * radius
            for (spoke in 0 until spokes) {
                val angle = spoke * 2f * Math.PI.toFloat() / spokes
                val x = cos(angle) * r
                val z = sin(angle) * r

                vertexList.add(x)
                vertexList.add(0f)
                vertexList.add(z)
                vertexList.add(0f); vertexList.add(1f); vertexList.add(0f)
                vertexList.add(0.92f); vertexList.add(0.96f); vertexList.add(1f); vertexList.add(0.85f)
            }
        }

        for (ring in 0 until rings) {
            if (ring == 0) {
                for (spoke in 0 until spokes) {
                    val nextSpoke = (spoke + 1) % spokes
                    indexList.add(0)
                    indexList.add((1 + spoke).toShort())
                    indexList.add((1 + nextSpoke).toShort())
                }
            } else {
                val currOffset = 1 + (ring - 1) * spokes
                val nextOffset = 1 + ring * spokes
                for (spoke in 0 until spokes) {
                    val nextSpoke = (spoke + 1) % spokes

                    val c0 = (currOffset + spoke).toShort()
                    val c1 = (currOffset + nextSpoke).toShort()
                    val n0 = (nextOffset + spoke).toShort()
                    val n1 = (nextOffset + nextSpoke).toShort()

                    indexList.add(c0); indexList.add(n0); indexList.add(n1)
                    indexList.add(c0); indexList.add(n1); indexList.add(c1)
                }
            }
        }

        return createMeshFromData(vertexList, indexList, floatArrayOf(1f, 1f, 1f, 0.88f))
    }

    fun createMeshFromData(vertexList: ArrayList<Float>, indexList: ArrayList<Short>, baseColor: FloatArray): Mesh {
        val vertexData = FloatArray(vertexList.size)
        for (i in vertexList.indices) {
            vertexData[i] = vertexList[i]
        }

        val indexData = ShortArray(indexList.size)
        for (i in indexList.indices) {
            indexData[i] = indexList[i]
        }

        val vBuffer = ByteBuffer.allocateDirect(vertexData.size * FLOAT_SIZE)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .apply {
                put(vertexData)
                position(0)
            }

        val iBuffer = ByteBuffer.allocateDirect(indexData.size * SHORT_SIZE)
            .order(ByteOrder.nativeOrder())
            .asShortBuffer()
            .apply {
                put(indexData)
                position(0)
            }

        return Mesh(vBuffer, iBuffer, indexData.size, baseColor)
    }
}
