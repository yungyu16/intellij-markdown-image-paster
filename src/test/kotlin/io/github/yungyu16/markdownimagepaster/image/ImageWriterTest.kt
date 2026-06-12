package io.github.yungyu16.markdownimagepaster.image

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.awt.image.BufferedImage
import java.nio.file.Path
import javax.imageio.ImageIO

class ImageWriterTest {

    @Test
    fun `prepareWrite returns bytes without writing to disk`(@TempDir tmp: Path) {
        val img = BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB)
        val targetDir = tmp.resolve("img/foo")
        val result = ImageWriter.prepareWrite(img, "png", targetDir, tmp)
        assertTrue(result.bytes.isNotEmpty())
        assertTrue(!targetDir.toFile().exists())
    }

    @Test
    fun `prepareWrite rejects target directory outside project root`(@TempDir tmp: Path) {
        val img = BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB)
        val outside = tmp.parent.resolve("outside-${tmp.fileName}")

        assertThrows(IllegalArgumentException::class.java) {
            ImageWriter.prepareWrite(img, "png", outside, tmp)
        }
    }

    @Test
    fun `write creates a file and returns project-relative path`(@TempDir tmp: Path) {
        val img = BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB)
        val projectRoot = tmp
        val targetDir = tmp.resolve("img/foo")
        val result = ImageWriter.write(img, "png", targetDir, "image", projectRoot)
        assertEquals("img/foo/image.png", result)
        assertTrue(targetDir.resolve("image.png").toFile().exists())
    }

    @Test
    fun `collision appends -2, -3 suffix`(@TempDir tmp: Path) {
        val img = BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB)
        val targetDir = tmp.resolve("img/foo")
        targetDir.toFile().mkdirs()
        targetDir.resolve("image.png").toFile().createNewFile()

        val result = ImageWriter.write(img, "png", targetDir, "image", tmp)
        assertEquals("img/foo/image-2.png", result)
    }

    @Test
    fun `sanitize strips path separators and nulls`() {
        assertEquals("image", ImageWriter.sanitize(""))
        assertEquals("image", ImageWriter.sanitize("   "))
        assertEquals("foo_bar", ImageWriter.sanitize("foo/bar"))
        assertEquals("foo_bar", ImageWriter.sanitize("foo\\bar"))
        assertEquals("foo_bar", ImageWriter.sanitize("foo" + '\u0000' + "bar"))
    }

    @Test
    fun `sanitize truncates very long names to 64 chars`() {
        val long = "a".repeat(100)
        val sanitized = ImageWriter.sanitize(long)
        assertEquals(64, sanitized.length)
    }

    @Test
    fun `write produces a valid PNG that ImageIO can re-read`(@TempDir tmp: Path) {
        val img = BufferedImage(4, 4, BufferedImage.TYPE_INT_ARGB)
        val targetDir = tmp.resolve("assets")
        ImageWriter.write(img, "png", targetDir, "test", tmp)
        val reRead = ImageIO.read(targetDir.resolve("test.png").toFile())
        assertEquals(4, reRead!!.width)
        assertEquals(4, reRead.height)
    }

    @Test
    fun `write rejects target directory outside project root`(@TempDir tmp: Path) {
        val img = BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB)
        val outside = tmp.parent.resolve("outside-${tmp.fileName}")

        assertThrows(IllegalArgumentException::class.java) {
            ImageWriter.write(img, "png", outside, "image", tmp)
        }

        assertTrue(!outside.resolve("image.png").toFile().exists())
    }

    @Test
    fun `sanitize replaces dot and dotdot with image`() {
        assertEquals("image", ImageWriter.sanitize("."))
        assertEquals("image", ImageWriter.sanitize(".."))
    }

    @Test
    fun `sanitize prefixes windows reserved names with underscore`() {
        assertEquals("_CON", ImageWriter.sanitize("CON"))
        assertEquals("_con", ImageWriter.sanitize("con"))
        assertEquals("_NUL", ImageWriter.sanitize("NUL"))
        assertEquals("_COM1", ImageWriter.sanitize("COM1"))
        assertEquals("_LPT9", ImageWriter.sanitize("LPT9"))
    }

    @Test
    fun `sanitize leaves non reserved names untouched`() {
        assertEquals("console", ImageWriter.sanitize("console"))
        assertEquals("COM10", ImageWriter.sanitize("COM10"))
    }
}
