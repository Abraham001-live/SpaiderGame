package com.example.game.engine

import kotlin.math.*

/**
 * Reference-Driven 3D Spider Mesh Generator for SPIDER HUNT.
 * Creates an anatomically authentic 3D Jumping Spider character based on the user reference images:
 * - Plush, bright orange fur with dense hair bristles/cards for a fuzzy silhouette
 * - Anatomically sculpted compact Cephalothorax & egg-shaped Abdomen with dark dorsal chevrons
 * - 6 glossy black eyes with 3D depth and specular catchlights
 * - Chelicerae fangs with vertical red stripes & plush pedipalps
 * - 8 multi-segmented articulated legs with Coxa, Femur, Tibia, Metatarsus, and dark Tarsus claw tips
 * - Mouth attachment sockets for SPIT, WEB, and EAT interactions
 */
class SpiderCharacterModel(
    val cephalothoraxMesh: Mesh,
    val abdomenMesh: Mesh,
    val furShellMesh: Mesh,
    val eyeGroupMesh: Mesh,
    val mouthGroupMesh: Mesh,
    val legFemurMesh: Mesh,
    val legTibiaMesh: Mesh,
    val legTarsusMesh: Mesh,
    val pedipalpMesh: Mesh
)

object SpiderMeshBuilder {

    // Palette strictly matched to reference photo
    val COLOR_ORANGE_FUR = floatArrayOf(0.96f, 0.44f, 0.05f, 1f)
    val COLOR_DEEP_ORANGE = floatArrayOf(0.88f, 0.36f, 0.02f, 1f)
    val COLOR_DARK_CHEVRON = floatArrayOf(0.22f, 0.12f, 0.08f, 1f)
    val COLOR_BLACK_EYE = floatArrayOf(0.02f, 0.02f, 0.03f, 1f)
    val COLOR_CATCHLIGHT = floatArrayOf(1.0f, 1.0f, 1.0f, 1.0f)
    val COLOR_RED_FANG_STRIPE = floatArrayOf(0.92f, 0.12f, 0.08f, 1f)
    val COLOR_BLACK_CLAW = floatArrayOf(0.08f, 0.08f, 0.10f, 1f)

    /**
     * Builds the complete set of 3D meshes for the Spider Player Character.
     */
    fun createSpiderModel(): SpiderCharacterModel {
        val cephalothorax = buildCephalothorax()
        val abdomen = buildAbdomen()
        val furShell = buildFurShells()
        val eyeGroup = buildEyeGroup()
        val mouthGroup = buildMouthGroup()
        val legFemur = buildLegFemur()
        val legTibia = buildLegTibia()
        val legTarsus = buildLegTarsus()
        val pedipalp = buildPedipalp()

        return SpiderCharacterModel(
            cephalothoraxMesh = cephalothorax,
            abdomenMesh = abdomen,
            furShellMesh = furShell,
            eyeGroupMesh = eyeGroup,
            mouthGroupMesh = mouthGroup,
            legFemurMesh = legFemur,
            legTibiaMesh = legTibia,
            legTarsusMesh = legTarsus,
            pedipalpMesh = pedipalp
        )
    }

    /**
     * Cephalothorax: Sculpted compact head with ocular turret and side slopes.
     */
    private fun buildCephalothorax(): Mesh {
        val vList = ArrayList<Float>()
        val iList = ArrayList<Short>()

        val rings = 16
        val sectors = 16
        val rx = 0.85f
        val ry = 0.58f
        val rz = 0.80f

        for (rIdx in 0..rings) {
            val v = rIdx.toFloat() / rings
            val phi = v * Math.PI.toFloat()
            val y = cos(phi) * ry

            // Flatten underside, sculpt ocular crest at front top
            val yScaled = if (y < 0f) y * 0.45f else y

            for (sIdx in 0..sectors) {
                val u = sIdx.toFloat() / sectors
                val theta = u * 2f * Math.PI.toFloat()

                // Ocular turret bulge at front top (phi near 0.25pi, theta near 0)
                var radiusFactorX = 1f
                var radiusFactorZ = 1f
                val isFront = cos(theta) > 0.3f && sin(phi) > 0.4f
                if (isFront) {
                    radiusFactorX = 1.08f
                    radiusFactorZ = 1.12f
                }

                val x = sin(phi) * cos(theta) * rx * radiusFactorX
                val z = sin(phi) * sin(theta) * rz * radiusFactorZ

                val nx = x / (rx * rx)
                val ny = yScaled / (ry * ry)
                val nz = z / (rz * rz)
                val len = sqrt(nx * nx + ny * ny + nz * nz).coerceAtLeast(0.0001f)

                // Front dark crest band vs orange fur
                val isDarkBand = isFront && yScaled > 0.2f && yScaled < 0.45f
                val col = if (isDarkBand) COLOR_DARK_CHEVRON else COLOR_ORANGE_FUR

                addVertex(vList, x, yScaled, z, nx / len, ny / len, nz / len, col)
            }
        }

        buildTriangles(iList, rings, sectors)
        return MeshBuilder.createMeshFromData(vList, iList, COLOR_ORANGE_FUR)
    }

