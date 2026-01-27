package com.nereid.preview

import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Disposer
import com.intellij.ui.jcef.JBCefBrowser
import com.intellij.ui.jcef.JBCefBrowserBase
import com.intellij.ui.jcef.JBCefJSQuery
import org.cef.browser.CefBrowser
import org.cef.browser.CefFrame
import org.cef.callback.CefContextMenuParams
import org.cef.callback.CefMenuModel
import org.cef.handler.CefContextMenuHandlerAdapter
import org.cef.handler.CefLoadHandlerAdapter
import java.awt.BorderLayout
import javax.swing.JComponent
import javax.swing.JPanel

class MermaidPreviewPanel(parentDisposable: Disposable) : Disposable {

    private val browser: JBCefBrowser
    private val panel: JPanel
    private val jsQuery: JBCefJSQuery
    private val pngExportQuery: JBCefJSQuery
    private val svgExportQuery: JBCefJSQuery

    private var pendingSource: String? = null
    private var pendingTheme: String = "default"
    private var isLoaded = false
    private var textSource: String? = null
    private val themeManager: ThemeManager

    private var pngExportCallback: ((String) -> Unit)? = null
    private var svgExportCallback: ((String) -> Unit)? = null

    var onRenderSuccess: (() -> Unit)? = null
    var onRenderError: ((String) -> Unit)? = null
    var onZoomChanged: ((Double) -> Unit)? = null
    var onReportIssue: (() -> Unit)? = null

    // Context menu callbacks
    var onExportPng: (() -> Unit)? = null
    var onExportSvg: (() -> Unit)? = null
    var onCopyPng: (() -> Unit)? = null

    companion object {
        private const val MENU_ID_EXPORT_PNG = 26501
        private const val MENU_ID_EXPORT_SVG = 26502
        private const val MENU_ID_COPY_PNG = 26503
    }

    init {
        browser = JBCefBrowser()
        panel = JPanel(BorderLayout())
        panel.add(browser.component, BorderLayout.CENTER)

        jsQuery = JBCefJSQuery.create(browser as JBCefBrowserBase)
        pngExportQuery = JBCefJSQuery.create(browser as JBCefBrowserBase)
        svgExportQuery = JBCefJSQuery.create(browser as JBCefBrowserBase)

        setupJsBridge()
        setupExportQueries()
        setupContextMenu()
        loadPreviewHtml()

        Disposer.register(parentDisposable, this)

        themeManager = ThemeManager(this)
        themeManager.onThemeChanged = { isDark, mermaidTheme, background ->
            setDarkMode(isDark)
            setBackground(background)
            pendingSource?.let { renderDiagram(it, mermaidTheme) }
                ?: textSource?.let { renderDiagram(it, mermaidTheme) }
        }
    }

    fun applySettings() {
        themeManager.applyCurrentSettings()
    }

    private fun setupExportQueries() {
        pngExportQuery.addHandler { dataUrl ->
            pngExportCallback?.invoke(dataUrl)
            pngExportCallback = null
            null
        }

        svgExportQuery.addHandler { svgContent ->
            svgExportCallback?.invoke(svgContent)
            svgExportCallback = null
            null
        }
    }

    private fun setupContextMenu() {
        browser.jbCefClient.addContextMenuHandler(object : CefContextMenuHandlerAdapter() {
            override fun onBeforeContextMenu(
                browser: CefBrowser?,
                frame: CefFrame?,
                params: CefContextMenuParams?,
                model: CefMenuModel?
            ) {
                model?.clear()
                model?.addItem(MENU_ID_EXPORT_PNG, "Export as PNG...")
                model?.addItem(MENU_ID_EXPORT_SVG, "Export as SVG...")
                model?.addSeparator()
                model?.addItem(MENU_ID_COPY_PNG, "Copy as PNG")
            }

            override fun onContextMenuCommand(
                browser: CefBrowser?,
                frame: CefFrame?,
                params: CefContextMenuParams?,
                commandId: Int,
                eventFlags: Int
            ): Boolean {
                when (commandId) {
                    MENU_ID_EXPORT_PNG -> onExportPng?.invoke()
                    MENU_ID_EXPORT_SVG -> onExportSvg?.invoke()
                    MENU_ID_COPY_PNG -> onCopyPng?.invoke()
                    else -> return false
                }
                return true
            }
        }, browser.cefBrowser)
    }

