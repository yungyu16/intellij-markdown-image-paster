package io.github.yungyu16.markdownimagepaster.path

data class FrontMatter(
    val typoraRootUrl: String?,
    val mediaSubpath: String?
)

object FrontMatterParser {

    private const val FRONT_MATTER_START = "---"

    fun parse(text: String): FrontMatter? {
        val lines = text.lineSequence().toList()
        if (lines.isEmpty() || lines[0].trim() != FRONT_MATTER_START) return null

        val endIndex = lines.withIndex()
            .firstOrNull { (i, l) -> i > 0 && l.trim() == FRONT_MATTER_START }
            ?.index
            ?: return null

        val yamlBlock = lines.subList(1, endIndex).joinToString("\n")
        val data: Map<String, Any> = try {
            @Suppress("UNCHECKED_CAST")
            org.yaml.snakeyaml.Yaml().load(yamlBlock) as? Map<String, Any> ?: return null
        } catch (_: Exception) {
            return null
        }

        return FrontMatter(
            typoraRootUrl = data["typora-root-url"]?.toString(),
            mediaSubpath = data["media_subpath"]?.toString()
        )
    }
}