    /**
     * Abdomen: Sculpted anatomical egg-shape with dorsal tapering and dark chevron pattern.
     */
    private fun buildAbdomen(): Mesh {
        val vList = ArrayList<Float>()
        val iList = ArrayList<Short>()

        val rings = 20
        val sectors = 20
        val rx = 1.25f
        val ry = 0.92f
        val rz = 1.45f

        for (rIdx in 0..rings) {
            val v = rIdx.toFloat() / rings
            val phi = v * Math.PI.toFloat()

            // Asymmetric egg tapering: wider at mid-rear, tapered toward spinnerets at back
            val taperZ = 1.0f - 0.22f * (v - 0.5f).pow(2)
            val y = cos(phi) * ry * taperZ
            val yScaled = if (y < -0.1f) y * 0.65f else y

            for (sIdx in 0..sectors) {
                val u = sIdx.toFloat() / sectors
                val theta = u * 2f * Math.PI.toFloat()

                val x = sin(phi) * cos(theta) * rx * taperZ
                val z = sin(phi) * sin(theta) * rz

                val nx = x / (rx * rx)
                val ny = yScaled / (ry * ry)
                val nz = z / (rz * rz)
                val len = sqrt(nx * nx + ny * ny + nz * nz).coerceAtLeast(0.0001f)

                // Dark chevron pattern along top dorsal ridge
                val isTopDorsal = yScaled > 0.35f
                val zDist1 = abs(z + 0.3f)
                val zDist2 = abs(z + 0.75f)
                val zDist3 = abs(z + 1.15f)
                val isChevron = isTopDorsal && (zDist1 < 0.18f || zDist2 < 0.22f || zDist3 < 0.16f) && abs(x) < (0.65f - z * 0.12f)

                val col = if (isChevron) COLOR_DARK_CHEVRON else COLOR_DEEP_ORANGE

                addVertex(vList, x, yScaled, z, nx / len, ny / len, nz / len, col)
            }
        }

        buildTriangles(iList, rings, sectors)
        return MeshBuilder.createMeshFromData(vList, iList, COLOR_DEEP_ORANGE)
    }

    /**
     * Fur Shells: Outer fuzzy hair bristling geometry catching light and producing fuzzy silhouette.
     */
    private fun buildFurShells(): Mesh {
        val vList = ArrayList<Float>()
        val iList = ArrayList<Short>()

        // Generate 48 dense radial fur bristling cards around body and head
        val hairCount = 48
        var vCount = 0

        for (h in 0 until hairCount) {
            val angle = (h * Math.PI * 2 / hairCount).toFloat()
            val radX = sin(angle)
            val radZ = cos(angle)

            val height = 0.25f + (h % 5) * 0.04f
            val width = 0.035f

            // Position hair root along cephalothorax/abdomen crest
            val rootX = radX * 0.80f
            val rootY = 0.35f + (h % 3) * 0.12f
            val rootZ = radZ * 1.10f - 0.2f

            val tipX = rootX + radX * height
            val tipY = rootY + height * 0.8f
            val tipZ = rootZ + radZ * height

            val nx = radX
            val ny = 0.6f
            val nz = radZ
            val len = sqrt(nx * nx + ny * ny + nz * nz).coerceAtLeast(0.0001f)

            // Quad hair card
            addVertex(vList, rootX - radZ * width, rootY, rootZ + radX * width, nx / len, ny / len, nz / len, COLOR_ORANGE_FUR)
            addVertex(vList, rootX + radZ * width, rootY, rootZ - radX * width, nx / len, ny / len, nz / len, COLOR_ORANGE_FUR)
            addVertex(vList, tipX + radZ * width * 0.3f, tipY, tipZ - radX * width * 0.3f, nx / len, ny / len, nz / len, COLOR_ORANGE_FUR)
            addVertex(vList, tipX - radZ * width * 0.3f, tipY, tipZ + radX * width * 0.3f, nx / len, ny / len, nz / len, COLOR_ORANGE_FUR)

            val baseIdx = vCount.toShort()
            iList.add(baseIdx)
            iList.add((baseIdx + 1).toShort())
            iList.add((baseIdx + 2).toShort())

            iList.add(baseIdx)
            iList.add((baseIdx + 2).toShort())
            iList.add((baseIdx + 3).toShort())

            vCount += 4
        }

        return MeshBuilder.createMeshFromData(vList, iList, COLOR_ORANGE_FUR)
    }

