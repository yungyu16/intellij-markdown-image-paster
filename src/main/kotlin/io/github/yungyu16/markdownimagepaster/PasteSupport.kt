package io.github.yungyu16.markdownimagepaster

import io.github.yungyu16.markdownimagepaster.image.ImageWriter
import io.github.yungyu16.markdownimagepaster.path.FrontMatterParser
import io.github.yungyu16.markdownimagepaster.path.PathResolver
import java.time.Clock
import java.time.format.DateTimeFormatter

internal object PasteSupport {

    private val timestampFormatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss")

    fun resolveTargetDir(
        documentText: String,
        fileBaseName: String,
        fileParentPath: String
    ): String? {
        val frontMatter = FrontMatterParser.parse(documentText) ?: return null
        return try {
            PathResolver.resolve(frontMatter, fileBaseName, fileParentPath)
        } catch (_: IllegalArgumentException) {
            null
        }
    }

    fun imageFileName(sourceName: String?, clock: Clock = Clock.systemDefaultZone()): String {
        val baseName = ImageWriter.sanitize(sourceName.orEmpty())
        val timestamp = timestampFormatter.format(clock.instant().atZone(clock.zone))
        return "$baseName-$timestamp"
    }

    fun markdownImagePath(projectRelativePath: String): String =
        "/" + projectRelativePath.trim('/')
}
