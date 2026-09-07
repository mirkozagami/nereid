package com.nereid.preview

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.util.Disposer
import com.intellij.ui.jcef.JBCefBrowser
import com.intellij.ui.jcef.JBCefBrowserBase
import com.intellij.ui.jcef.JBCefJSQuery
import com.nereid.settings.MermaidSettings
import com.nereid.settings.MermaidSettingsListener
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
    // One-shot per page load. The configured zoom applies to the first render only;
    // re-applying on every render would refit on each keystroke and undo any zoom the
    // user had just set by hand.
    private var initialZoomApplied = false
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
         * Replaced with the user's [MermaidSettings.SecurityMode] before the page loads.
         *
         * The template must contain this exactly once: both `mermaid.initialize` calls
         * read the single variable it becomes, so the initial call and the one on every
         * theme change cannot drift apart the way they had in #44.
         */
        internal const val SECURITY_LEVEL_PLACEHOLDER = "__SECURITY_LEVEL__"

        /**
         * Replaced with the user's `mouseWheelZoomEnabled` before the page loads.
         *
         * Substituted rather than pushed after load so the very first wheel event obeys
         * the setting; [applyMouseWheelZoom] handles changes made while a preview is open.
         */
        internal const val MOUSE_WHEEL_ZOOM_PLACEHOLDER = "__MOUSE_WHEEL_ZOOM__"

        /**
         * Builds the page actually handed to the browser.
         *
         * Separate from [loadPreviewHtml] so the substitution is reachable without a
         * JCEF browser: none of this is visible to the Plugin Verifier, and a dropped
         * substitution leaves either a blank preview or an invalid security level.
         */
        internal fun buildPreviewHtml(
            template: String,
            securityLevel: String,
            mouseWheelZoom: Boolean,
        ): String =
            template
                .replace(MERMAID_LIBRARY_PLACEHOLDER, MermaidBundle.script)
                .replace(SECURITY_LEVEL_PLACEHOLDER, securityLevel)
                .replace(MOUSE_WHEEL_ZOOM_PLACEHOLDER, mouseWheelZoom.toString())

        /** The only payload shape [exportAsPng] is able to produce. */
        internal const val PNG_DATA_URL_PREFIX = "data:image/png;base64,"

        /**
         * Stands in for a payload that did not come from [exportAsPng].
         *
         * Phrased as the message `reportPngFailure` already produced for an unrecognised
         * payload, so hardening the boundary does not change what the user is told when
         * an export genuinely fails.
         */
        internal const val PNG_EXPORT_REJECTED = "error:unexpected image data format"

        private val BASE64_BODY = Regex("""[A-Za-z0-9+/]+={0,2}""")

        /**
         * Vets a PNG export payload before it reaches the pending export callback.
         *
         * The payload crosses from page script, and under `loose` that is not
         * necessarily *our* page script: `click ... href "javascript:..."` lets diagram
         * source call `javaBridge.onPngExport` itself, and whatever it passes would be
         * written to the file the user chose in the save dialog (#52).
         *
         * Both export call sites tested this prefix at the far end already. Checking here
         * instead means one place decides what a PNG export may contain, and a third
         * caller cannot forget to.
         *
         * The prefix alone is not sufficient -- it is just as attacker-chosen as the rest
         * -- so the body has to be base64 too. Anything else is turned into an `error:`
         * report rather than dropped, because a silently discarded export is the failure
         * mode this codebase keeps producing.
         */
        internal fun sanitizePngExportPayload(payload: String): String = when {
            // exportAsPng() calls back with "" when the browser is not loaded, and
            // reportPngFailure has its own message for that.
            payload.isEmpty() -> payload
            payload.startsWith("error:") -> payload
            payload.startsWith(PNG_DATA_URL_PREFIX) &&
                BASE64_BODY.matches(payload.removePrefix(PNG_DATA_URL_PREFIX)) -> payload
            else -> PNG_EXPORT_REJECTED
        }

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

        // Every open preview reacts to a settings change, not just the one whose toolbar
        // was used. The connection is tied to this panel, so it goes away with it.
        ApplicationManager.getApplication().messageBus
            .connect(this)
            .subscribe(
                MermaidSettingsListener.TOPIC,
                object : MermaidSettingsListener {
                    override fun settingsChanged() = applySettings()
                }
            )

        themeManager = ThemeManager(this)
        themeManager.onThemeChanged = { isDark, mermaidTheme, background ->
            setDarkMode(isDark)
            setBackground(background)
            pendingSource?.let { renderDiagram(it, mermaidTheme) }
                ?: textSource?.let { renderDiagram(it, mermaidTheme) }
        }
    }

    fun applySettings() {
        // Order matters. applyCurrentSettings() re-renders the diagram, and the render
        // uses whatever security level the page currently holds -- so the level has to be
        // updated first, or the user's change only takes effect on the *next* render and
        // the setting looks inert all over again (#44).
        applySecurityLevel(MermaidSettings.getInstance().securityMode)
        applyMouseWheelZoom(MermaidSettings.getInstance().mouseWheelZoomEnabled)
        themeManager.applyCurrentSettings()
        applyCustomCss(MermaidSettings.getInstance().customCss)
    }

    /**
     * Applies the user's `defaultZoomLevel` to the first render after a page load.
     *
     * Runs after a render rather than on load because fitting measures the rendered SVG,
     * which does not exist until a diagram has been drawn. Nothing read the setting at
     * all before this; every preview fitted to view because that is what the page
     * happened to do (#39).
     */
    private fun applyInitialZoom() {
        if (initialZoomApplied) return
        initialZoomApplied = true

        val settings = MermaidSettings.getInstance()
        when (val zoom = initialZoomFor(settings.defaultZoomLevel, settings.lastZoom)) {
            is InitialZoom.FitToView -> fitToView()
            is InitialZoom.Factor -> setZoom(zoom.value)
        }
    }

    /**
     * Pushes the mouse wheel zoom setting into an already-loaded preview, so unticking
     * the box stops the next wheel event rather than the next time the file is opened.
     */
    fun applyMouseWheelZoom(enabled: Boolean) {
        if (!isLoaded) return

        browser.cefBrowser.executeJavaScript(
            "window.setMouseWheelZoom($enabled);",
            browser.cefBrowser.url,
            0
        )
    }

    /**
     * Pushes the security level into an already-loaded preview.
     *
     * The level is substituted into the page at load time, so without this a change
     * would not take effect until the file was reopened -- the failure mode #39 was
     * about, and a poor one for a security control in particular.
     */
    fun applySecurityLevel(mode: MermaidSettings.SecurityMode) {
        if (!isLoaded) return

        browser.cefBrowser.executeJavaScript(
            "window.setSecurityLevel('${mode.mermaidValue}');",
            browser.cefBrowser.url,
            0
        )
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
            // Vetted at the boundary rather than trusted: see sanitizePngExportPayload.
            pngExportCallback?.invoke(sanitizePngExportPayload(dataUrl))
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
                result.startsWith("success:") -> {
                    // After the render, not on load: fitToView() measures the rendered
                    // SVG, which does not exist until a diagram has been drawn.
                    applyInitialZoom()
                    onRenderSuccess?.invoke()
                }
                result.startsWith("error:") -> onRenderError?.invoke(result.removePrefix("error:"))
                result.startsWith("zoom:") -> {
                    val zoom = result.removePrefix("zoom:").toDoubleOrNull()
                    if (zoom != null) {
                        // Recorded unconditionally, not only under LAST_USED, so switching
                        // to Last Used later restores a real value rather than the default.
                        MermaidSettings.getInstance().lastZoom = zoom
                        onZoomChanged?.invoke(zoom)
                    }
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
                    initialZoomApplied = false
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
        browser.loadHTML(
            buildPreviewHtml(
                template,
                MermaidSettings.getInstance().securityMode.mermaidValue,
                MermaidSettings.getInstance().mouseWheelZoomEnabled,
            )
        )
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
