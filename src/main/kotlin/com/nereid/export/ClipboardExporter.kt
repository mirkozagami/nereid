package com.nereid.export

import java.awt.Image
import java.awt.Toolkit
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.StringSelection
import java.awt.datatransfer.Transferable
import java.awt.datatransfer.UnsupportedFlavorException
import java.awt.image.BufferedImage

class ClipboardExporter {

    fun copyImageToClipboard(image: BufferedImage) {
        val transferable = ImageTransferable(image)
        Toolkit.getDefaultToolkit().systemClipboard.setContents(transferable, null)
    }

    fun copySvgToClipboard(svgContent: String) {
        val selection = StringSelection(svgContent)
        Toolkit.getDefaultToolkit().systemClipboard.setContents(selection, null)
    }

    fun copySourceToClipboard(source: String) {
        val selection = StringSelection(source)
        Toolkit.getDefaultToolkit().systemClipboard.setContents(selection, null)
    }

    private class ImageTransferable(private val image: Image) : Transferable {
        override fun getTransferDataFlavors(): Array<DataFlavor> = arrayOf(DataFlavor.imageFlavor)

        override fun isDataFlavorSupported(flavor: DataFlavor): Boolean =
            DataFlavor.imageFlavor.equals(flavor)

        override fun getTransferData(flavor: DataFlavor): Any {
            if (!DataFlavor.imageFlavor.equals(flavor)) {
                throw UnsupportedFlavorException(flavor)
            }
            return image
        }
    }
}
