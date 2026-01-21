package com.nereid.export

import com.intellij.ui.jcef.JBCefBrowser
import java.io.File
import java.util.concurrent.CompletableFuture

class SvgExporter {

    fun exportToSvg(
        browser: JBCefBrowser,
        outputFile: File,
        embedFonts: Boolean = true
    ): CompletableFuture<Boolean> {
        val future = CompletableFuture<Boolean>()

        val js = """
            (function() {
                const svg = document.querySelector('#diagram svg');
                if (!svg) return null;

                const clone = svg.cloneNode(true);

                // Clean up unnecessary attributes
                clone.removeAttribute('style');
                clone.setAttribute('xmlns', 'http://www.w3.org/2000/svg');

                return new XMLSerializer().serializeToString(clone);
            })();
        """.trimIndent()

        browser.cefBrowser.executeJavaScript(
            """
            const svgContent = $js;
            if (svgContent) {
                window.exportSvgResult = svgContent;
            }
            """.trimIndent(),
            browser.cefBrowser.url,
            0
        )

        future.complete(true)
        return future
    }
}
