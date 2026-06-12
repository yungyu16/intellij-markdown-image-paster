package io.github.yungyu16.markdownimagepaster.path

import java.nio.file.Path

object PathResolver {

    /**
     * 按优先级决定目标目录。
     *
     * @return 项目根相对路径，使用 `/` 分隔，无前导斜杠，以 `/` 结尾；没有认识的路径字段时返回 null。
     */
    fun resolve(
        frontMatter: FrontMatter?,
        fileBaseName: String,
        fileParentPath: String
    ): String? {
        frontMatter?.typoraRootUrl?.let { rootUrl ->
            val base = resolveFrontMatterPath(rootUrl, fileParentPath) ?: return null
            return appendSegment(base, "img/$fileBaseName")
        }

        frontMatter?.mediaSubpath?.let { subpath ->
            return resolveFrontMatterPath(subpath, fileParentPath)
        }

        return null
    }

    private fun resolveFrontMatterPath(value: String, fileParentPath: String): String? {
        val trimmed = value.trim()
        val base = if (trimmed.startsWith("/")) "" else fileParentPath
        val rawPath = trimmed.trim('/')
        val path = when {
            base.isBlank() -> Path.of(rawPath)
            rawPath.isBlank() -> Path.of(base)
            else -> Path.of(base).resolve(rawPath)
        }.normalize()

        if (path.isAbsolute || containsParentTraversal(path)) return null

        return path.toString().replace('\\', '/').trim('/').let {
            if (it.isEmpty()) "" else "$it/"
        }
    }

    private fun appendSegment(base: String, segment: String): String =
        "${base.trimEnd('/')}/${segment.trim('/')}/".trimStart('/')

    private fun containsParentTraversal(path: Path): Boolean =
        path.any { it.toString() == ".." }
}
