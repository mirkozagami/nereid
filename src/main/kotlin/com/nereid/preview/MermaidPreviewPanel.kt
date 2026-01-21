package com.nereid.preview

import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Disposer
import com.intellij.ui.jcef.JBCefBrowser
import com.intellij.ui.jcef.JBCefBrowserBase
import com.intellij.ui.jcef.JBCefJSQuery
import org.cef.browser.CefBrowser
import org.cef.browser.CefFrame
import org.cef.handler.CefLoadHandlerAdapter
import java.awt.BorderLayout
import javax.swing.JComponent
import javax.swing.JPanel

class MermaidPreviewPanel(parentDisposable: Disposable) : Disposable {

    private val browser: JBCefBrowser
    private val panel: JPanel
    private val jsQuery: JBCefJSQuery

    private var pendingSource: String? = null
    private var pendingTheme: String = "default"
    private var isLoaded = false
    private var textSource: String? = null
    private val themeManager: ThemeManager

    var onRenderSuccess: (() -> Unit)? = null
    var onRenderError: ((String) -> Unit)? = null
    var onZoomChanged: ((Double) -> Unit)? = null

    init {
        browser = JBCefBrowser()
        panel = JPanel(BorderLayout())
        panel.add(browser.component, BorderLayout.CENTER)

        jsQuery = JBCefJSQuery.create(browser as JBCefBrowserBase)

        setupJsBridge()
        loadPreviewHtml()

        Disposer.register(parentDisposable, this)

        themeManager = ThemeManager(this)
        themeManager.onThemeChanged = { isDark, mermaidTheme ->
            setDarkMode(isDark)
            pendingSource?.let { renderDiagram(it, mermaidTheme) }
                ?: textSource?.let { renderDiagram(it, mermaidTheme) }
        }
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
            }
            null
        }

        browser.jbCefClient.addLoadHandler(object : CefLoadHandlerAdapter() {
            override fun onLoadEnd(browser: CefBrowser?, frame: CefFrame?, httpStatusCode: Int) {
                if (frame?.isMain == true) {
                    injectJavaBridge()
                    isLoaded = true
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
                    }
                    .hidden { display: none !important; }
                    body.dark { --background-color: #1e1e1e; }
                    body.dark #error {
                        background: #3a1a1a;
                        border-color: #5a2a2a;
                        color: #faa;
                    }
                </style>
                <script src="https://cdn.jsdelivr.net/npm/mermaid@10/dist/mermaid.min.js"></script>
            </head>
            <body>
                <div id="container">
                    <div id="diagram"></div>
                    <div id="error" class="hidden"></div>
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
                            error.textContent = e.message || 'Failed to render diagram';
                            error.classList.remove('hidden');

                            if (window.javaBridge) {
                                window.javaBridge.onRenderError(e.message || 'Unknown error');
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

                        const containerRect = document.getElementById('container').getBoundingClientRect();
                        const svgRect = svg.getBoundingClientRect();

                        const scaleX = containerRect.width / svgRect.width;
                        const scaleY = containerRect.height / svgRect.height;
                        currentZoom = Math.min(scaleX, scaleY, 1) * 0.9;
                        panX = 0;
                        panY = 0;
                        applyTransform();
                    };

                    function applyTransform() {
                        const diagram = document.getElementById('diagram');
                        diagram.style.transform = 'translate(' + panX + 'px, ' + panY + 'px) scale(' + currentZoom + ')';
                    }

                    document.addEventListener('wheel', function(e) {
                        e.preventDefault();
                        // Calculate zoom factor based on scroll amount
                        const scrollAmount = Math.abs(e.deltaY);
                        const zoomIntensity = 0.002;
                        const delta = e.deltaY > 0
                            ? 1 / (1 + scrollAmount * zoomIntensity)
                            : 1 + scrollAmount * zoomIntensity;
                        currentZoom = Math.max(0.05, Math.min(10, currentZoom * delta));
                        applyTransform();

                        if (window.javaBridge) {
                            window.javaBridge.onZoomChanged(currentZoom);
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

    val component: JComponent get() = panel

    override fun dispose() {
        Disposer.dispose(jsQuery)
        Disposer.dispose(browser)
    }
}
