package com.nereid.preview

import com.nereid.settings.MermaidSettings
import com.nereid.settings.MermaidSettings.PreviewUpdateMode.LIVE
import com.nereid.settings.MermaidSettings.PreviewUpdateMode.MANUAL
import com.nereid.settings.MermaidSettings.PreviewUpdateMode.ON_SAVE
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.File

/**
 * `previewUpdateMode` offers Live / On Save / Manual on the settings page and was read by
 * nothing but the diagnostics bundle. The preview always re-rendered on a debounce after
 * every keystroke, so On Save and Manual did nothing at all (#39).
 *
 * Unlike the other Tier 2 settings this one needed machinery rather than wiring: On Save
 * needs a save hook and Manual needs a way to ask for a render.
 */
class PreviewUpdatePolicyTest {

    @Test
    fun testLiveRendersAsTheDocumentChanges() {
        assertTrue(shouldRender(RenderTrigger.DOCUMENT_CHANGE, LIVE))
    }

    @Test
    fun testOnSaveIgnoresTypingAndRendersOnSave() {
        assertFalse(
            "On Save still re-rendered while typing, which is the mode's whole purpose",
            shouldRender(RenderTrigger.DOCUMENT_CHANGE, ON_SAVE)
        )
        assertTrue(shouldRender(RenderTrigger.SAVE, ON_SAVE))
    }

    @Test
    fun testManualIgnoresBothTypingAndSaving() {
        assertFalse(
            "Manual re-rendered while typing",
            shouldRender(RenderTrigger.DOCUMENT_CHANGE, MANUAL)
        )
        assertFalse(
            "Manual re-rendered on save",
            shouldRender(RenderTrigger.SAVE, MANUAL)
        )
    }

    /**
     * An explicit refresh is a direct instruction, so it renders in every mode. Manual
     * would otherwise have no way to update at all.
     */
    @Test
    fun testAnExplicitRefreshRendersInEveryMode() {
        MermaidSettings.PreviewUpdateMode.entries.forEach { mode ->
            assertTrue(
                "An explicit refresh did nothing in $mode mode",
                shouldRender(RenderTrigger.MANUAL_REFRESH, mode)
            )
        }
    }

    /**
     * Live must not render twice for one edit. Saving in Live mode is not an extra
     * trigger, because the document change already rendered.
     */
    @Test
    fun testLiveDoesNotAlsoRenderOnSave() {
        assertFalse(
            "Live renders on both the document change and the save, so saving re-renders " +
                "a diagram that is already up to date",
            shouldRender(RenderTrigger.SAVE, LIVE)
        )
    }

    @Test
    fun testTheEditorSubscribesToSavesAndOffersARefresh() {
        val source = editorSource()

        assertTrue(
            "MermaidSplitEditor never consults the update mode, so Live / On Save / " +
                "Manual remain decoration (#39)",
            source.contains("shouldRender")
        )
        assertTrue(
            "Nothing listens for saves, so On Save can never fire",
            source.contains("FileDocumentManagerListener")
        )
    }

    @Test
    fun testTheToolbarOffersARefreshControl() {
        val relative = "src/main/kotlin/com/nereid/spliteditor/MermaidEditorToolbar.kt"
        assertTrue(
            "The toolbar has no refresh control. Manual mode would leave the user no way " +
                "to update the preview at all.",
            read(relative).contains("Refresh")
        )
    }

    private fun editorSource(): String =
        read("src/main/kotlin/com/nereid/spliteditor/MermaidSplitEditor.kt")

    private fun read(relative: String): String {
        var dir: File? = File("").absoluteFile
        while (dir != null) {
            val candidate = File(dir, relative)
            if (candidate.isFile) return candidate.readText()
            dir = dir.parentFile
        }
        fail("Could not locate $relative from ${File("").absolutePath}")
        return ""
    }
}