    private fun setupJsBridge() {
        jsQuery.addHandler { result ->
            when {
                result.startsWith("success:") -> onRenderSuccess?.invoke()
                result.startsWith("error:") -> onRenderError?.invoke(result.removePrefix("error:"))
                result.startsWith("zoom:") -> {
                    val zoom = result.removePrefix("zoom:").toDoubleOrNull()
                    if (zoom != null) onZoomChanged?.invoke(zoom)
                }
                result == "report" -> onReportIssue?.invoke()
            }
            null
        }

        browser.jbCefClient.addLoadHandler(object : CefLoadHandlerAdapter() {
            override fun onLoadEnd(browser: CefBrowser?, frame: CefFrame?, httpStatusCode: Int) {
                if (frame?.isMain == true) {
                    injectJavaBridge()
                    isLoaded = true
                    // Apply theme and background settings immediately
                    applySettings()
                    pendingSource?.let { renderDiagram(it, pendingTheme) }
                    pendingSource = null
                }
            }
        }, browser.cefBrowser)
    }

    private fun injectJavaBridge() {
        val js = """
            window.javaBridge = {
                onRenderSuccess: function() {
                    ${jsQuery.inject("'success:'")}
                },
                onRenderError: function(msg) {
                    ${jsQuery.inject("'error:' + msg")}
                },
                onZoomChanged: function(zoom) {
                    ${jsQuery.inject("'zoom:' + zoom")}
                },
                onPngExport: function(dataUrl) {
                    ${pngExportQuery.inject("dataUrl")}
                },
                onSvgExport: function(svgContent) {
                    ${svgExportQuery.inject("svgContent")}
                },
                onReportIssue: function() {
                    ${jsQuery.inject("'report'")}
                }
            };
        """.trimIndent()
        browser.cefBrowser.executeJavaScript(js, browser.cefBrowser.url, 0)
    }

