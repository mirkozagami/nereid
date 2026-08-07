package com.nereid.preview

import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Disposer
import com.intellij.ui.jcef.JBCefBrowser
import com.intellij.ui.jcef.JBCefBrowserBase
import com.intellij.ui.jcef.JBCefJSQuery
import com.nereid.settings.MermaidSettings
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

        internal const val PREVIEW_TEMPLATE_RESOURCE = "/mermaid/preview.html"

        /** Replaced with the bundled Mermaid source before the page is loaded. */
        internal const val MERMAID_LIBRARY_PLACEHOLDER = "__MERMAID_LIBRARY__"

        /**
         * Escapes text for embedding in a JavaScript template literal.
         *
         * Both the diagram source and the user's custom CSS are passed to the page this
         * way, so the rules live in one place rather than being duplicated per call site.
         *
         * `$` needs escaping as much as the backtick does: an unescaped `${...}` would be
         * evaluated as interpolation rather than treated as text.
         */
        internal fun escapeForTemplateLiteral(text: String): String =
            text
                .replace("\\", "\\\\")
                .replace("`", "\\`")
                .replace("\$", "\\$")
                .replace("\n", "\\n")
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
        applyCustomCss(MermaidSettings.getInstance().customCss)
    }

    /**
     * Pushes the user's custom CSS into the preview document.
     *
     * Applied unconditionally, including when blank: clearing the setting has to clear
     * the rules, so skipping empty input would strand whatever was injected last.
     *
     * The CSS is escaped for the JS template literal it is embedded in -- the same
     * treatment [renderDiagram] gives diagram source. `$` matters as much as the
     * backtick here, since `${...}` would otherwise interpolate.
     */
    fun applyCustomCss(css: String) {
        if (!isLoaded) return

        browser.cefBrowser.executeJavaScript(
            "window.applyCustomCss(`${escapeForTemplateLiteral(css)}`);",
            browser.cefBrowser.url,
            0
        )
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

    /**
     * Loads the preview page from `preview.html`, substituting the bundled Mermaid
     * library into it.
     *
     * The page lives in a real .html resource rather than a Kotlin string so its ~265
     * lines of JavaScript get syntax highlighting, inspection and template literals --
     * inside a Kotlin raw string, `${'$'}{...}` collides with Kotlin interpolation, which is
     * why the page previously had to build strings by concatenation.
     *
     * The library is substituted rather than referenced with a relative <script src>
     * because loadHTML() gives the document an opaque origin with nothing to resolve
     * relative URLs against. See MermaidBundle.
     */
    private fun loadPreviewHtml() {
        val template = readPreviewTemplate()
        if (template.isEmpty()) {
            onRenderError?.invoke("Preview template could not be loaded from the plugin resources")
            return
        }
        browser.loadHTML(template.replace(MERMAID_LIBRARY_PLACEHOLDER, MermaidBundle.script))
    }

    private fun readPreviewTemplate(): String =
        try {
            MermaidPreviewPanel::class.java.getResourceAsStream(PREVIEW_TEMPLATE_RESOURCE)
                ?.bufferedReader()
                ?.use { it.readText() }
                ?: ""
        } catch (e: Exception) {
            ""
        }

    fun renderDiagram(source: String, theme: String? = null) {
        textSource = source
        val actualTheme = theme ?: themeManager.getMermaidTheme()

        if (!isLoaded) {
            pendingSource = source
            pendingTheme = actualTheme
            return
        }

        val js = "window.renderDiagram(`${escapeForTemplateLiteral(source)}`, '$actualTheme');"
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
            val settings = MermaidSettings.getInstance()
            // Read at export time so changing the settings takes effect immediately.
            val scale = settings.pngScaleFactor.coerceIn(1, 8)
            val transparent = settings.pngTransparentBackground
            browser.cefBrowser.executeJavaScript(
                "window.exportAsPng($scale, $transparent);",
                browser.cefBrowser.url,
                0
            )
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
