package com.nereid.markdown

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.util.xmlb.XmlSerializerUtil
import com.nereid.settings.MermaidSettings

/**
 * The Markdown preview's half of #44.
 *
 * This provider serves markdown-init.js to the platform's Markdown preview, and that
 * script hardcoded `securityLevel: "loose"` -- so a user who chose Strict because they
 * open untrusted files got `loose` anyway wherever a diagram sat in a fenced block.
 * `loose` permits HTML in node labels and `click ... call` handlers, so the path most
 * likely to meet untrusted content (previewing a cloned repository's README) was the
 * permissive one, and the setting that should have governed it was inert.
 *
 * Unlike the dedicated preview, this is served as bytes rather than substituted into a
 * template, so the substitution has to happen here.
 */
class MermaidMarkdownResourceProviderTest : BasePlatformTestCase() {

    private val provider = MermaidMarkdownResourceProvider()

    // MermaidSettings is an application service shared by every test in the run.
    // Snapshot and restore it, or whichever test runs next inherits our edits.
    private lateinit var saved: MermaidSettings

    override fun setUp() {
        super.setUp()
        saved = MermaidSettings()
        XmlSerializerUtil.copyBean(MermaidSettings.getInstance(), saved)
    }

    override fun tearDown() {
        try {
            XmlSerializerUtil.copyBean(saved, MermaidSettings.getInstance())
        } finally {
            super.tearDown()
        }
    }

    private fun initScript(): String {
        val resource = provider.loadResource(
            MermaidMarkdownResourceProvider.RESOURCE_PREFIX + "markdown-init.js"
        )
        assertNotNull("Provider no longer serves markdown-init.js", resource)
        return String(resource!!.content, Charsets.UTF_8)
    }

    /**
     * The level Mermaid is actually configured with, rather than anywhere the words
     * appear in the file. Asserting on raw file contents also matches explanatory
     * comments, which made an earlier version of this test pass and fail for reasons
     * that had nothing to do with the served configuration.
     */
    private fun servedLevel(): String? =
        Regex("""securityLevel\s*:\s*"([^"]*)"""").find(initScript())?.groupValues?.get(1)

    fun testStrictModeIsServedToTheMarkdownPreview() {
        MermaidSettings.getInstance().securityMode = MermaidSettings.SecurityMode.STRICT

        assertEquals(
            "Markdown preview was not configured with securityLevel 'strict' after the user " +
                "chose Strict. This is the #44 defect: the setting is ignored and the " +
                "Markdown path renders permissively regardless.",
            "strict",
            servedLevel()
        )
    }

    fun testLooseModeIsServedToTheMarkdownPreview() {
        MermaidSettings.getInstance().securityMode = MermaidSettings.SecurityMode.LOOSE

        assertEquals(
            "Markdown preview was not configured with securityLevel 'loose' after the user " +
                "chose Loose",
            "loose",
            servedLevel()
        )
    }

    fun testNoPlaceholderSurvivesIntoTheServedScript() {
        val script = initScript()

        assertFalse(
            "The security level placeholder reached the browser unsubstituted, which is " +
                "not a valid Mermaid securityLevel and would break rendering",
            script.contains("__SECURITY_LEVEL__")
        )
    }

    /**
     * The provider also serves CSS and the Mermaid library itself. Substitution must not
     * corrupt content that has nothing to do with the setting.
     */
    fun testNonScriptResourcesAreServedUnchanged() {
        val css = provider.loadResource(
            MermaidMarkdownResourceProvider.RESOURCE_PREFIX + "markdown-preview.css"
        )
        assertNotNull("Provider no longer serves the Markdown preview stylesheet", css)
        assertTrue("Stylesheet came back empty", css!!.content.isNotEmpty())
    }
}
