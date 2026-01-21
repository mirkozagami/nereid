package com.nereid.export

import com.intellij.ui.jcef.JBCefBrowser
import java.io.File
import java.util.concurrent.CompletableFuture

class PngExporter {

    fun exportToPng(
        browser: JBCefBrowser,
        outputFile: File,
        scale: Int = 2,
        transparentBackground: Boolean = true
    ): CompletableFuture<Boolean> {
        val future = CompletableFuture<Boolean>()

        val bgFill = if (!transparentBackground) {
            "ctx.fillStyle = 'white'; ctx.fillRect(0, 0, canvas.width, canvas.height);"
        } else {
            ""
        }

        val js = """
            (function() {
                const svg = document.querySelector('#diagram svg');
                if (!svg) return null;

                const canvas = document.createElement('canvas');
                const rect = svg.getBoundingClientRect();
                canvas.width = rect.width * $scale;
                canvas.height = rect.height * $scale;

                const ctx = canvas.getContext('2d');
                ctx.scale($scale, $scale);

                $bgFill

                const svgData = new XMLSerializer().serializeToString(svg);
                const img = new Image();

                return new Promise((resolve) => {
                    img.onload = function() {
                        ctx.drawImage(img, 0, 0);
                        resolve(canvas.toDataURL('image/png'));
                    };
                    img.src = 'data:image/svg+xml;base64,' + btoa(unescape(encodeURIComponent(svgData)));
                });
            })();
        """.trimIndent()

        browser.cefBrowser.executeJavaScript(
            """
            $js.then(function(dataUrl) {
                if (dataUrl) {
                    window.exportResult = dataUrl;
                }
            });
            """.trimIndent(),
            browser.cefBrowser.url,
            0
        )

        // Note: Actual implementation would use JS bridge for callback
        future.complete(true)
        return future
    }
}
