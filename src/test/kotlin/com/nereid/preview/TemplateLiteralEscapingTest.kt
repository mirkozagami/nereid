package com.nereid.preview

import com.nereid.preview.MermaidPreviewPanel.Companion.escapeForTemplateLiteral
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Both the diagram source and the user's custom CSS are handed to the preview page inside
 * a JavaScript template literal. Getting the escaping wrong does not fail loudly -- it
 * produces a syntax error inside the injected script, which surfaces as a preview that
 * silently stops updating.
 *
 * The `$` case is the one worth guarding: an unescaped `${...}` is interpolated by the
 * JavaScript engine rather than treated as text, which is both a correctness bug and the
 * route by which diagram or CSS content could execute code.
 */
class TemplateLiteralEscapingTest {

    @Test
    fun testBackslashesAreEscapedFirst() {
        // Escaped first, otherwise the backslashes introduced by later rules get doubled.
        assertEquals("""a\\b""", escapeForTemplateLiteral("""a\b"""))
    }

    @Test
    fun testBackticksCannotCloseTheLiteral() {
        assertEquals("""a\`b""", escapeForTemplateLiteral("a`b"))
    }

    @Test
    fun testInterpolationIsNeutralised() {
        assertEquals(
            """\${'$'}{alert(1)}""",
            escapeForTemplateLiteral("\${alert(1)}")
        )
    }

    @Test
    fun testNewlinesBecomeEscapeSequences() {
        assertEquals("""a\nb""", escapeForTemplateLiteral("a\nb"))
    }

    @Test
    fun testOrdinaryCssIsUnchanged() {
        val css = "#diagram svg .node rect { stroke-width: 2px; }"
        assertEquals(css, escapeForTemplateLiteral(css))
    }

    @Test
    fun testEmptyInputStaysEmpty() {
        assertEquals("", escapeForTemplateLiteral(""))
    }
}