    private fun loadPreviewHtml() {
        val html = """
            <!DOCTYPE html>
            <html lang="en">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Mermaid Preview</title>
                <style>
                    * { margin: 0; padding: 0; box-sizing: border-box; }
                    body {
                        font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
                        overflow: hidden;
                        cursor: grab;
                    }
                    #container {
                        width: 100vw;
                        height: 100vh;
                        display: flex;
                        align-items: center;
                        justify-content: center;
                        background: var(--background-color, #ffffff);
                    }
                    #diagram {
                        transform-origin: center center;
                        transition: transform 0.1s ease-out;
                    }
                    #diagram svg { max-width: none; }
                    #error {
                        position: absolute;
                        bottom: 20px;
                        left: 20px;
                        right: 20px;
                        padding: 12px 16px;
                        background: #fee;
                        border: 1px solid #fcc;
                        border-radius: 4px;
                        color: #c00;
                        font-size: 13px;
                        display: flex;
                        flex-direction: column;
                        gap: 8px;
                    }
                    #error-message { flex: 1; }
                    #report-link { font-size: 12px; color: #666; text-align: right; }
                    #report-link:hover { color: #333; }
                    .hidden { display: none !important; }
                    body.dark { --background-color: #1e1e1e; }
                    body.dark #error {
                        background: #3a1a1a;
                        border-color: #5a2a2a;
                        color: #faa;
                    }
                    body.dark #report-link { color: #aaa; }
                    body.dark #report-link:hover { color: #fff; }
                </style>
                <script src="https://cdn.jsdelivr.net/npm/mermaid@11/dist/mermaid.min.js"></script>
            </head>
            <body>
                <div id="container">
                    <div id="diagram"></div>
                    <div id="error" class="hidden">
                        <span id="error-message"></span>
                        <a href="#" id="report-link" onclick="reportIssue(); return false;">Report this issue</a>
                    </div>
                </div>
                <script>
                    let currentZoom = 1;
                    let panX = 0;
                    let panY = 0;
                    let isDragging = false;
                    let lastX, lastY;

                    mermaid.initialize({
                        startOnLoad: false,
                        theme: 'default',
                        securityLevel: 'strict'
                    });

                    window.renderDiagram = async function(source, theme) {
                        const diagram = document.getElementById('diagram');
                        const error = document.getElementById('error');

                        try {
                            if (theme) {
                                mermaid.initialize({ theme: theme, securityLevel: 'strict' });
                            }

                            const { svg } = await mermaid.render('mermaid-diagram', source);
                            diagram.innerHTML = svg;
                            diagram.classList.remove('hidden');
                            error.classList.add('hidden');

                            applyTransform();

                            if (window.javaBridge) {
                                window.javaBridge.onRenderSuccess();
                            }
                        } catch (e) {
                            const errorMessage = e.message || 'Failed to render diagram';
                            document.getElementById('error-message').textContent = errorMessage;
                            error.classList.remove('hidden');

                            if (window.javaBridge) {
                                window.javaBridge.onRenderError(errorMessage);
                            }
                        }
                    };

                    window.setZoom = function(zoom) {
                        currentZoom = zoom;
                        applyTransform();
                    };

                    window.resetView = function() {
                        currentZoom = 1;
                        panX = 0;
                        panY = 0;
                        applyTransform();
                    };

                    window.fitToView = function() {
                        const diagram = document.getElementById('diagram');
                        const svg = diagram.querySelector('svg');
                        if (!svg) return;

                        // Reset transform first to get accurate natural dimensions
                        currentZoom = 1;
                        panX = 0;
                        panY = 0;
                        applyTransform();

                        // Wait for layout to settle, then calculate fit
                        requestAnimationFrame(function() {
                            const containerRect = document.getElementById('container').getBoundingClientRect();
                            const svgRect = svg.getBoundingClientRect();

                            // Add small padding (5%) around the diagram
                            const padding = 0.95;
                            const scaleX = (containerRect.width * padding) / svgRect.width;
                            const scaleY = (containerRect.height * padding) / svgRect.height;

                            // Use the smaller scale to fit both dimensions, allow zoom > 1 for small diagrams
                            currentZoom = Math.min(scaleX, scaleY);
                            panX = 0;
                            panY = 0;
                            applyTransform();

                            if (window.javaBridge) {
                                window.javaBridge.onZoomChanged(currentZoom);
                            }
                        });
                    };

                    function applyTransform() {
                        const diagram = document.getElementById('diagram');
                        diagram.style.transform = 'translate(' + panX + 'px, ' + panY + 'px) scale(' + currentZoom + ')';
                    }

                    window.zoomIn = function() {
                        currentZoom = Math.min(10, currentZoom * 1.25);
                        applyTransform();
                        if (window.javaBridge) {
                            window.javaBridge.onZoomChanged(currentZoom);
                        }
                    };

                    window.zoomOut = function() {
                        currentZoom = Math.max(0.05, currentZoom / 1.25);
                        applyTransform();
                        if (window.javaBridge) {
                            window.javaBridge.onZoomChanged(currentZoom);
                        }
                    };

                    document.addEventListener('wheel', function(e) {
                        if (e.ctrlKey || e.metaKey) {
                            e.preventDefault();
                            const scrollAmount = Math.abs(e.deltaY);
                            const zoomIntensity = 0.003;
                            const delta = e.deltaY > 0
                                ? 1 / (1 + scrollAmount * zoomIntensity)
                                : 1 + scrollAmount * zoomIntensity;
                            currentZoom = Math.max(0.05, Math.min(10, currentZoom * delta));
                            applyTransform();

                            if (window.javaBridge) {
                                window.javaBridge.onZoomChanged(currentZoom);
                            }
                        }
                    }, { passive: false });

                    document.addEventListener('mousedown', function(e) {
                        if (e.button === 0) {
                            isDragging = true;
                            lastX = e.clientX;
                            lastY = e.clientY;
                            document.body.style.cursor = 'grabbing';
                        }
                    });

                    document.addEventListener('mousemove', function(e) {
                        if (isDragging) {
                            panX += e.clientX - lastX;
                            panY += e.clientY - lastY;
                            lastX = e.clientX;
                            lastY = e.clientY;
                            applyTransform();
                        }
                    });

                    document.addEventListener('mouseup', function() {
                        isDragging = false;
                        document.body.style.cursor = 'grab';
                    });

                    window.exportAsPng = function() {
                        const svg = document.querySelector('#diagram svg');
                        if (!svg) {
                            if (window.javaBridge) window.javaBridge.onPngExport('');
                            return;
                        }

                        // Get SVG dimensions from attributes or viewBox
                        let width, height;
                        const widthAttr = svg.getAttribute('width');
                        const heightAttr = svg.getAttribute('height');

                        if (widthAttr && heightAttr) {
                            width = parseFloat(widthAttr);
                            height = parseFloat(heightAttr);
                        }

                        // Fallback to viewBox
                        if (!width || !height || isNaN(width) || isNaN(height)) {
                            const viewBox = svg.getAttribute('viewBox');
                            if (viewBox) {
                                const parts = viewBox.split(/[\s,]+/);
                                if (parts.length >= 4) {
                                    width = parseFloat(parts[2]);
                                    height = parseFloat(parts[3]);
                                }
                            }
                        }

                        // Final fallback to getBBox
                        if (!width || !height || isNaN(width) || isNaN(height)) {
                            const bbox = svg.getBBox();
                            width = bbox.width;
                            height = bbox.height;
                        }

                        // Clone SVG and ensure it has explicit dimensions
                        const svgClone = svg.cloneNode(true);
                        svgClone.setAttribute('width', width);
                        svgClone.setAttribute('height', height);

                        const svgData = new XMLSerializer().serializeToString(svgClone);
                        const svgBlob = new Blob([svgData], { type: 'image/svg+xml;charset=utf-8' });
                        const url = URL.createObjectURL(svgBlob);

                        const img = new Image();
                        img.onload = function() {
                            const canvas = document.createElement('canvas');
                            const scale = 2; // Higher resolution
                            canvas.width = width * scale;
                            canvas.height = height * scale;
                            const ctx = canvas.getContext('2d');
                            ctx.fillStyle = getComputedStyle(document.getElementById('container')).backgroundColor || '#ffffff';
                            ctx.fillRect(0, 0, canvas.width, canvas.height);
                            ctx.scale(scale, scale);
                            ctx.drawImage(img, 0, 0, width, height);
                            URL.revokeObjectURL(url);

                            const dataUrl = canvas.toDataURL('image/png');
                            if (window.javaBridge) {
                                window.javaBridge.onPngExport(dataUrl);
                            }
                        };
                        img.onerror = function() {
                            URL.revokeObjectURL(url);
                            if (window.javaBridge) window.javaBridge.onPngExport('');
                        };
                        img.src = url;
                    };

                    window.exportAsSvg = function() {
                        const svg = document.querySelector('#diagram svg');
                        if (!svg) {
                            if (window.javaBridge) window.javaBridge.onSvgExport('');
                            return;
                        }

                        const svgData = new XMLSerializer().serializeToString(svg);
                        if (window.javaBridge) {
                            window.javaBridge.onSvgExport(svgData);
                        }
                    };

                    function reportIssue() {
                        if (window.javaBridge) {
                            window.javaBridge.onReportIssue();
                        }
                    }
                </script>
            </body>
            </html>
        """.trimIndent()

        browser.loadHTML(html)
    }

