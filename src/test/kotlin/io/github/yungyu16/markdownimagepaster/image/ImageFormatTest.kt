package io.github.yungyu16.markdownimagepaster.image

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.awt.Image
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable
import java.awt.datatransfer.UnsupportedFlavorException
import java.awt.image.BufferedImage
import java.io.File

class ImageFormatTest {

    private class BufferedImageTransferable(private val image: BufferedImage) : Transferable {
        override fun getTransferData(flavor: DataFlavor): Any {
            if (!isDataFlavorSupported(flavor)) throw UnsupportedFlavorException(flavor)
            return image
        }
        override fun getTransferDataFlavors(): Array<DataFlavor> = arrayOf(DataFlavor.imageFlavor)
        override fun isDataFlavorSupported(flavor: DataFlavor): Boolean = flavor == DataFlavor.imageFlavor
    }

    @Test
    fun `detect returns png ImageData for BufferedImage on imageFlavor`() {
        val img = BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB)
        val result = ImageFormatDetector.detect(BufferedImageTransferable(img))
        assertNotNull(result)
        assertEquals("png", result!!.format)
        assertEquals(img, result.image)
        assertNull(result.sourceName)
    }

    @Test
    fun `detect extracts source name from file list flavor when present`() {
        val img = BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB)
        val file = File("cover image.jpg")
        val result = ImageFormatDetector.detect(
            BufferedImageTransferable(img).wrapWithFileList(listOf(file))
        )
        assertNotNull(result)
        assertEquals("cover image", result!!.sourceName)
    }

    @Test
    fun `detect converts non-BufferedImage Image from imageFlavor`() {
        val source = BufferedImage(2, 3, BufferedImage.TYPE_INT_RGB)
        val image = source.getScaledInstance(2, 3, Image.SCALE_DEFAULT)
        val result = ImageFormatDetector.detect(BufferedImageTransferable(source).wrapWith(image))
        assertNotNull(result)
        assertEquals("png", result!!.format)
        assertEquals(2, result.image.width)
        assertEquals(3, result.image.height)
    }

    @Test
    fun `detect returns null for null transferable`() {
        assertNull(ImageFormatDetector.detect(null))
    }

    private fun Transferable.wrapWith(value: Any): Transferable = object : Transferable {
        override fun getTransferData(flavor: DataFlavor): Any {
            if (!isDataFlavorSupported(flavor)) throw UnsupportedFlavorException(flavor)
            return value
        }

        override fun getTransferDataFlavors(): Array<DataFlavor> = this@wrapWith.transferDataFlavors

        override fun isDataFlavorSupported(flavor: DataFlavor): Boolean =
            this@wrapWith.isDataFlavorSupported(flavor)
    }

    private fun Transferable.wrapWithFileList(files: List<File>): Transferable = object : Transferable {
        override fun getTransferData(flavor: DataFlavor): Any =
            when (flavor) {
                DataFlavor.imageFlavor -> this@wrapWithFileList.getTransferData(flavor)
                DataFlavor.javaFileListFlavor -> files
                else -> throw UnsupportedFlavorException(flavor)
            }

        override fun getTransferDataFlavors(): Array<DataFlavor> =
            arrayOf(DataFlavor.imageFlavor, DataFlavor.javaFileListFlavor)

        override fun isDataFlavorSupported(flavor: DataFlavor): Boolean =
            flavor == DataFlavor.imageFlavor || flavor == DataFlavor.javaFileListFlavor
    }
}
