package io.github.yungyu16.markdownimagepaster.image

import com.intellij.openapi.diagnostic.Logger
import java.awt.Image
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable
import java.awt.datatransfer.UnsupportedFlavorException
import java.awt.image.BufferedImage
import java.io.File
import java.io.IOException

object ImageFormatDetector {

    private val log = Logger.getInstance(ImageFormatDetector::class.java)

    data class ImageData(
        val image: BufferedImage,
        val format: String,
        val sourceName: String?
    )

    /**
     * 剪贴板里的 imageFlavor 统一存为 PNG。如需其它格式，可在调用方扩展。
     */
    fun detect(transferable: Transferable?): ImageData? {
        if (transferable == null) return null
        return try {
            val image = transferable.getTransferData(DataFlavor.imageFlavor) as? Image
            if (image != null) {
                ImageData(toBufferedImage(image), "png", detectSourceName(transferable))
            } else {
                null
            }
        } catch (_: UnsupportedFlavorException) {
            null
        } catch (_: IOException) {
            null
        } catch (e: Exception) {
            log.warn("Unexpected error detecting clipboard image", e)
            null
        }
    }

    private fun toBufferedImage(image: Image): BufferedImage {
        if (image is BufferedImage) return image

        val width = image.getWidth(null)
        val height = image.getHeight(null)
        if (width <= 0 || height <= 0) throw IOException("Clipboard image dimensions are unavailable")

        val buffered = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
        val graphics = buffered.createGraphics()
        try {
            graphics.drawImage(image, 0, 0, null)
        } finally {
            graphics.dispose()
        }
        return buffered
    }

    private fun detectSourceName(transferable: Transferable): String? {
        if (!transferable.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) return null

        val files = try {
            @Suppress("UNCHECKED_CAST")
            transferable.getTransferData(DataFlavor.javaFileListFlavor) as? List<File>
        } catch (_: UnsupportedFlavorException) {
            null
        } catch (_: IOException) {
            null
        }

        val name = files?.firstOrNull()?.nameWithoutExtension
        return name?.takeIf { it.isNotBlank() }
    }
}
