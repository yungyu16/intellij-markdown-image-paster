package io.github.yungyu16.markdownimagepaster.path

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class PathResolverTest {

    @Test
    fun `absolute typora-root-url is resolved from project root`() {
        val front = FrontMatter(typoraRootUrl = "/assets/", mediaSubpath = null)
        val result = PathResolver.resolve(front, "my-post", "_posts/")
        assertEquals("assets/img/my-post/", result)
    }

    @Test
    fun `relative typora-root-url is resolved from current file directory`() {
        val front = FrontMatter(typoraRootUrl = "assets/", mediaSubpath = null)
        val result = PathResolver.resolve(front, "my-post", "_posts/")
        assertEquals("_posts/assets/img/my-post/", result)
    }

    @Test
    fun `typora-root-url with empty fileParentPath yields clean path`() {
        val front = FrontMatter(typoraRootUrl = "/assets", mediaSubpath = null)
        val result = PathResolver.resolve(front, "post", "")
        assertEquals("assets/img/post/", result)
    }

    @Test
    fun `absolute media_subpath is resolved from project root`() {
        val front = FrontMatter(typoraRootUrl = null, mediaSubpath = "/assets/images/")
        val result = PathResolver.resolve(front, "post", "_posts/")
        assertEquals("assets/images/", result)
    }

    @Test
    fun `relative media_subpath is resolved from current file directory`() {
        val front = FrontMatter(typoraRootUrl = null, mediaSubpath = "assets/images/")
        val result = PathResolver.resolve(front, "post", "_posts/")
        assertEquals("_posts/assets/images/", result)
    }

    @Test
    fun `front matter path cannot escape project root`() {
        val front = FrontMatter(typoraRootUrl = null, mediaSubpath = "../../outside/")

        assertThrows(IllegalArgumentException::class.java) {
            PathResolver.resolve(front, "post", "_posts/")
        }
    }

    @Test
    fun `returns null when front matter has no recognized path fields`() {
        val result = PathResolver.resolve(null, "foo", "_posts/")
        assertNull(result)
    }

    @Test
    fun `typora-root-url with leading and trailing slashes is normalized`() {
        val front = FrontMatter(typoraRootUrl = "///assets///", mediaSubpath = null)
        val result = PathResolver.resolve(front, "post", "_posts/")
        assertEquals("assets/img/post/", result)
    }
}