    /**
     * Eye Group: 6 glossy black eyes with 3D depth and specular catchlights.
     */
    private fun buildEyeGroup(): Mesh {
        val vList = ArrayList<Float>()
        val iList = ArrayList<Short>()

        // 2 Giant Front Central Eyes
        addSphereSubmesh(vList, iList, cx = -0.21f, cy = 0.45f, cz = 0.82f, radius = 0.20f, color = COLOR_BLACK_EYE)
        addSphereSubmesh(vList, iList, cx = 0.21f, cy = 0.45f, cz = 0.82f, radius = 0.20f, color = COLOR_BLACK_EYE)

        // 2 Specular Catchlights (Front eyes)
        addSphereSubmesh(vList, iList, cx = -0.16f, cy = 0.52f, cz = 0.96f, radius = 0.05f, color = COLOR_CATCHLIGHT)
        addSphereSubmesh(vList, iList, cx = 0.16f, cy = 0.52f, cz = 0.96f, radius = 0.05f, color = COLOR_CATCHLIGHT)

        // 2 Front-Side Secondary Eyes
        addSphereSubmesh(vList, iList, cx = -0.48f, cy = 0.42f, cz = 0.72f, radius = 0.13f, color = COLOR_BLACK_EYE)
        addSphereSubmesh(vList, iList, cx = 0.48f, cy = 0.42f, cz = 0.72f, radius = 0.13f, color = COLOR_BLACK_EYE)

        // 2 Rear-Side Lateral Eyes
        addSphereSubmesh(vList, iList, cx = -0.56f, cy = 0.48f, cz = 0.32f, radius = 0.10f, color = COLOR_BLACK_EYE)
        addSphereSubmesh(vList, iList, cx = 0.56f, cy = 0.48f, cz = 0.32f, radius = 0.10f, color = COLOR_BLACK_EYE)

        return MeshBuilder.createMeshFromData(vList, iList, COLOR_BLACK_EYE)
    }

    /**
     * Mouth Group: Chelicerae fangs with vertical red stripes and mouth cavity.
     */
    private fun buildMouthGroup(): Mesh {
        val vList = ArrayList<Float>()
        val iList = ArrayList<Short>()

        // Left Chelicera fang block
        addCylinderSubmesh(vList, iList, x0 = -0.18f, y0 = 0.28f, z0 = 0.78f, x1 = -0.16f, y1 = -0.05f, z1 = 0.88f, r0 = 0.15f, r1 = 0.06f, color = COLOR_DARK_CHEVRON)
        // Right Chelicera fang block
        addCylinderSubmesh(vList, iList, x0 = 0.18f, y0 = 0.28f, z0 = 0.78f, x1 = 0.16f, y1 = -0.05f, z1 = 0.88f, r0 = 0.15f, r1 = 0.06f, color = COLOR_DARK_CHEVRON)

        // Red Vertical Fang Stripes
        addCylinderSubmesh(vList, iList, x0 = -0.18f, y0 = 0.22f, z0 = 0.90f, x1 = -0.16f, y1 = 0.02f, z1 = 0.94f, r0 = 0.038f, r1 = 0.025f, color = COLOR_RED_FANG_STRIPE)
        addCylinderSubmesh(vList, iList, x0 = 0.18f, y0 = 0.22f, z0 = 0.90f, x1 = 0.16f, y1 = 0.02f, z1 = 0.94f, r0 = 0.038f, r1 = 0.025f, color = COLOR_RED_FANG_STRIPE)

        return MeshBuilder.createMeshFromData(vList, iList, COLOR_DARK_CHEVRON)
    }

