package io.github.yungyu16.markdownimagepaster.image

import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import javax.imageio.ImageIO

object ImageWriter {

    data class WriteTarget(val bytes: ByteArray)

    private val writeLock = Any()
    private const val MAX_COLLISION_COUNTER = 10_000

    private val WINDOWS_RESERVED_NAMES = setOf(
        "CON", "PRN", "AUX", "NUL",
        "COM1", "COM2", "COM3", "COM4", "COM5", "COM6", "COM7", "COM8", "COM9",
        "LPT1", "LPT2", "LPT3", "LPT4", "LPT5", "LPT6", "LPT7", "LPT8", "LPT9"
    )

    fun prepareWrite(
        image: BufferedImage,
        format: String,
        targetDir: Path,
        projectBasePath: Path
    ): WriteTarget {
        val projectRoot = projectBasePath.toAbsolutePath().normalize()
        val safeTargetDir = targetDir.toAbsolutePath().normalize()
        require(safeTargetDir.startsWith(projectRoot)) {
            "Target directory must be inside project root: $safeTargetDir"
        }

        val bytes = synchronized(writeLock) { encodeImage(image, format) }
        return WriteTarget(bytes)
    }

    /** @deprecated Retained for testing. Production code uses [prepareWrite] + VFS. */
    fun write(
        image: BufferedImage,
        format: String,
        targetDir: Path,
        fileName: String,
        projectBasePath: Path
    ): String {
        val projectRoot = projectBasePath.toAbsolutePath().normalize()
        val safeTargetDir = targetDir.toAbsolutePath().normalize()
        require(safeTargetDir.startsWith(projectRoot)) {
            "Target directory must be inside project root: $safeTargetDir"
        }

        val resolvedName: String
        val bytes: ByteArray
        synchronized(writeLock) {
            resolvedName = resolveName(safeTargetDir, fileName, format)
            bytes = encodeImage(image, format)
        }

        val targetFile = safeTargetDir.resolve("$resolvedName.$format")
        Files.createDirectories(targetFile.parent)
        Files.write(targetFile, bytes)

        return projectRoot.relativize(targetFile).toString().replace('\\', '/')
    }

    fun sanitize(fileName: String): String {
        val safe = fileName
            .replace('\\', '_')
            .replace('/', '_')
            .replace('\u0000', '_')
            .take(64)
            .trim()
            .let { if (it.isEmpty() || it == "." || it == "..") "image" else it }
            .let { if (it.uppercase() in WINDOWS_RESERVED_NAMES) "_$it" else it }
        return if (safe.isEmpty()) "image" else safe
    }

    private fun encodeImage(image: BufferedImage, format: String): ByteArray {
        val stream = ByteArrayOutputStream()
        val ok = ImageIO.write(image, format, stream)
        if (!ok) throw IOException("No ImageIO writer found for format: $format")
        return stream.toByteArray()
    }

    private fun resolveName(dir: Path, baseName: String, ext: String): String {
        if (!Files.exists(dir.resolve("$baseName.$ext"))) return baseName
        var counter = 2
        while (counter <= MAX_COLLISION_COUNTER) {
            if (!Files.exists(dir.resolve("$baseName-$counter.$ext"))) {
                return "$baseName-$counter"
            }
            counter++
        }
        throw IOException("Could not find a non-conflicting filename for '$baseName' after $MAX_COLLISION_COUNTER attempts")
    }
}