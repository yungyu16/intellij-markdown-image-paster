package io.github.yungyu16.markdownimagepaster

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class PasteSupportTest {

    @Test
    fun `image file name uses clipboard source name plus timestamp`() {
        val clock = Clock.fixed(Instant.parse("2026-06-12T12:34:56Z"), ZoneOffset.UTC)

        val result = PasteSupport.imageFileName("cover image", clock)

        assertEquals("cover image-20260612123456", result)
    }

    @Test
    fun `image file name falls back to image when source name is absent`() {
        val clock = Clock.fixed(Instant.parse("2026-06-12T12:34:56Z"), ZoneOffset.UTC)

        val result = PasteSupport.imageFileName(null, clock)

        assertEquals("image-20260612123456", result)
    }

    @Test
    fun `target path is resolved only when current document has front matter`() {
        val text = """
            ---
            media_subpath: /assets/current/
            ---
            # Body
        """.trimIndent()

        val result = PasteSupport.resolveTargetDir(text, "post", "_posts/")

        assertEquals("assets/current/", result)
    }

    @Test
    fun `target path returns null when current document has no front matter`() {
        val result = PasteSupport.resolveTargetDir("# Body", "post", "_posts/")

        assertNull(result)
    }

    @Test
    fun `target path returns null when front matter has no recognized path fields`() {
        val text = """
            ---
            title: Post
            ---
            # Body
        """.trimIndent()

        val result = PasteSupport.resolveTargetDir(text, "post", "_posts/")

        assertNull(result)
    }

    @Test
    fun `target path returns null when front matter path escapes project root`() {
        val text = """
            ---
            media_subpath: ../../outside
            ---
            # Body
        """.trimIndent()

        val result = PasteSupport.resolveTargetDir(text, "post", "_posts/")

        assertNull(result)
    }

    @Test
    fun `markdown image path is project absolute without url encoding`() {
        val result = PasteSupport.markdownImagePath(
            "img/2026-06-04-Claude Code 权限模式：四种模式怎么选/cover image-20260612123456.png"
        )

        assertEquals(
            "/img/2026-06-04-Claude Code 权限模式：四种模式怎么选/cover image-20260612123456.png",
            result
        )
    }
}
