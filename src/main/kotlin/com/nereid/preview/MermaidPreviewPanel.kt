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
        val htmlUrl = javaClass.getResource("/mermaid/preview.html")
        if (htmlUrl != null) {
            browser.loadURL(htmlUrl.toExternalForm())
        }
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