    fun renderDiagram(source: String, theme: String? = null) {
        textSource = source
        val actualTheme = theme ?: themeManager.getMermaidTheme()

        if (!isLoaded) {
            pendingSource = source
            pendingTheme = actualTheme
            return
        }

        val escapedSource = source
            .replace("\\", "\\\\")
            .replace("`", "\\`")
            .replace("\$", "\\$")
            .replace("\n", "\\n")

        val js = "window.renderDiagram(`$escapedSource`, '$actualTheme');"
        browser.cefBrowser.executeJavaScript(js, browser.cefBrowser.url, 0)
    }

    fun setZoom(zoom: Double) {
        if (isLoaded) {
            browser.cefBrowser.executeJavaScript("window.setZoom($zoom);", browser.cefBrowser.url, 0)
        }
    }

    fun zoomIn() {
        if (isLoaded) {
            browser.cefBrowser.executeJavaScript("window.zoomIn();", browser.cefBrowser.url, 0)
        }
    }

    fun zoomOut() {
        if (isLoaded) {
            browser.cefBrowser.executeJavaScript("window.zoomOut();", browser.cefBrowser.url, 0)
        }
    }

    fun resetView() {
        if (isLoaded) {
            browser.cefBrowser.executeJavaScript("window.resetView();", browser.cefBrowser.url, 0)
        }
    }

    fun fitToView() {
        if (isLoaded) {
            browser.cefBrowser.executeJavaScript("window.fitToView();", browser.cefBrowser.url, 0)
        }
    }

    fun exportAsPng(callback: (String) -> Unit) {
        if (isLoaded) {
            pngExportCallback = callback
            browser.cefBrowser.executeJavaScript("window.exportAsPng();", browser.cefBrowser.url, 0)
        } else {
            callback("")
        }
    }

    fun exportAsSvg(callback: (String) -> Unit) {
        if (isLoaded) {
            svgExportCallback = callback
            browser.cefBrowser.executeJavaScript("window.exportAsSvg();", browser.cefBrowser.url, 0)
        } else {
            callback("")
        }
    }

    fun setDarkMode(dark: Boolean) {
        if (isLoaded) {
            val js = if (dark) {
                "document.body.classList.add('dark');"
            } else {
                "document.body.classList.remove('dark');"
            }
            browser.cefBrowser.executeJavaScript(js, browser.cefBrowser.url, 0)
        }
    }

    fun setBackground(color: String) {
        if (isLoaded) {
            val js = "document.getElementById('container').style.background = '$color';"
            browser.cefBrowser.executeJavaScript(js, browser.cefBrowser.url, 0)
        }
    }

    val component: JComponent get() = panel

    override fun dispose() {
        Disposer.dispose(jsQuery)
        Disposer.dispose(pngExportQuery)
        Disposer.dispose(svgExportQuery)
        Disposer.dispose(browser)
    }
}
