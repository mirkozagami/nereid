package com.nereid.spliteditor

import com.nereid.settings.MermaidSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.File

/**
 * Covers the settings `MermaidSplitEditor` is responsible for honouring (#39).
 *
 * The editor cannot be constructed in a headless test -- it builds a JCEF preview panel
 * -- so, as with [MermaidSplitEditorThreadingTest], the wiring that cannot be exercised
 * is asserted against the source instead. The pure parts are tested directly.
 */
class MermaidSplitEditorSettingsTest {

    private val relativePath = "src/main/kotlin/com/nereid/spliteditor/MermaidSplitEditor.kt"

    private fun source(): String {
        var dir: File? = File("").absoluteFile
        while (dir != null) {
            val candidate = File(dir, relativePath)
            if (candidate.isFile) return candidate.readText()
            dir = dir.parentFile
        }
        fail("Could not locate $relativePath from ${File("").absolutePath}")
        return ""
    }

    /**
     * The editor keeps its own `ViewMode` because [MermaidEditorState] serialises it into
     * workspace.xml; collapsing the two enums would change that stored type. The mapping
     * is therefore explicit, so a rename on either side is a compile error rather than a
     * silently unmatched `valueOf`.
     */
    @Test
    fun testEverySettingsViewModeMapsToAnEditorViewMode() {
        assertEquals(
            MermaidSplitEditor.ViewMode.CODE_ONLY,
            MermaidSplitEditor.viewModeFrom(MermaidSettings.ViewMode.CODE_ONLY)
        )
        assertEquals(
            MermaidSplitEditor.ViewMode.SPLIT,
            MermaidSplitEditor.viewModeFrom(MermaidSettings.ViewMode.SPLIT)
        )
        assertEquals(
            MermaidSplitEditor.ViewMode.PREVIEW_ONLY,
            MermaidSplitEditor.viewModeFrom(MermaidSettings.ViewMode.PREVIEW_ONLY)
        )
    }

    /**
     * `defaultViewMode` offers Editor / Split / Preview on the settings page. The editor
     * declared `private var viewMode: ViewMode = ViewMode.SPLIT` and never consulted it,
     * so every file opened split whatever the user chose (#39).
     */
    @Test
    fun testInitialViewModeComesFromTheSettingRatherThanALiteral() {
        val declaration = Regex("""private var viewMode: ViewMode = ([^\n]+)""")
            .find(source())?.groupValues?.get(1)

        assertTrue(
            "MermaidSplitEditor no longer declares 'private var viewMode: ViewMode = ...'; " +
                "update this test to match",
            declaration != null
        )
        assertTrue(
            "The editor's initial view mode is hardcoded to '$declaration'. It must come " +
                "from MermaidSettings.defaultViewMode, or the setting is decoration and " +
                "every file opens in the same mode whatever the user picked (#39).",
            declaration!!.contains("defaultViewMode")
        )
    }

    /**
     * Setting the field alone would leave the split pane laid out for SPLIT, so a user
     * who chose Preview Only would still see the editor pane. The configured mode has to
     * be applied to the layout during construction.
     */
    @Test
    fun testTheConfiguredViewModeIsAppliedToTheLayoutDuringInit() {
        val init = Regex("""\n    init \{.*?\n    \}""", RegexOption.DOT_MATCHES_ALL)
            .find(source())?.value

        assertTrue("MermaidSplitEditor no longer has an init block", init != null)
        assertTrue(
            "The init block never applies the view mode, so the split pane keeps its " +
                "hardcoded SPLIT layout and Editor Only / Preview Only do nothing on open",
            init!!.contains("applyViewMode")
        )
    }
}
