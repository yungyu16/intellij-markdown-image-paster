plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.intellij.platform)
}

group = "io.github.yungyu16"
// CI 通过环境变量 PLUGIN_VERSION 注入（来自 git tag 去掉 v 前缀）；本地默认 1.0.0-dev
version = System.getenv("PLUGIN_VERSION") ?: "1.0.0-dev"

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    intellijPlatform {
        intellijIdeaCommunity("2024.2")
    }

    testImplementation(libs.junit5.jupiter)
    testRuntimeOnly(libs.junit5.jupiter.engine)
}

tasks.test {
    useJUnitPlatform()
}

intellijPlatform {
    pluginConfiguration {
        id = "io.github.yungyu16.markdown-image-paster"
        name = "Markdown Image Paster"
        version = project.version.toString()

        description = """
            <p>Paste images into Markdown files — auto-saved to the correct directory based on front matter rules.</p>
            <br/>
            <p><b>Why this plugin?</b></p>
            <p>
                Jekyll / Hexo and other static blog frameworks require strict path correspondence between posts and images.
                IntelliJ's built-in Markdown plugin doesn't support dynamic image paths based on the document filename.
                This plugin fills that gap.
            </p>
            <br/>
            <p><b>Features</b></p>
            <ul>
                <li>Intercept image paste only in Markdown files with front matter; other files keep the IDE default paste behavior.</li>
                <li>Front matter aware: <code>typora-root-url</code> is treated as a Typora resource root and expands to <code>.../img/&lt;basename&gt;/</code>; <code>media_subpath</code> is treated as the target image directory. Other front matter is ignored.</li>
                <li>Path rules: absolute front matter paths are project-relative; relative paths are resolved from the current Markdown file directory and cannot escape the project.</li>
                <li>No prompt: images are named from the clipboard source filename when available, otherwise <code>image</code>, plus a <code>yyyyMMddHHmmss</code> timestamp.</li>
                <li>Markdown links use project-root absolute paths and URL-encoded path segments.</li>
                <li>Stable output: clipboard images are written as PNG.</li>
            </ul>
            <br/>
            <p>在 Markdown 文件中粘贴图片时，根据 front matter 规则自动保存到正确目录。</p>
            <br/>
            <p><b>背景</b></p>
            <p>
                Jekyll / Hexo 等静态博客的文章文件和图片目录有严格的路径对应关系。
                IntelliJ 内置 Markdown 插件不支持根据文件名动态决定图片路径，此插件弥补了这一能力。
            </p>
            <br/>
            <p><b>功能</b></p>
            <ul>
                <li>仅拦截带 front matter 的 Markdown 图片粘贴，其它情况保留 IDE 默认粘贴行为。</li>
                <li>识别 front matter：<code>typora-root-url</code> 按 Typora 资源根语义展开为 <code>.../img/&lt;basename&gt;/</code>；<code>media_subpath</code> 按目标图片目录处理；其它 front matter 不接管。</li>
                <li>路径规则：front matter 绝对路径从 project 根目录解析，相对路径从当前 Markdown 文件目录解析，且不能逃逸出 project。</li>
                <li>无需弹窗：图片文件名优先使用剪贴板原文件名，拿不到时使用 <code>image</code>，并追加 <code>yyyyMMddHHmmss</code> 时间戳。</li>
                <li>Markdown 引用使用 project 根目录绝对路径，并对路径片段做 URL encode。</li>
                <li>稳定输出：剪贴板图片统一写为 PNG。</li>
            </ul>
        """.trimIndent()

        vendor {
            name = "yungyu16"
            url = "https://github.com/yungyu16"
        }

        changeNotes = """
            <p>Full changelog: <a href="https://github.com/yungyu16/intellij-markdown-image-paster/releases">GitHub Releases</a>.</p>
            <p>完整更新日志参见 <a href="https://github.com/yungyu16/intellij-markdown-image-paster/releases">GitHub Releases</a>。</p>
        """.trimIndent()

        ideaVersion {
            sinceBuild = "242"
            untilBuild = provider { null }
        }
    }
}

kotlin {
    jvmToolchain(17)
}