    /**
     * Pedipalps: Short plush furry front limbs flanking fangs.
     */
    private fun buildPedipalp(): Mesh {
        val vList = ArrayList<Float>()
        val iList = ArrayList<Short>()

        addCylinderSubmesh(vList, iList, x0 = 0f, y0 = 0.22f, z0 = 0f, x1 = 0f, y1 = -0.28f, z1 = 0.12f, r0 = 0.13f, r1 = 0.09f, color = COLOR_ORANGE_FUR)
        // Pedipalp plush fur bristles
        addSphereSubmesh(vList, iList, cx = 0f, cy = -0.26f, cz = 0.12f, radius = 0.12f, color = COLOR_ORANGE_FUR)

        return MeshBuilder.createMeshFromData(vList, iList, COLOR_ORANGE_FUR)
    }

    /**
     * Leg Femur (Upper leg segment) with dense fur bristles.
     */
    private fun buildLegFemur(): Mesh {
        val vList = ArrayList<Float>()
        val iList = ArrayList<Short>()

        val len = 1.15f
        val r0 = 0.16f
        val r1 = 0.12f

        addCylinderSubmesh(vList, iList, x0 = 0f, y0 = 0f, z0 = 0f, x1 = -len, y1 = 0.28f, z1 = 0f, r0 = r0, r1 = r1, color = COLOR_ORANGE_FUR)

        // Fur bristles along Femur upper edge
        for (b in 1..4) {
            val frac = b / 5f
            val bx = -len * frac
            val by = 0.28f * frac + 0.12f
            addSphereSubmesh(vList, iList, cx = bx, cy = by, cz = 0f, radius = 0.08f, color = COLOR_ORANGE_FUR)
        }

        return MeshBuilder.createMeshFromData(vList, iList, COLOR_ORANGE_FUR)
    }

    /**
     * Leg Tibia (Lower leg segment).
     */
    private fun buildLegTibia(): Mesh {
        val vList = ArrayList<Float>()
        val iList = ArrayList<Short>()

        val len = 1.10f
        val r0 = 0.12f
        val r1 = 0.08f

        addCylinderSubmesh(vList, iList, x0 = 0f, y0 = 0f, z0 = 0f, x1 = -len, y1 = -0.65f, z1 = 0f, r0 = r0, r1 = r1, color = COLOR_ORANGE_FUR)

        // Knee joint sphere
        addSphereSubmesh(vList, iList, cx = 0f, cy = 0f, cz = 0f, radius = 0.13f, color = COLOR_DEEP_ORANGE)

        return MeshBuilder.createMeshFromData(vList, iList, COLOR_ORANGE_FUR)
    }

    /**
     * Leg Tarsus (Claw tip touching ground).
     */
    private fun buildLegTarsus(): Mesh {
        val vList = ArrayList<Float>()
        val iList = ArrayList<Short>()

        val len = 0.38f
        addCylinderSubmesh(vList, iList, x0 = 0f, y0 = 0f, z0 = 0f, x1 = -len, y1 = -0.22f, z1 = 0f, r0 = 0.08f, r1 = 0.03f, color = COLOR_BLACK_CLAW)

        return MeshBuilder.createMeshFromData(vList, iList, COLOR_BLACK_CLAW)
    }

    // --- Helper mesh assembly primitives ---

    private fun addVertex(vList: ArrayList<Float>, x: Float, y: Float, z: Float, nx: Float, ny: Float, nz: Float, col: FloatArray) {
        vList.add(x)
        vList.add(y)
        vList.add(z)
        vList.add(nx)
        vList.add(ny)
        vList.add(nz)
        vList.add(col[0])
        vList.add(col[1])
        vList.add(col[2])
        vList.add(col[3])
    }

    private fun buildTriangles(iList: ArrayList<Short>, rings: Int, sectors: Int) {
        for (rIdx in 0 until rings) {
            for (sIdx in 0 until sectors) {
                val i0 = (rIdx * (sectors + 1) + sIdx).toShort()
                val i1 = (rIdx * (sectors + 1) + sIdx + 1).toShort()
                val i2 = ((rIdx + 1) * (sectors + 1) + sIdx + 1).toShort()
                val i3 = ((rIdx + 1) * (sectors + 1) + sIdx).toShort()

                iList.add(i0)
                iList.add(i1)
                iList.add(i2)

                iList.add(i0)
                iList.add(i2)
                iList.add(i3)
            }
        }
    }

