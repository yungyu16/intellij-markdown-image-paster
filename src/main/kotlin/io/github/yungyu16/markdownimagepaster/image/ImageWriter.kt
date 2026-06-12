package io.github.yungyu16.markdownimagepaster.image

import java.awt.image.BufferedImage
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import javax.imageio.ImageIO

object ImageWriter {

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

        Files.createDirectories(safeTargetDir)

        val ext = if (format == "jpeg") "jpg" else format
        val resolvedName = resolveName(safeTargetDir, fileName, ext)
        val targetFile = safeTargetDir.resolve("$resolvedName.$ext")

        val ok = ImageIO.write(image, ext, targetFile.toFile())
        if (!ok) throw IOException("No ImageIO writer found for format: $ext")

        return projectRoot.relativize(targetFile).toString().replace('\\', '/')
    }

    fun sanitize(fileName: String): String {
        val safe = fileName
            .replace('\\', '_')
            .replace('/', '_')
            .replace('\u0000', '_')
            .take(64)
            .trim()
        return if (safe.isEmpty()) "image" else safe
    }

    private fun resolveName(dir: Path, baseName: String, ext: String): String {
        if (!Files.exists(dir.resolve("$baseName.$ext"))) return baseName
        var counter = 2
        while (Files.exists(dir.resolve("$baseName-$counter.$ext"))) {
            counter++
        }
        return "$baseName-$counter"
    }
}