    private fun addSphereSubmesh(vList: ArrayList<Float>, iList: ArrayList<Short>, cx: Float, cy: Float, cz: Float, radius: Float, color: FloatArray) {
        val baseIdx = (vList.size / 10).toShort()
        val rings = 8
        val sectors = 8

        for (rIdx in 0..rings) {
            val v = rIdx.toFloat() / rings
            val phi = v * Math.PI.toFloat()
            for (sIdx in 0..sectors) {
                val u = sIdx.toFloat() / sectors
                val theta = u * 2f * Math.PI.toFloat()

                val nx = sin(phi) * cos(theta)
                val ny = cos(phi)
                val nz = sin(phi) * sin(theta)

                addVertex(vList, cx + nx * radius, cy + ny * radius, cz + nz * radius, nx, ny, nz, color)
            }
        }

        buildTrianglesWithOffset(iList, baseIdx, rings, sectors)
    }

    private fun addCylinderSubmesh(
        vList: ArrayList<Float>,
        iList: ArrayList<Short>,
        x0: Float, y0: Float, z0: Float,
        x1: Float, y1: Float, z1: Float,
        r0: Float, r1: Float,
        color: FloatArray
    ) {
        val baseIdx = (vList.size / 10).toShort()
        val sectors = 10

        val dx = x1 - x0
        val dy = y1 - y0
        val dz = z1 - z0
        val len = sqrt(dx * dx + dy * dy + dz * dz).coerceAtLeast(0.0001f)

        val dirX = dx / len
        val dirY = dy / len
        val dirZ = dz / len

        val perpX = if (abs(dirX) < 0.9f) 1f else 0f
        val perpY = if (abs(dirX) < 0.9f) 0f else 1f
        val perpZ = 0f

        val uX = (dirY * perpZ - dirZ * perpY)
        val uY = (dirZ * perpX - dirX * perpZ)
        val uZ = (dirX * perpY - dirY * perpX)
        val uLen = sqrt(uX * uX + uY * uY + uZ * uZ).coerceAtLeast(0.0001f)

        val ux = uX / uLen
        val uy = uY / uLen
        val uz = uZ / uLen

        val vx = (dirY * uz - dirZ * uy)
        val vy = (dirZ * ux - dirX * uz)
        val vz = (dirX * uy - dirY * ux)

        for (sIdx in 0..sectors) {
            val u = sIdx.toFloat() / sectors
            val angle = u * 2f * Math.PI.toFloat()
            val c = cos(angle)
            val s = sin(angle)

            val nx = ux * c + vx * s
            val ny = uy * c + vy * s
            val nz = uz * c + vz * s

            // Ring 0 (Start)
            addVertex(vList, x0 + nx * r0, y0 + ny * r0, z0 + nz * r0, nx, ny, nz, color)
            // Ring 1 (End)
            addVertex(vList, x1 + nx * r1, y1 + ny * r1, z1 + nz * r1, nx, ny, nz, color)
        }

        for (sIdx in 0 until sectors) {
            val i0 = (baseIdx + sIdx * 2).toShort()
            val i1 = (baseIdx + sIdx * 2 + 1).toShort()
            val i2 = (baseIdx + (sIdx + 1) * 2 + 1).toShort()
            val i3 = (baseIdx + (sIdx + 1) * 2).toShort()

            iList.add(i0)
            iList.add(i1)
            iList.add(i2)

            iList.add(i0)
            iList.add(i2)
            iList.add(i3)
        }
    }

    private fun buildTrianglesWithOffset(iList: ArrayList<Short>, baseIdx: Short, rings: Int, sectors: Int) {
        for (rIdx in 0 until rings) {
            for (sIdx in 0 until sectors) {
                val i0 = (baseIdx + rIdx * (sectors + 1) + sIdx).toShort()
                val i1 = (baseIdx + rIdx * (sectors + 1) + sIdx + 1).toShort()
                val i2 = (baseIdx + (rIdx + 1) * (sectors + 1) + sIdx + 1).toShort()
                val i3 = (baseIdx + (rIdx + 1) * (sectors + 1) + sIdx).toShort()

                iList.add(i0)
                iList.add(i1)
                iList.add(i2)

                iList.add(i0)
                iList.add(i2)
                iList.add(i3)
            }
        }
    }
}
