# Nereid Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Build a full-featured Mermaid diagramming plugin for JetBrains IDEs with live preview, syntax highlighting, and export capabilities.

**Architecture:** Monolithic Kotlin plugin using JCEF for Mermaid.js rendering, custom Language/PSI for editor features, and split editor for code/preview modes.

**Tech Stack:** Kotlin, Gradle with intellij-platform-gradle-plugin, IntelliJ Platform 2023.3+, JCEF, Mermaid.js, JUnit 5

---

## Phase 1: Project Setup

### Task 1.1: Initialize Gradle Project

**Files:**
- Create: `build.gradle.kts`
- Create: `settings.gradle.kts`
- Create: `gradle.properties`

**Step 1: Create settings.gradle.kts**

```kotlin
rootProject.name = "nereid"
```

**Step 2: Create gradle.properties**

```properties
pluginGroup = com.nereid
pluginName = Nereid
pluginVersion = 0.1.0

platformType = IC
platformVersion = 2023.3

javaVersion = 17
kotlinVersion = 1.9.21

org.gradle.jvmargs = -Xmx2048m
org.gradle.parallel = true
```

**Step 3: Create build.gradle.kts**

```kotlin
plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "1.9.21"
    id("org.jetbrains.intellij.platform") version "2.2.1"
}

group = providers.gradleProperty("pluginGroup").get()
version = providers.gradleProperty("pluginVersion").get()

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    intellijPlatform {
        intellijIdeaCommunity(providers.gradleProperty("platformVersion").get())
        bundledPlugin("com.intellij.java")
        instrumentationTools()
        pluginVerifier()
        testFramework(TestFrameworkType.Platform)
    }
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.1")
}

kotlin {
    jvmToolchain(17)
}

intellijPlatform {
    pluginConfiguration {
        name = providers.gradleProperty("pluginName")
        version = providers.gradleProperty("pluginVersion")
        ideaVersion {
            sinceBuild = "233"
            untilBuild = provider { null }
        }
    }
}

tasks {
    test {
        useJUnitPlatform()
    }
}
```

**Step 4: Create Gradle wrapper**

Run: `gradle wrapper --gradle-version 8.5`

**Step 5: Verify build compiles**

Run: `./gradlew build`
Expected: BUILD SUCCESSFUL

**Step 6: Commit**

```bash
git add build.gradle.kts settings.gradle.kts gradle.properties gradle/ gradlew gradlew.bat
git commit -m "chore: initialize Gradle project with IntelliJ Platform plugin"
```

---

### Task 1.2: Create Plugin Descriptor

**Files:**
- Create: `src/main/resources/META-INF/plugin.xml`

**Step 1: Create directory structure**

```bash
mkdir -p src/main/resources/META-INF
mkdir -p src/main/kotlin/com/nereid
mkdir -p src/test/kotlin/com/nereid
```

**Step 2: Create plugin.xml**

```xml
<idea-plugin>
    <id>com.nereid.mermaid</id>
    <name>Nereid - Mermaid Diagrams</name>
    <vendor email="support@nereid.dev" url="https://github.com/nereid">Nereid</vendor>

    <description><![CDATA[
    Full-featured Mermaid diagram support for JetBrains IDEs.

    <ul>
        <li>Live preview with zoom and pan</li>
        <li>Syntax highlighting and error detection</li>
        <li>Autocomplete for diagram types and syntax</li>
        <li>Export to PNG, SVG, and clipboard</li>
        <li>Markdown fenced code block support</li>
    </ul>
    ]]></description>

    <depends>com.intellij.modules.platform</depends>

    <extensions defaultExtensionNs="com.intellij">
        <!-- Extensions will be added as we implement features -->
    </extensions>

    <actions>
        <!-- Actions will be added as we implement features -->
    </actions>
</idea-plugin>
```

**Step 3: Verify plugin builds**

Run: `./gradlew buildPlugin`
Expected: BUILD SUCCESSFUL, plugin ZIP created in build/distributions/

**Step 4: Commit**

```bash
git add src/
git commit -m "chore: add plugin descriptor and directory structure"
```

---

### Task 1.3: Create Plugin Icon

**Files:**
- Create: `src/main/resources/META-INF/pluginIcon.svg`

**Step 1: Create plugin icon (40x40 SVG)**

```svg
<svg width="40" height="40" viewBox="0 0 40 40" xmlns="http://www.w3.org/2000/svg">
  <rect width="40" height="40" rx="8" fill="#1a1a2e"/>
  <path d="M8 12h10v6H8z" fill="#4fc3f7"/>
  <path d="M22 12h10v6H22z" fill="#4fc3f7"/>
  <path d="M15 22h10v6H15z" fill="#81c784"/>
  <path d="M13 18v4h2v-4z" fill="#4fc3f7"/>
  <path d="M25 18v4h2v-4z" fill="#4fc3f7"/>
  <path d="M20 28v4h2v-4z" fill="#81c784"/>
</svg>
```

**Step 2: Create dark variant**

Create: `src/main/resources/META-INF/pluginIcon_dark.svg`

```svg
<svg width="40" height="40" viewBox="0 0 40 40" xmlns="http://www.w3.org/2000/svg">
  <rect width="40" height="40" rx="8" fill="#2d2d3a"/>
  <path d="M8 12h10v6H8z" fill="#4fc3f7"/>
  <path d="M22 12h10v6H22z" fill="#4fc3f7"/>
  <path d="M15 22h10v6H15z" fill="#81c784"/>
  <path d="M13 18v4h2v-4z" fill="#4fc3f7"/>
  <path d="M25 18v4h2v-4z" fill="#4fc3f7"/>
  <path d="M20 28v4h2v-4z" fill="#81c784"/>
</svg>
```

**Step 3: Commit**

```bash
git add src/main/resources/META-INF/pluginIcon*.svg
git commit -m "chore: add plugin icons"
```

---

## Phase 2: Language Foundation

### Task 2.1: Create Mermaid Language Definition

**Files:**
- Create: `src/main/kotlin/com/nereid/language/MermaidLanguage.kt`
- Test: `src/test/kotlin/com/nereid/language/MermaidLanguageTest.kt`

**Step 1: Write the failing test**

```kotlin
package com.nereid.language

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.junit.jupiter.api.Assertions.*

class MermaidLanguageTest : BasePlatformTestCase() {

    fun testLanguageInstance() {
        val language = MermaidLanguage.INSTANCE
        assertNotNull(language)
        assertEquals("Mermaid", language.id)
        assertEquals("Mermaid", language.displayName)
    }

    fun testLanguageIsCaseSensitive() {
        assertTrue(MermaidLanguage.INSTANCE.isCaseSensitive)
    }
}
```

**Step 2: Run test to verify it fails**

Run: `./gradlew test --tests "com.nereid.language.MermaidLanguageTest"`
Expected: FAIL - MermaidLanguage not found

**Step 3: Write minimal implementation**

```kotlin
package com.nereid.language

import com.intellij.lang.Language

object MermaidLanguage : Language("Mermaid") {

    @JvmStatic
    val INSTANCE: MermaidLanguage = this

    override fun getDisplayName(): String = "Mermaid"

    override fun isCaseSensitive(): Boolean = true
}
```

**Step 4: Run test to verify it passes**

Run: `./gradlew test --tests "com.nereid.language.MermaidLanguageTest"`
Expected: PASS

**Step 5: Commit**

```bash
git add src/main/kotlin/com/nereid/language/MermaidLanguage.kt
git add src/test/kotlin/com/nereid/language/MermaidLanguageTest.kt
git commit -m "feat: add MermaidLanguage definition"
```

---

### Task 2.2: Create Mermaid File Type

**Files:**
- Create: `src/main/kotlin/com/nereid/language/MermaidFileType.kt`
- Create: `src/main/resources/icons/mermaid.svg`
- Modify: `src/main/resources/META-INF/plugin.xml`
- Test: `src/test/kotlin/com/nereid/language/MermaidFileTypeTest.kt`

**Step 1: Write the failing test**

```kotlin
package com.nereid.language

import com.intellij.testFramework.fixtures.BasePlatformTestCase

class MermaidFileTypeTest : BasePlatformTestCase() {

    fun testFileTypeProperties() {
        val fileType = MermaidFileType.INSTANCE
        assertEquals("Mermaid", fileType.name)
        assertEquals("Mermaid diagram file", fileType.description)
        assertEquals("mmd", fileType.defaultExtension)
        assertNotNull(fileType.icon)
    }

    fun testFileTypeAssociation() {
        val file = myFixture.configureByText("test.mmd", "graph TD")
        assertEquals(MermaidFileType.INSTANCE, file.virtualFile.fileType)
    }
}
```

**Step 2: Run test to verify it fails**

Run: `./gradlew test --tests "com.nereid.language.MermaidFileTypeTest"`
Expected: FAIL - MermaidFileType not found

**Step 3: Create file icon**

Create `src/main/resources/icons/mermaid.svg`:

```svg
<svg width="16" height="16" viewBox="0 0 16 16" xmlns="http://www.w3.org/2000/svg">
  <rect x="1" y="2" width="6" height="4" rx="1" fill="#4fc3f7"/>
  <rect x="9" y="2" width="6" height="4" rx="1" fill="#4fc3f7"/>
  <rect x="5" y="10" width="6" height="4" rx="1" fill="#81c784"/>
  <path d="M4 6v2h1V6z" fill="#4fc3f7"/>
  <path d="M11 6v2h1V6z" fill="#4fc3f7"/>
  <path d="M8 8v2h1V8z" fill="#81c784"/>
</svg>
```

**Step 4: Write MermaidFileType implementation**

```kotlin
package com.nereid.language

import com.intellij.openapi.fileTypes.LanguageFileType
import com.intellij.openapi.util.IconLoader
import javax.swing.Icon

object MermaidFileType : LanguageFileType(MermaidLanguage.INSTANCE) {

    @JvmStatic
    val INSTANCE: MermaidFileType = this

    override fun getName(): String = "Mermaid"

    override fun getDescription(): String = "Mermaid diagram file"

    override fun getDefaultExtension(): String = "mmd"

    override fun getIcon(): Icon = IconLoader.getIcon("/icons/mermaid.svg", MermaidFileType::class.java)
}
```

**Step 5: Register file type in plugin.xml**

Add inside `<extensions defaultExtensionNs="com.intellij">`:

```xml
<fileType
    name="Mermaid"
    implementationClass="com.nereid.language.MermaidFileType"
    fieldName="INSTANCE"
    language="Mermaid"
    extensions="mmd;mermaid"/>
```

**Step 6: Run test to verify it passes**

Run: `./gradlew test --tests "com.nereid.language.MermaidFileTypeTest"`
Expected: PASS

**Step 7: Commit**

```bash
git add src/main/kotlin/com/nereid/language/MermaidFileType.kt
git add src/main/resources/icons/mermaid.svg
git add src/main/resources/META-INF/plugin.xml
git add src/test/kotlin/com/nereid/language/MermaidFileTypeTest.kt
git commit -m "feat: add MermaidFileType with .mmd extension"
```

---

### Task 2.3: Create Mermaid Token Types

**Files:**
- Create: `src/main/kotlin/com/nereid/language/MermaidTokenTypes.kt`
- Create: `src/main/kotlin/com/nereid/language/MermaidElementType.kt`

**Step 1: Create MermaidElementType**

```kotlin
package com.nereid.language

import com.intellij.psi.tree.IElementType

class MermaidElementType(debugName: String) : IElementType(debugName, MermaidLanguage.INSTANCE)
```

**Step 2: Create MermaidTokenTypes**

```kotlin
package com.nereid.language

import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.TokenSet

object MermaidTokenTypes {

    // Diagram types
    @JvmField val GRAPH = MermaidElementType("GRAPH")
    @JvmField val FLOWCHART = MermaidElementType("FLOWCHART")
    @JvmField val SEQUENCE_DIAGRAM = MermaidElementType("SEQUENCE_DIAGRAM")
    @JvmField val CLASS_DIAGRAM = MermaidElementType("CLASS_DIAGRAM")
    @JvmField val STATE_DIAGRAM = MermaidElementType("STATE_DIAGRAM")
    @JvmField val ER_DIAGRAM = MermaidElementType("ER_DIAGRAM")
    @JvmField val GANTT = MermaidElementType("GANTT")
    @JvmField val PIE = MermaidElementType("PIE")
    @JvmField val GITGRAPH = MermaidElementType("GITGRAPH")
    @JvmField val MINDMAP = MermaidElementType("MINDMAP")
    @JvmField val TIMELINE = MermaidElementType("TIMELINE")
    @JvmField val QUADRANT = MermaidElementType("QUADRANT")
    @JvmField val REQUIREMENT = MermaidElementType("REQUIREMENT")
    @JvmField val C4CONTEXT = MermaidElementType("C4CONTEXT")
    @JvmField val SANKEY = MermaidElementType("SANKEY")
    @JvmField val XY_CHART = MermaidElementType("XY_CHART")
    @JvmField val BLOCK = MermaidElementType("BLOCK")

    // Directions
    @JvmField val DIRECTION = MermaidElementType("DIRECTION")

    // Keywords
    @JvmField val KEYWORD = MermaidElementType("KEYWORD")
    @JvmField val SUBGRAPH = MermaidElementType("SUBGRAPH")
    @JvmField val END = MermaidElementType("END")

    // Identifiers and literals
    @JvmField val IDENTIFIER = MermaidElementType("IDENTIFIER")
    @JvmField val STRING = MermaidElementType("STRING")
    @JvmField val NUMBER = MermaidElementType("NUMBER")

    // Connectors/arrows
    @JvmField val ARROW = MermaidElementType("ARROW")
    @JvmField val LINE = MermaidElementType("LINE")

    // Brackets and delimiters
    @JvmField val LBRACKET = MermaidElementType("LBRACKET")
    @JvmField val RBRACKET = MermaidElementType("RBRACKET")
    @JvmField val LPAREN = MermaidElementType("LPAREN")
    @JvmField val RPAREN = MermaidElementType("RPAREN")
    @JvmField val LBRACE = MermaidElementType("LBRACE")
    @JvmField val RBRACE = MermaidElementType("RBRACE")
    @JvmField val PIPE = MermaidElementType("PIPE")
    @JvmField val COLON = MermaidElementType("COLON")
    @JvmField val SEMICOLON = MermaidElementType("SEMICOLON")

    // Comments and directives
    @JvmField val COMMENT = MermaidElementType("COMMENT")
    @JvmField val DIRECTIVE = MermaidElementType("DIRECTIVE")

    // Whitespace and misc
    @JvmField val WHITE_SPACE = MermaidElementType("WHITE_SPACE")
    @JvmField val NEWLINE = MermaidElementType("NEWLINE")
    @JvmField val BAD_CHARACTER = MermaidElementType("BAD_CHARACTER")

    // Token sets for syntax highlighter
    @JvmField val DIAGRAM_TYPES = TokenSet.create(
        GRAPH, FLOWCHART, SEQUENCE_DIAGRAM, CLASS_DIAGRAM, STATE_DIAGRAM,
        ER_DIAGRAM, GANTT, PIE, GITGRAPH, MINDMAP, TIMELINE, QUADRANT,
        REQUIREMENT, C4CONTEXT, SANKEY, XY_CHART, BLOCK
    )

    @JvmField val KEYWORDS = TokenSet.create(KEYWORD, SUBGRAPH, END, DIRECTION)

    @JvmField val STRINGS = TokenSet.create(STRING)

    @JvmField val COMMENTS = TokenSet.create(COMMENT)

    @JvmField val BRACKETS = TokenSet.create(
        LBRACKET, RBRACKET, LPAREN, RPAREN, LBRACE, RBRACE
    )
}
```

**Step 3: Commit**

```bash
git add src/main/kotlin/com/nereid/language/MermaidElementType.kt
git add src/main/kotlin/com/nereid/language/MermaidTokenTypes.kt
git commit -m "feat: add Mermaid token types"
```

---

### Task 2.4: Create Mermaid Lexer

**Files:**
- Create: `src/main/kotlin/com/nereid/language/MermaidLexer.kt`
- Test: `src/test/kotlin/com/nereid/language/MermaidLexerTest.kt`

**Step 1: Write the failing test**

```kotlin
package com.nereid.language

import com.intellij.testFramework.LexerTestCase

class MermaidLexerTest : LexerTestCase() {

    override fun createLexer() = MermaidLexer()

    override fun getDirPath() = "src/test/resources/lexer"

    fun testDiagramType() {
        doTest(
            "graph TD",
            """
            GRAPH ('graph')
            WHITE_SPACE (' ')
            DIRECTION ('TD')
            """.trimIndent()
        )
    }

    fun testFlowchartWithNodes() {
        doTest(
            "flowchart LR\n    A --> B",
            """
            FLOWCHART ('flowchart')
            WHITE_SPACE (' ')
            DIRECTION ('LR')
            NEWLINE ('\n')
            WHITE_SPACE ('    ')
            IDENTIFIER ('A')
            WHITE_SPACE (' ')
            ARROW ('-->')
            WHITE_SPACE (' ')
            IDENTIFIER ('B')
            """.trimIndent()
        )
    }

    fun testComment() {
        doTest(
            "%% This is a comment",
            """
            COMMENT ('%% This is a comment')
            """.trimIndent()
        )
    }

    fun testDirective() {
        doTest(
            "%%{init: {'theme': 'dark'}}%%",
            """
            DIRECTIVE ('%%{init: {'theme': 'dark'}}%%')
            """.trimIndent()
        )
    }

    fun testNodeWithLabel() {
        doTest(
            "A[Hello World]",
            """
            IDENTIFIER ('A')
            LBRACKET ('[')
            STRING ('Hello World')
            RBRACKET (']')
            """.trimIndent()
        )
    }
}
```

**Step 2: Run test to verify it fails**

Run: `./gradlew test --tests "com.nereid.language.MermaidLexerTest"`
Expected: FAIL - MermaidLexer not found

**Step 3: Write MermaidLexer implementation**

```kotlin
package com.nereid.language

import com.intellij.lexer.LexerBase
import com.intellij.psi.tree.IElementType

class MermaidLexer : LexerBase() {

    private var buffer: CharSequence = ""
    private var bufferEnd: Int = 0
    private var tokenStart: Int = 0
    private var tokenEnd: Int = 0
    private var tokenType: IElementType? = null

    companion object {
        private val DIAGRAM_KEYWORDS = mapOf(
            "graph" to MermaidTokenTypes.GRAPH,
            "flowchart" to MermaidTokenTypes.FLOWCHART,
            "sequenceDiagram" to MermaidTokenTypes.SEQUENCE_DIAGRAM,
            "classDiagram" to MermaidTokenTypes.CLASS_DIAGRAM,
            "stateDiagram" to MermaidTokenTypes.STATE_DIAGRAM,
            "stateDiagram-v2" to MermaidTokenTypes.STATE_DIAGRAM,
            "erDiagram" to MermaidTokenTypes.ER_DIAGRAM,
            "gantt" to MermaidTokenTypes.GANTT,
            "pie" to MermaidTokenTypes.PIE,
            "gitGraph" to MermaidTokenTypes.GITGRAPH,
            "mindmap" to MermaidTokenTypes.MINDMAP,
            "timeline" to MermaidTokenTypes.TIMELINE,
            "quadrantChart" to MermaidTokenTypes.QUADRANT,
            "requirementDiagram" to MermaidTokenTypes.REQUIREMENT,
            "C4Context" to MermaidTokenTypes.C4CONTEXT,
            "sankey-beta" to MermaidTokenTypes.SANKEY,
            "xychart-beta" to MermaidTokenTypes.XY_CHART,
            "block-beta" to MermaidTokenTypes.BLOCK
        )

        private val DIRECTIONS = setOf("TB", "TD", "BT", "RL", "LR")

        private val KEYWORDS = mapOf(
            "subgraph" to MermaidTokenTypes.SUBGRAPH,
            "end" to MermaidTokenTypes.END,
            "participant" to MermaidTokenTypes.KEYWORD,
            "actor" to MermaidTokenTypes.KEYWORD,
            "note" to MermaidTokenTypes.KEYWORD,
            "loop" to MermaidTokenTypes.KEYWORD,
            "alt" to MermaidTokenTypes.KEYWORD,
            "else" to MermaidTokenTypes.KEYWORD,
            "opt" to MermaidTokenTypes.KEYWORD,
            "par" to MermaidTokenTypes.KEYWORD,
            "rect" to MermaidTokenTypes.KEYWORD,
            "class" to MermaidTokenTypes.KEYWORD,
            "section" to MermaidTokenTypes.KEYWORD,
            "title" to MermaidTokenTypes.KEYWORD
        )

        private val ARROWS = listOf(
            "-->", "---", "-.->", "-.-", "==>", "===",
            "--o", "--x", "o--o", "x--x", "<-->", "<-.->",
            "->", "--", "-)", "(-", "-))", "((-"
        )
    }

    override fun start(buffer: CharSequence, startOffset: Int, endOffset: Int, initialState: Int) {
        this.buffer = buffer
        this.bufferEnd = endOffset
        this.tokenStart = startOffset
        this.tokenEnd = startOffset
        advance()
    }

    override fun getState(): Int = 0

    override fun getTokenType(): IElementType? = tokenType

    override fun getTokenStart(): Int = tokenStart

    override fun getTokenEnd(): Int = tokenEnd

    override fun advance() {
        tokenStart = tokenEnd
        if (tokenStart >= bufferEnd) {
            tokenType = null
            return
        }

        val c = buffer[tokenStart]

        when {
            c == '%' && lookAhead(1) == '%' -> lexCommentOrDirective()
            c == '\n' || c == '\r' -> lexNewline()
            c.isWhitespace() -> lexWhitespace()
            c == '"' || c == '\'' -> lexQuotedString()
            c == '[' -> lexBracketContent()
            c == '(' -> lexParenContent()
            c == '{' -> lexBraceContent()
            c == ']' -> singleChar(MermaidTokenTypes.RBRACKET)
            c == ')' -> singleChar(MermaidTokenTypes.RPAREN)
            c == '}' -> singleChar(MermaidTokenTypes.RBRACE)
            c == '|' -> singleChar(MermaidTokenTypes.PIPE)
            c == ':' -> singleChar(MermaidTokenTypes.COLON)
            c == ';' -> singleChar(MermaidTokenTypes.SEMICOLON)
            isArrowStart(c) -> lexArrow()
            c.isLetter() || c == '_' -> lexIdentifier()
            c.isDigit() -> lexNumber()
            else -> singleChar(MermaidTokenTypes.BAD_CHARACTER)
        }
    }

    override fun getBufferSequence(): CharSequence = buffer

    override fun getBufferEnd(): Int = bufferEnd

    private fun lookAhead(offset: Int): Char? {
        val pos = tokenStart + offset
        return if (pos < bufferEnd) buffer[pos] else null
    }

    private fun singleChar(type: IElementType) {
        tokenType = type
        tokenEnd = tokenStart + 1
    }

    private fun lexCommentOrDirective() {
        if (lookAhead(2) == '{') {
            // Directive: %%{...}%%
            var pos = tokenStart + 3
            while (pos < bufferEnd - 2) {
                if (buffer[pos] == '}' && buffer[pos + 1] == '%' && buffer[pos + 2] == '%') {
                    tokenEnd = pos + 3
                    tokenType = MermaidTokenTypes.DIRECTIVE
                    return
                }
                pos++
            }
            tokenEnd = bufferEnd
            tokenType = MermaidTokenTypes.DIRECTIVE
        } else {
            // Comment: %% until end of line
            var pos = tokenStart + 2
            while (pos < bufferEnd && buffer[pos] != '\n' && buffer[pos] != '\r') {
                pos++
            }
            tokenEnd = pos
            tokenType = MermaidTokenTypes.COMMENT
        }
    }

    private fun lexNewline() {
        var pos = tokenStart
        while (pos < bufferEnd && (buffer[pos] == '\n' || buffer[pos] == '\r')) {
            pos++
        }
        tokenEnd = pos
        tokenType = MermaidTokenTypes.NEWLINE
    }

    private fun lexWhitespace() {
        var pos = tokenStart
        while (pos < bufferEnd && buffer[pos].isWhitespace() && buffer[pos] != '\n' && buffer[pos] != '\r') {
            pos++
        }
        tokenEnd = pos
        tokenType = MermaidTokenTypes.WHITE_SPACE
    }

    private fun lexQuotedString() {
        val quote = buffer[tokenStart]
        var pos = tokenStart + 1
        while (pos < bufferEnd) {
            val c = buffer[pos]
            if (c == quote) {
                tokenEnd = pos + 1
                tokenType = MermaidTokenTypes.STRING
                return
            }
            if (c == '\\' && pos + 1 < bufferEnd) {
                pos += 2
            } else {
                pos++
            }
        }
        tokenEnd = bufferEnd
        tokenType = MermaidTokenTypes.STRING
    }

    private fun lexBracketContent() {
        tokenType = MermaidTokenTypes.LBRACKET
        tokenEnd = tokenStart + 1
    }

    private fun lexParenContent() {
        tokenType = MermaidTokenTypes.LPAREN
        tokenEnd = tokenStart + 1
    }

    private fun lexBraceContent() {
        tokenType = MermaidTokenTypes.LBRACE
        tokenEnd = tokenStart + 1
    }

    private fun isArrowStart(c: Char): Boolean {
        return c == '-' || c == '=' || c == '.' || c == '<' || c == 'o' || c == 'x'
    }

    private fun lexArrow() {
        for (arrow in ARROWS.sortedByDescending { it.length }) {
            if (matches(arrow)) {
                tokenEnd = tokenStart + arrow.length
                tokenType = MermaidTokenTypes.ARROW
                return
            }
        }
        // Not an arrow, treat as identifier or bad char
        if (buffer[tokenStart].isLetter()) {
            lexIdentifier()
        } else {
            singleChar(MermaidTokenTypes.BAD_CHARACTER)
        }
    }

    private fun matches(s: String): Boolean {
        if (tokenStart + s.length > bufferEnd) return false
        for (i in s.indices) {
            if (buffer[tokenStart + i] != s[i]) return false
        }
        return true
    }

    private fun lexIdentifier() {
        var pos = tokenStart
        while (pos < bufferEnd) {
            val c = buffer[pos]
            if (c.isLetterOrDigit() || c == '_' || c == '-') {
                pos++
            } else {
                break
            }
        }
        tokenEnd = pos
        val text = buffer.substring(tokenStart, tokenEnd)

        tokenType = when {
            DIAGRAM_KEYWORDS.containsKey(text) -> DIAGRAM_KEYWORDS[text]
            DIRECTIONS.contains(text) -> MermaidTokenTypes.DIRECTION
            KEYWORDS.containsKey(text) -> KEYWORDS[text]
            else -> MermaidTokenTypes.IDENTIFIER
        }
    }

    private fun lexNumber() {
        var pos = tokenStart
        while (pos < bufferEnd && (buffer[pos].isDigit() || buffer[pos] == '.')) {
            pos++
        }
        tokenEnd = pos
        tokenType = MermaidTokenTypes.NUMBER
    }
}
```

**Step 4: Run test to verify it passes**

Run: `./gradlew test --tests "com.nereid.language.MermaidLexerTest"`
Expected: PASS

**Step 5: Commit**

```bash
git add src/main/kotlin/com/nereid/language/MermaidLexer.kt
git add src/test/kotlin/com/nereid/language/MermaidLexerTest.kt
git commit -m "feat: add Mermaid lexer with token recognition"
```

---

### Task 2.5: Create Lexer Adapter for Parser

**Files:**
- Create: `src/main/kotlin/com/nereid/language/MermaidLexerAdapter.kt`

**Step 1: Write implementation**

```kotlin
package com.nereid.language

import com.intellij.lexer.FlexAdapter

class MermaidLexerAdapter : FlexAdapter(MermaidLexer())
```

**Step 2: Commit**

```bash
git add src/main/kotlin/com/nereid/language/MermaidLexerAdapter.kt
git commit -m "feat: add MermaidLexerAdapter"
```

---

## Phase 3: Syntax Highlighting

### Task 3.1: Create Syntax Highlighter

**Files:**
- Create: `src/main/kotlin/com/nereid/editor/MermaidSyntaxHighlighter.kt`
- Test: `src/test/kotlin/com/nereid/editor/MermaidSyntaxHighlighterTest.kt`

**Step 1: Write the failing test**

```kotlin
package com.nereid.editor

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.nereid.language.MermaidTokenTypes

class MermaidSyntaxHighlighterTest : BasePlatformTestCase() {

    fun testHighlighterReturnsCorrectAttributesForDiagramType() {
        val highlighter = MermaidSyntaxHighlighter()
        val attributes = highlighter.getTokenHighlights(MermaidTokenTypes.GRAPH)
        assertTrue(attributes.isNotEmpty())
        assertEquals(MermaidSyntaxHighlighter.DIAGRAM_TYPE, attributes[0])
    }

    fun testHighlighterReturnsCorrectAttributesForKeyword() {
        val highlighter = MermaidSyntaxHighlighter()
        val attributes = highlighter.getTokenHighlights(MermaidTokenTypes.KEYWORD)
        assertTrue(attributes.isNotEmpty())
        assertEquals(MermaidSyntaxHighlighter.KEYWORD, attributes[0])
    }

    fun testHighlighterReturnsCorrectAttributesForComment() {
        val highlighter = MermaidSyntaxHighlighter()
        val attributes = highlighter.getTokenHighlights(MermaidTokenTypes.COMMENT)
        assertTrue(attributes.isNotEmpty())
        assertEquals(MermaidSyntaxHighlighter.COMMENT, attributes[0])
    }
}
```

**Step 2: Run test to verify it fails**

Run: `./gradlew test --tests "com.nereid.editor.MermaidSyntaxHighlighterTest"`
Expected: FAIL - MermaidSyntaxHighlighter not found

**Step 3: Write implementation**

```kotlin
package com.nereid.editor

import com.intellij.lexer.Lexer
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.editor.HighlighterColors
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.editor.colors.TextAttributesKey.createTextAttributesKey
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase
import com.intellij.psi.tree.IElementType
import com.nereid.language.MermaidLexer
import com.nereid.language.MermaidTokenTypes

class MermaidSyntaxHighlighter : SyntaxHighlighterBase() {

    companion object {
        @JvmField
        val DIAGRAM_TYPE = createTextAttributesKey(
            "MERMAID_DIAGRAM_TYPE",
            DefaultLanguageHighlighterColors.KEYWORD
        )

        @JvmField
        val KEYWORD = createTextAttributesKey(
            "MERMAID_KEYWORD",
            DefaultLanguageHighlighterColors.KEYWORD
        )

        @JvmField
        val DIRECTION = createTextAttributesKey(
            "MERMAID_DIRECTION",
            DefaultLanguageHighlighterColors.CONSTANT
        )

        @JvmField
        val IDENTIFIER = createTextAttributesKey(
            "MERMAID_IDENTIFIER",
            DefaultLanguageHighlighterColors.IDENTIFIER
        )

        @JvmField
        val STRING = createTextAttributesKey(
            "MERMAID_STRING",
            DefaultLanguageHighlighterColors.STRING
        )

        @JvmField
        val NUMBER = createTextAttributesKey(
            "MERMAID_NUMBER",
            DefaultLanguageHighlighterColors.NUMBER
        )

        @JvmField
        val ARROW = createTextAttributesKey(
            "MERMAID_ARROW",
            DefaultLanguageHighlighterColors.OPERATION_SIGN
        )

        @JvmField
        val BRACKETS = createTextAttributesKey(
            "MERMAID_BRACKETS",
            DefaultLanguageHighlighterColors.BRACKETS
        )

        @JvmField
        val COMMENT = createTextAttributesKey(
            "MERMAID_COMMENT",
            DefaultLanguageHighlighterColors.LINE_COMMENT
        )

        @JvmField
        val DIRECTIVE = createTextAttributesKey(
            "MERMAID_DIRECTIVE",
            DefaultLanguageHighlighterColors.METADATA
        )

        @JvmField
        val BAD_CHARACTER = createTextAttributesKey(
            "MERMAID_BAD_CHARACTER",
            HighlighterColors.BAD_CHARACTER
        )

        private val DIAGRAM_TYPE_KEYS = arrayOf(DIAGRAM_TYPE)
        private val KEYWORD_KEYS = arrayOf(KEYWORD)
        private val DIRECTION_KEYS = arrayOf(DIRECTION)
        private val IDENTIFIER_KEYS = arrayOf(IDENTIFIER)
        private val STRING_KEYS = arrayOf(STRING)
        private val NUMBER_KEYS = arrayOf(NUMBER)
        private val ARROW_KEYS = arrayOf(ARROW)
        private val BRACKETS_KEYS = arrayOf(BRACKETS)
        private val COMMENT_KEYS = arrayOf(COMMENT)
        private val DIRECTIVE_KEYS = arrayOf(DIRECTIVE)
        private val BAD_CHARACTER_KEYS = arrayOf(BAD_CHARACTER)
        private val EMPTY_KEYS = emptyArray<TextAttributesKey>()
    }

    override fun getHighlightingLexer(): Lexer = MermaidLexer()

    override fun getTokenHighlights(tokenType: IElementType?): Array<TextAttributesKey> {
        return when (tokenType) {
            in MermaidTokenTypes.DIAGRAM_TYPES.types -> DIAGRAM_TYPE_KEYS

            MermaidTokenTypes.KEYWORD,
            MermaidTokenTypes.SUBGRAPH,
            MermaidTokenTypes.END -> KEYWORD_KEYS

            MermaidTokenTypes.DIRECTION -> DIRECTION_KEYS

            MermaidTokenTypes.IDENTIFIER -> IDENTIFIER_KEYS

            MermaidTokenTypes.STRING -> STRING_KEYS

            MermaidTokenTypes.NUMBER -> NUMBER_KEYS

            MermaidTokenTypes.ARROW,
            MermaidTokenTypes.LINE -> ARROW_KEYS

            MermaidTokenTypes.LBRACKET,
            MermaidTokenTypes.RBRACKET,
            MermaidTokenTypes.LPAREN,
            MermaidTokenTypes.RPAREN,
            MermaidTokenTypes.LBRACE,
            MermaidTokenTypes.RBRACE -> BRACKETS_KEYS

            MermaidTokenTypes.COMMENT -> COMMENT_KEYS

            MermaidTokenTypes.DIRECTIVE -> DIRECTIVE_KEYS

            MermaidTokenTypes.BAD_CHARACTER -> BAD_CHARACTER_KEYS

            else -> EMPTY_KEYS
        }
    }
}
```

**Step 4: Run test to verify it passes**

Run: `./gradlew test --tests "com.nereid.editor.MermaidSyntaxHighlighterTest"`
Expected: PASS

**Step 5: Commit**

```bash
git add src/main/kotlin/com/nereid/editor/MermaidSyntaxHighlighter.kt
git add src/test/kotlin/com/nereid/editor/MermaidSyntaxHighlighterTest.kt
git commit -m "feat: add syntax highlighter with token-to-color mapping"
```

---

### Task 3.2: Create Syntax Highlighter Factory

**Files:**
- Create: `src/main/kotlin/com/nereid/editor/MermaidSyntaxHighlighterFactory.kt`
- Modify: `src/main/resources/META-INF/plugin.xml`

**Step 1: Write implementation**

```kotlin
package com.nereid.editor

import com.intellij.openapi.fileTypes.SyntaxHighlighter
import com.intellij.openapi.fileTypes.SyntaxHighlighterFactory
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile

class MermaidSyntaxHighlighterFactory : SyntaxHighlighterFactory() {

    override fun getSyntaxHighlighter(project: Project?, virtualFile: VirtualFile?): SyntaxHighlighter {
        return MermaidSyntaxHighlighter()
    }
}
```

**Step 2: Register in plugin.xml**

Add inside `<extensions defaultExtensionNs="com.intellij">`:

```xml
<lang.syntaxHighlighterFactory
    language="Mermaid"
    implementationClass="com.nereid.editor.MermaidSyntaxHighlighterFactory"/>
```

**Step 3: Verify build**

Run: `./gradlew buildPlugin`
Expected: BUILD SUCCESSFUL

**Step 4: Commit**

```bash
git add src/main/kotlin/com/nereid/editor/MermaidSyntaxHighlighterFactory.kt
git add src/main/resources/META-INF/plugin.xml
git commit -m "feat: register syntax highlighter factory"
```

---

### Task 3.3: Create Color Settings Page

**Files:**
- Create: `src/main/kotlin/com/nereid/editor/MermaidColorSettingsPage.kt`
- Modify: `src/main/resources/META-INF/plugin.xml`

**Step 1: Write implementation**

```kotlin
package com.nereid.editor

import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.fileTypes.SyntaxHighlighter
import com.intellij.openapi.options.colors.AttributesDescriptor
import com.intellij.openapi.options.colors.ColorDescriptor
import com.intellij.openapi.options.colors.ColorSettingsPage
import com.nereid.language.MermaidFileType
import javax.swing.Icon

class MermaidColorSettingsPage : ColorSettingsPage {

    companion object {
        private val DESCRIPTORS = arrayOf(
            AttributesDescriptor("Diagram type", MermaidSyntaxHighlighter.DIAGRAM_TYPE),
            AttributesDescriptor("Keyword", MermaidSyntaxHighlighter.KEYWORD),
            AttributesDescriptor("Direction", MermaidSyntaxHighlighter.DIRECTION),
            AttributesDescriptor("Identifier", MermaidSyntaxHighlighter.IDENTIFIER),
            AttributesDescriptor("String", MermaidSyntaxHighlighter.STRING),
            AttributesDescriptor("Number", MermaidSyntaxHighlighter.NUMBER),
            AttributesDescriptor("Arrow", MermaidSyntaxHighlighter.ARROW),
            AttributesDescriptor("Brackets", MermaidSyntaxHighlighter.BRACKETS),
            AttributesDescriptor("Comment", MermaidSyntaxHighlighter.COMMENT),
            AttributesDescriptor("Directive", MermaidSyntaxHighlighter.DIRECTIVE),
            AttributesDescriptor("Bad character", MermaidSyntaxHighlighter.BAD_CHARACTER)
        )
    }

    override fun getIcon(): Icon = MermaidFileType.INSTANCE.icon

    override fun getHighlighter(): SyntaxHighlighter = MermaidSyntaxHighlighter()

    override fun getDemoText(): String = """
        %%{init: {'theme': 'dark'}}%%
        %% A sample flowchart
        flowchart LR
            A[Start] --> B{Decision}
            B -->|Yes| C[Process 1]
            B -->|No| D[Process 2]
            C --> E((End))
            D --> E

            subgraph sub1 [Subprocess]
                F["Step 1"] --> G["Step 2"]
            end
    """.trimIndent()

    override fun getAdditionalHighlightingTagToDescriptorMap(): Map<String, TextAttributesKey>? = null

    override fun getAttributeDescriptors(): Array<AttributesDescriptor> = DESCRIPTORS

    override fun getColorDescriptors(): Array<ColorDescriptor> = ColorDescriptor.EMPTY_ARRAY

    override fun getDisplayName(): String = "Mermaid"
}
```

**Step 2: Register in plugin.xml**

Add inside `<extensions defaultExtensionNs="com.intellij">`:

```xml
<colorSettingsPage implementationClass="com.nereid.editor.MermaidColorSettingsPage"/>
```

**Step 3: Commit**

```bash
git add src/main/kotlin/com/nereid/editor/MermaidColorSettingsPage.kt
git add src/main/resources/META-INF/plugin.xml
git commit -m "feat: add color settings page for Mermaid highlighting"
```

---

## Phase 4: Basic Preview

### Task 4.1: Bundle Mermaid.js

**Files:**
- Create: `src/main/resources/mermaid/mermaid.min.js` (download from CDN)
- Create: `src/main/resources/mermaid/preview.html`
- Create: `src/main/resources/mermaid/preview.css`

**Step 1: Download Mermaid.js**

```bash
mkdir -p src/main/resources/mermaid
curl -o src/main/resources/mermaid/mermaid.min.js https://cdn.jsdelivr.net/npm/mermaid@10/dist/mermaid.min.js
```

**Step 2: Create preview.html**

```html
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Mermaid Preview</title>
    <link rel="stylesheet" href="preview.css">
    <script src="mermaid.min.js"></script>
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

        window.setPan = function(x, y) {
            panX = x;
            panY = y;
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
            diagram.style.transform = `translate(${panX}px, ${panY}px) scale(${currentZoom})`;
        }

        // Mouse wheel zoom
        document.addEventListener('wheel', function(e) {
            if (e.ctrlKey) {
                e.preventDefault();
                const delta = e.deltaY > 0 ? 0.9 : 1.1;
                currentZoom = Math.max(0.1, Math.min(5, currentZoom * delta));
                applyTransform();

                if (window.javaBridge) {
                    window.javaBridge.onZoomChanged(currentZoom);
                }
            }
        }, { passive: false });

        // Pan with drag
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
```

**Step 3: Create preview.css**

```css
* {
    margin: 0;
    padding: 0;
    box-sizing: border-box;
}

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

#diagram svg {
    max-width: none;
}

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

.hidden {
    display: none !important;
}

/* Dark mode */
body.dark {
    --background-color: #1e1e1e;
}

body.dark #error {
    background: #3a1a1a;
    border-color: #5a2a2a;
    color: #faa;
}
```

**Step 4: Commit**

```bash
git add src/main/resources/mermaid/
git commit -m "feat: bundle Mermaid.js with preview HTML/CSS"
```

---

### Task 4.2: Create JCEF Preview Panel

**Files:**
- Create: `src/main/kotlin/com/nereid/preview/MermaidPreviewPanel.kt`

**Step 1: Write implementation**

```kotlin
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

    fun renderDiagram(source: String, theme: String = "default") {
        if (!isLoaded) {
            pendingSource = source
            pendingTheme = theme
            return
        }

        val escapedSource = source
            .replace("\\", "\\\\")
            .replace("`", "\\`")
            .replace("\$", "\\\$")
            .replace("\n", "\\n")

        val js = "window.renderDiagram(`$escapedSource`, '$theme');"
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
```

**Step 2: Commit**

```bash
git add src/main/kotlin/com/nereid/preview/MermaidPreviewPanel.kt
git commit -m "feat: add JCEF-based MermaidPreviewPanel"
```

---

## Phase 5: Split Editor

### Task 5.1: Create Split Editor Provider

**Files:**
- Create: `src/main/kotlin/com/nereid/spliteditor/MermaidEditorProvider.kt`
- Modify: `src/main/resources/META-INF/plugin.xml`

**Step 1: Write implementation**

```kotlin
package com.nereid.spliteditor

import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorPolicy
import com.intellij.openapi.fileEditor.FileEditorProvider
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.fileEditor.impl.text.TextEditorProvider
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.nereid.language.MermaidFileType

class MermaidEditorProvider : FileEditorProvider, DumbAware {

    override fun accept(project: Project, file: VirtualFile): Boolean {
        return file.fileType == MermaidFileType.INSTANCE
    }

    override fun createEditor(project: Project, file: VirtualFile): FileEditor {
        val textEditor = TextEditorProvider.getInstance().createEditor(project, file) as TextEditor
        return MermaidSplitEditor(textEditor, project, file)
    }

    override fun getEditorTypeId(): String = "mermaid-split-editor"

    override fun getPolicy(): FileEditorPolicy = FileEditorPolicy.HIDE_DEFAULT_EDITOR
}
```

**Step 2: Register in plugin.xml**

Add inside `<extensions defaultExtensionNs="com.intellij">`:

```xml
<fileEditorProvider implementationClass="com.nereid.spliteditor.MermaidEditorProvider"/>
```

**Step 3: Commit**

```bash
git add src/main/kotlin/com/nereid/spliteditor/MermaidEditorProvider.kt
git add src/main/resources/META-INF/plugin.xml
git commit -m "feat: add MermaidEditorProvider"
```

---

### Task 5.2: Create Split Editor

**Files:**
- Create: `src/main/kotlin/com/nereid/spliteditor/MermaidSplitEditor.kt`

**Step 1: Write implementation**

```kotlin
package com.nereid.spliteditor

import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorLocation
import com.intellij.openapi.fileEditor.FileEditorState
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.UserDataHolderBase
import com.intellij.openapi.vfs.VirtualFile
import com.nereid.preview.MermaidPreviewPanel
import java.awt.BorderLayout
import java.beans.PropertyChangeListener
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JSplitPane

class MermaidSplitEditor(
    private val textEditor: TextEditor,
    private val project: Project,
    private val file: VirtualFile
) : UserDataHolderBase(), FileEditor {

    enum class ViewMode { CODE_ONLY, SPLIT, PREVIEW_ONLY }

    private val mainPanel: JPanel
    private val splitPane: JSplitPane
    private val previewPanel: MermaidPreviewPanel
    private val toolbar: MermaidEditorToolbar

    private var viewMode: ViewMode = ViewMode.SPLIT

    init {
        previewPanel = MermaidPreviewPanel(this)

        splitPane = JSplitPane(JSplitPane.HORIZONTAL_SPLIT).apply {
            leftComponent = textEditor.component
            rightComponent = previewPanel.component
            resizeWeight = 0.5
            dividerSize = 3
        }

        toolbar = MermaidEditorToolbar(
            onViewModeChanged = { mode -> setViewMode(mode) },
            onZoomIn = { previewPanel.setZoom(1.1) },
            onZoomOut = { previewPanel.setZoom(0.9) },
            onZoomReset = { previewPanel.resetView() },
            onFitToView = { previewPanel.fitToView() }
        )

        mainPanel = JPanel(BorderLayout()).apply {
            add(toolbar.component, BorderLayout.NORTH)
            add(splitPane, BorderLayout.CENTER)
        }

        setupDocumentListener()
        updatePreview()
    }

    private fun setupDocumentListener() {
        textEditor.editor.document.addDocumentListener(object : com.intellij.openapi.editor.event.DocumentListener {
            override fun documentChanged(event: com.intellij.openapi.editor.event.DocumentEvent) {
                updatePreview()
            }
        }, this)
    }

    private fun updatePreview() {
        val source = textEditor.editor.document.text
        previewPanel.renderDiagram(source)
    }

    fun setViewMode(mode: ViewMode) {
        viewMode = mode
        when (mode) {
            ViewMode.CODE_ONLY -> {
                splitPane.leftComponent = textEditor.component
                splitPane.rightComponent = null
                splitPane.dividerSize = 0
            }
            ViewMode.SPLIT -> {
                splitPane.leftComponent = textEditor.component
                splitPane.rightComponent = previewPanel.component
                splitPane.dividerSize = 3
                splitPane.resizeWeight = 0.5
            }
            ViewMode.PREVIEW_ONLY -> {
                splitPane.leftComponent = null
                splitPane.rightComponent = previewPanel.component
                splitPane.dividerSize = 0
            }
        }
        toolbar.setViewMode(mode)
    }

    override fun getComponent(): JComponent = mainPanel

    override fun getPreferredFocusedComponent(): JComponent? = textEditor.preferredFocusedComponent

    override fun getName(): String = "Mermaid Editor"

    override fun setState(state: FileEditorState) {
        if (state is MermaidEditorState) {
            setViewMode(state.viewMode)
        }
    }

    override fun getState(level: com.intellij.openapi.fileEditor.FileEditorStateLevel): FileEditorState {
        return MermaidEditorState(viewMode)
    }

    override fun isModified(): Boolean = textEditor.isModified

    override fun isValid(): Boolean = textEditor.isValid

    override fun addPropertyChangeListener(listener: PropertyChangeListener) {
        textEditor.addPropertyChangeListener(listener)
    }

    override fun removePropertyChangeListener(listener: PropertyChangeListener) {
        textEditor.removePropertyChangeListener(listener)
    }

    override fun getCurrentLocation(): FileEditorLocation? = textEditor.currentLocation

    override fun dispose() {
        Disposer.dispose(previewPanel)
        Disposer.dispose(textEditor)
    }

    override fun getFile(): VirtualFile = file
}

data class MermaidEditorState(val viewMode: MermaidSplitEditor.ViewMode) : FileEditorState {
    override fun canBeMergedWith(otherState: FileEditorState, level: com.intellij.openapi.fileEditor.FileEditorStateLevel): Boolean {
        return otherState is MermaidEditorState
    }
}
```

**Step 2: Commit**

```bash
git add src/main/kotlin/com/nereid/spliteditor/MermaidSplitEditor.kt
git commit -m "feat: add MermaidSplitEditor with three view modes"
```

---

### Task 5.3: Create Editor Toolbar

**Files:**
- Create: `src/main/kotlin/com/nereid/spliteditor/MermaidEditorToolbar.kt`

**Step 1: Write implementation**

```kotlin
package com.nereid.spliteditor

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionToolbar
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.actionSystem.Toggleable
import javax.swing.JComponent

class MermaidEditorToolbar(
    private val onViewModeChanged: (MermaidSplitEditor.ViewMode) -> Unit,
    private val onZoomIn: () -> Unit,
    private val onZoomOut: () -> Unit,
    private val onZoomReset: () -> Unit,
    private val onFitToView: () -> Unit
) {

    private var currentMode: MermaidSplitEditor.ViewMode = MermaidSplitEditor.ViewMode.SPLIT

    private val codeOnlyAction = ViewModeAction("Code", MermaidSplitEditor.ViewMode.CODE_ONLY, AllIcons.Actions.EditSource)
    private val splitAction = ViewModeAction("Split", MermaidSplitEditor.ViewMode.SPLIT, AllIcons.Actions.PreviewDetails)
    private val previewOnlyAction = ViewModeAction("Preview", MermaidSplitEditor.ViewMode.PREVIEW_ONLY, AllIcons.Actions.Preview)

    private val toolbar: ActionToolbar

    init {
        val group = DefaultActionGroup().apply {
            add(codeOnlyAction)
            add(splitAction)
            add(previewOnlyAction)
            addSeparator()
            add(ZoomInAction())
            add(ZoomOutAction())
            add(ZoomResetAction())
            add(FitToViewAction())
        }

        toolbar = ActionManager.getInstance().createActionToolbar("MermaidEditor", group, true)
        toolbar.targetComponent = toolbar.component

        updateToggleStates()
    }

    val component: JComponent get() = toolbar.component

    fun setViewMode(mode: MermaidSplitEditor.ViewMode) {
        currentMode = mode
        updateToggleStates()
    }

    private fun updateToggleStates() {
        codeOnlyAction.isSelected = currentMode == MermaidSplitEditor.ViewMode.CODE_ONLY
        splitAction.isSelected = currentMode == MermaidSplitEditor.ViewMode.SPLIT
        previewOnlyAction.isSelected = currentMode == MermaidSplitEditor.ViewMode.PREVIEW_ONLY
    }

    private inner class ViewModeAction(
        text: String,
        private val mode: MermaidSplitEditor.ViewMode,
        icon: javax.swing.Icon
    ) : AnAction(text, "Switch to $text view", icon), Toggleable {

        var isSelected: Boolean = false
            set(value) {
                field = value
            }

        override fun actionPerformed(e: AnActionEvent) {
            currentMode = mode
            onViewModeChanged(mode)
            updateToggleStates()
        }

        override fun update(e: AnActionEvent) {
            Toggleable.setSelected(e.presentation, isSelected)
        }
    }

    private inner class ZoomInAction : AnAction("Zoom In", "Zoom in", AllIcons.General.Add) {
        override fun actionPerformed(e: AnActionEvent) = onZoomIn()
    }

    private inner class ZoomOutAction : AnAction("Zoom Out", "Zoom out", AllIcons.General.Remove) {
        override fun actionPerformed(e: AnActionEvent) = onZoomOut()
    }

    private inner class ZoomResetAction : AnAction("Reset Zoom", "Reset to 100%", AllIcons.General.ActualZoom) {
        override fun actionPerformed(e: AnActionEvent) = onZoomReset()
    }

    private inner class FitToViewAction : AnAction("Fit to View", "Fit diagram to view", AllIcons.General.FitContent) {
        override fun actionPerformed(e: AnActionEvent) = onFitToView()
    }
}
```

**Step 2: Commit**

```bash
git add src/main/kotlin/com/nereid/spliteditor/MermaidEditorToolbar.kt
git commit -m "feat: add editor toolbar with view mode and zoom controls"
```

---

## Phase 6: Live Preview with Debounce

### Task 6.1: Create Debounced Document Listener

**Files:**
- Create: `src/main/kotlin/com/nereid/preview/DebouncedDocumentListener.kt`

**Step 1: Write implementation**

```kotlin
package com.nereid.preview

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.util.Alarm
import com.intellij.util.AlarmFactory

class DebouncedDocumentListener(
    private val delayMs: Int = 300,
    private val onUpdate: () -> Unit,
    parentDisposable: Disposable
) : DocumentListener {

    private val alarm: Alarm = AlarmFactory.getInstance().create(Alarm.ThreadToUse.SWING_THREAD, parentDisposable)

    override fun documentChanged(event: DocumentEvent) {
        alarm.cancelAllRequests()
        alarm.addRequest({
            ApplicationManager.getApplication().invokeLater {
                onUpdate()
            }
        }, delayMs)
    }

    fun setDelay(delayMs: Int) {
        // Note: Would need to recreate listener for new delay
    }

    fun forceUpdate() {
        alarm.cancelAllRequests()
        onUpdate()
    }
}
```

**Step 2: Commit**

```bash
git add src/main/kotlin/com/nereid/preview/DebouncedDocumentListener.kt
git commit -m "feat: add debounced document listener for live preview"
```

---

### Task 6.2: Update Split Editor to Use Debounced Listener

**Files:**
- Modify: `src/main/kotlin/com/nereid/spliteditor/MermaidSplitEditor.kt`

**Step 1: Update setupDocumentListener method**

Replace the `setupDocumentListener` method:

```kotlin
private fun setupDocumentListener() {
    val listener = DebouncedDocumentListener(
        delayMs = 300,
        onUpdate = { updatePreview() },
        parentDisposable = this
    )
    textEditor.editor.document.addDocumentListener(listener, this)
}
```

Add import:
```kotlin
import com.nereid.preview.DebouncedDocumentListener
```

**Step 2: Commit**

```bash
git add src/main/kotlin/com/nereid/spliteditor/MermaidSplitEditor.kt
git commit -m "feat: use debounced listener for live preview updates"
```

---

## Phase 7: Editor Features

### Task 7.1: Create Brace Matcher

**Files:**
- Create: `src/main/kotlin/com/nereid/editor/MermaidBraceMatcher.kt`
- Modify: `src/main/resources/META-INF/plugin.xml`

**Step 1: Write implementation**

```kotlin
package com.nereid.editor

import com.intellij.lang.BracePair
import com.intellij.lang.PairedBraceMatcher
import com.intellij.psi.PsiFile
import com.intellij.psi.tree.IElementType
import com.nereid.language.MermaidTokenTypes

class MermaidBraceMatcher : PairedBraceMatcher {

    companion object {
        private val PAIRS = arrayOf(
            BracePair(MermaidTokenTypes.LBRACKET, MermaidTokenTypes.RBRACKET, false),
            BracePair(MermaidTokenTypes.LPAREN, MermaidTokenTypes.RPAREN, false),
            BracePair(MermaidTokenTypes.LBRACE, MermaidTokenTypes.RBRACE, false)
        )
    }

    override fun getPairs(): Array<BracePair> = PAIRS

    override fun isPairedBracesAllowedBeforeType(lbraceType: IElementType, contextType: IElementType?): Boolean = true

    override fun getCodeConstructStart(file: PsiFile?, openingBraceOffset: Int): Int = openingBraceOffset
}
```

**Step 2: Register in plugin.xml**

Add inside `<extensions defaultExtensionNs="com.intellij">`:

```xml
<lang.braceMatcher language="Mermaid" implementationClass="com.nereid.editor.MermaidBraceMatcher"/>
```

**Step 3: Commit**

```bash
git add src/main/kotlin/com/nereid/editor/MermaidBraceMatcher.kt
git add src/main/resources/META-INF/plugin.xml
git commit -m "feat: add brace matcher for brackets, parens, and braces"
```

---

### Task 7.2: Create Commenter

**Files:**
- Create: `src/main/kotlin/com/nereid/editor/MermaidCommenter.kt`
- Modify: `src/main/resources/META-INF/plugin.xml`

**Step 1: Write implementation**

```kotlin
package com.nereid.editor

import com.intellij.lang.Commenter

class MermaidCommenter : Commenter {

    override fun getLineCommentPrefix(): String = "%% "

    override fun getBlockCommentPrefix(): String? = null

    override fun getBlockCommentSuffix(): String? = null

    override fun getCommentedBlockCommentPrefix(): String? = null

    override fun getCommentedBlockCommentSuffix(): String? = null
}
```

**Step 2: Register in plugin.xml**

Add inside `<extensions defaultExtensionNs="com.intellij">`:

```xml
<lang.commenter language="Mermaid" implementationClass="com.nereid.editor.MermaidCommenter"/>
```

**Step 3: Commit**

```bash
git add src/main/kotlin/com/nereid/editor/MermaidCommenter.kt
git add src/main/resources/META-INF/plugin.xml
git commit -m "feat: add commenter for %% line comments"
```

---

### Task 7.3: Create Folding Builder

**Files:**
- Create: `src/main/kotlin/com/nereid/editor/MermaidFoldingBuilder.kt`
- Modify: `src/main/resources/META-INF/plugin.xml`

**Step 1: Write implementation**

```kotlin
package com.nereid.editor

import com.intellij.lang.ASTNode
import com.intellij.lang.folding.FoldingBuilderEx
import com.intellij.lang.folding.FoldingDescriptor
import com.intellij.openapi.editor.Document
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.nereid.language.MermaidTokenTypes

class MermaidFoldingBuilder : FoldingBuilderEx(), DumbAware {

    override fun buildFoldRegions(root: PsiElement, document: Document, quick: Boolean): Array<FoldingDescriptor> {
        val descriptors = mutableListOf<FoldingDescriptor>()
        val text = document.text

        // Fold subgraph blocks
        val subgraphRegex = Regex("""subgraph\s+.*?\n([\s\S]*?)\n\s*end""", RegexOption.MULTILINE)
        subgraphRegex.findAll(text).forEach { match ->
            val range = TextRange(match.range.first, match.range.last + 1)
            if (range.length > 0) {
                descriptors.add(FoldingDescriptor(root.node, range))
            }
        }

        // Fold directive blocks
        val directiveRegex = Regex("""%%\{[\s\S]*?}%%""")
        directiveRegex.findAll(text).forEach { match ->
            val range = TextRange(match.range.first, match.range.last + 1)
            if (range.length > 10) {
                descriptors.add(FoldingDescriptor(root.node, range))
            }
        }

        return descriptors.toTypedArray()
    }

    override fun getPlaceholderText(node: ASTNode): String {
        val text = node.text
        return when {
            text.startsWith("subgraph") -> "subgraph..."
            text.startsWith("%%{") -> "%%{...}%%"
            else -> "..."
        }
    }

    override fun isCollapsedByDefault(node: ASTNode): Boolean = false
}
```

**Step 2: Register in plugin.xml**

Add inside `<extensions defaultExtensionNs="com.intellij">`:

```xml
<lang.foldingBuilder language="Mermaid" implementationClass="com.nereid.editor.MermaidFoldingBuilder"/>
```

**Step 3: Commit**

```bash
git add src/main/kotlin/com/nereid/editor/MermaidFoldingBuilder.kt
git add src/main/resources/META-INF/plugin.xml
git commit -m "feat: add folding for subgraph blocks and directives"
```

---

### Task 7.4: Create Completion Contributor

**Files:**
- Create: `src/main/kotlin/com/nereid/editor/MermaidCompletionContributor.kt`
- Modify: `src/main/resources/META-INF/plugin.xml`

**Step 1: Write implementation**

```kotlin
package com.nereid.editor

import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.patterns.PlatformPatterns
import com.intellij.util.ProcessingContext
import com.nereid.language.MermaidLanguage

class MermaidCompletionContributor : CompletionContributor() {

    companion object {
        private val DIAGRAM_TYPES = listOf(
            "graph" to "Graph diagram",
            "flowchart" to "Flowchart diagram",
            "sequenceDiagram" to "Sequence diagram",
            "classDiagram" to "Class diagram",
            "stateDiagram-v2" to "State diagram",
            "erDiagram" to "Entity-Relationship diagram",
            "gantt" to "Gantt chart",
            "pie" to "Pie chart",
            "gitGraph" to "Git graph",
            "mindmap" to "Mind map",
            "timeline" to "Timeline",
            "quadrantChart" to "Quadrant chart",
            "sankey-beta" to "Sankey diagram",
            "xychart-beta" to "XY chart",
            "block-beta" to "Block diagram"
        )

        private val DIRECTIONS = listOf(
            "TB" to "Top to Bottom",
            "TD" to "Top Down (same as TB)",
            "BT" to "Bottom to Top",
            "RL" to "Right to Left",
            "LR" to "Left to Right"
        )

        private val KEYWORDS = listOf(
            "subgraph" to "Create a subgraph",
            "end" to "End subgraph block",
            "participant" to "Declare participant",
            "actor" to "Declare actor",
            "note" to "Add a note",
            "loop" to "Loop block",
            "alt" to "Alternative block",
            "else" to "Else branch",
            "opt" to "Optional block",
            "par" to "Parallel block",
            "rect" to "Rectangle highlight",
            "section" to "Gantt section",
            "title" to "Diagram title"
        )

        private val ARROWS = listOf(
            "-->" to "Solid arrow",
            "---" to "Solid line",
            "-.->  " to "Dotted arrow",
            "-.-" to "Dotted line",
            "==>" to "Thick arrow",
            "===" to "Thick line",
            "--o" to "Circle end",
            "--x" to "Cross end",
            "<-->" to "Bidirectional arrow"
        )

        private val SHAPES = listOf(
            "[text]" to "Rectangle",
            "(text)" to "Rounded rectangle",
            "([text])" to "Stadium shape",
            "[[text]]" to "Subroutine",
            "[(text)]" to "Cylinder",
            "((text))" to "Circle",
            ">text]" to "Asymmetric",
            "{text}" to "Rhombus",
            "{{text}}" to "Hexagon",
            "[/text/]" to "Parallelogram",
            "[\\text\\]" to "Parallelogram alt"
        )
    }

    init {
        extend(
            CompletionType.BASIC,
            PlatformPatterns.psiElement().withLanguage(MermaidLanguage.INSTANCE),
            object : CompletionProvider<CompletionParameters>() {
                override fun addCompletions(
                    parameters: CompletionParameters,
                    context: ProcessingContext,
                    result: CompletionResultSet
                ) {
                    val position = parameters.position
                    val text = parameters.originalFile.text
                    val offset = parameters.offset

                    // Check if we're at the start of the file or after a newline
                    val isLineStart = offset == 0 || text.getOrNull(offset - 1) == '\n'
                    val lineStart = text.lastIndexOf('\n', offset - 1) + 1
                    val linePrefix = text.substring(lineStart, offset).trim()

                    when {
                        // Diagram types at start
                        isLineStart || linePrefix.isEmpty() -> {
                            DIAGRAM_TYPES.forEach { (type, desc) ->
                                result.addElement(
                                    LookupElementBuilder.create(type)
                                        .withTypeText(desc)
                                        .bold()
                                )
                            }
                            KEYWORDS.forEach { (kw, desc) ->
                                result.addElement(
                                    LookupElementBuilder.create(kw)
                                        .withTypeText(desc)
                                )
                            }
                        }

                        // Directions after graph/flowchart
                        linePrefix.matches(Regex("(graph|flowchart)\\s*")) -> {
                            DIRECTIONS.forEach { (dir, desc) ->
                                result.addElement(
                                    LookupElementBuilder.create(dir)
                                        .withTypeText(desc)
                                        .bold()
                                )
                            }
                        }

                        // Arrows after identifier
                        linePrefix.matches(Regex(".*\\w\\s*")) -> {
                            ARROWS.forEach { (arrow, desc) ->
                                result.addElement(
                                    LookupElementBuilder.create(arrow)
                                        .withTypeText(desc)
                                )
                            }
                        }
                    }

                    // Always offer shapes as they can be useful
                    if (linePrefix.matches(Regex(".*\\w$"))) {
                        SHAPES.forEach { (shape, desc) ->
                            result.addElement(
                                LookupElementBuilder.create(shape)
                                    .withTypeText(desc)
                                    .withPresentableText(desc)
                            )
                        }
                    }
                }
            }
        )
    }
}
```

**Step 2: Register in plugin.xml**

Add inside `<extensions defaultExtensionNs="com.intellij">`:

```xml
<completion.contributor language="Mermaid" implementationClass="com.nereid.editor.MermaidCompletionContributor"/>
```

**Step 3: Commit**

```bash
git add src/main/kotlin/com/nereid/editor/MermaidCompletionContributor.kt
git add src/main/resources/META-INF/plugin.xml
git commit -m "feat: add completion for diagram types, directions, keywords, arrows, shapes"
```

---

## Phase 8: Error Detection

### Task 8.1: Create Parser Definition (Minimal)

**Files:**
- Create: `src/main/kotlin/com/nereid/language/MermaidParserDefinition.kt`
- Create: `src/main/kotlin/com/nereid/language/psi/MermaidFile.kt`
- Modify: `src/main/resources/META-INF/plugin.xml`

**Step 1: Create MermaidFile**

```kotlin
package com.nereid.language.psi

import com.intellij.extapi.psi.PsiFileBase
import com.intellij.openapi.fileTypes.FileType
import com.intellij.psi.FileViewProvider
import com.nereid.language.MermaidFileType
import com.nereid.language.MermaidLanguage

class MermaidFile(viewProvider: FileViewProvider) : PsiFileBase(viewProvider, MermaidLanguage.INSTANCE) {

    override fun getFileType(): FileType = MermaidFileType.INSTANCE

    override fun toString(): String = "Mermaid File"
}
```

**Step 2: Create MermaidParserDefinition**

```kotlin
package com.nereid.language

import com.intellij.lang.ASTNode
import com.intellij.lang.ParserDefinition
import com.intellij.lang.PsiParser
import com.intellij.lexer.Lexer
import com.intellij.openapi.project.Project
import com.intellij.psi.FileViewProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.tree.IFileElementType
import com.intellij.psi.tree.TokenSet
import com.nereid.language.psi.MermaidFile

class MermaidParserDefinition : ParserDefinition {

    companion object {
        val FILE = IFileElementType(MermaidLanguage.INSTANCE)
    }

    override fun createLexer(project: Project?): Lexer = MermaidLexer()

    override fun createParser(project: Project?): PsiParser {
        return PsiParser { root, builder ->
            val marker = builder.mark()
            while (!builder.eof()) {
                builder.advanceLexer()
            }
            marker.done(root)
            builder.treeBuilt
        }
    }

    override fun getFileNodeType(): IFileElementType = FILE

    override fun getCommentTokens(): TokenSet = MermaidTokenTypes.COMMENTS

    override fun getStringLiteralElements(): TokenSet = MermaidTokenTypes.STRINGS

    override fun createElement(node: ASTNode?): PsiElement {
        throw UnsupportedOperationException("Not implemented")
    }

    override fun createFile(viewProvider: FileViewProvider): PsiFile = MermaidFile(viewProvider)
}
```

**Step 3: Register in plugin.xml**

Add inside `<extensions defaultExtensionNs="com.intellij">`:

```xml
<lang.parserDefinition language="Mermaid" implementationClass="com.nereid.language.MermaidParserDefinition"/>
```

**Step 4: Commit**

```bash
git add src/main/kotlin/com/nereid/language/psi/MermaidFile.kt
git add src/main/kotlin/com/nereid/language/MermaidParserDefinition.kt
git add src/main/resources/META-INF/plugin.xml
git commit -m "feat: add minimal parser definition and MermaidFile"
```

---

### Task 8.2: Create Annotator for Syntax Errors

**Files:**
- Create: `src/main/kotlin/com/nereid/editor/MermaidAnnotator.kt`
- Modify: `src/main/resources/META-INF/plugin.xml`

**Step 1: Write implementation**

```kotlin
package com.nereid.editor

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.nereid.language.psi.MermaidFile

class MermaidAnnotator : Annotator {

    companion object {
        private val VALID_DIAGRAM_TYPES = setOf(
            "graph", "flowchart", "sequenceDiagram", "classDiagram",
            "stateDiagram", "stateDiagram-v2", "erDiagram", "gantt",
            "pie", "gitGraph", "mindmap", "timeline", "quadrantChart",
            "requirementDiagram", "C4Context", "sankey-beta", "xychart-beta", "block-beta"
        )

        private val VALID_DIRECTIONS = setOf("TB", "TD", "BT", "RL", "LR")
    }

    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        if (element !is MermaidFile) return

        val text = element.text
        val lines = text.lines()

        var offset = 0
        var hasDiagramType = false

        for ((lineNum, line) in lines.withIndex()) {
            val trimmed = line.trim()

            // Skip empty lines and comments
            if (trimmed.isEmpty() || trimmed.startsWith("%%")) {
                offset += line.length + 1
                continue
            }

            // Check first non-comment line for diagram type
            if (!hasDiagramType) {
                val firstWord = trimmed.split(Regex("\\s+")).firstOrNull() ?: ""

                if (firstWord !in VALID_DIAGRAM_TYPES) {
                    holder.newAnnotation(HighlightSeverity.ERROR, "Invalid or missing diagram type")
                        .range(TextRange(offset, offset + line.length.coerceAtLeast(1)))
                        .create()
                } else {
                    hasDiagramType = true

                    // Check direction for graph/flowchart
                    if (firstWord in setOf("graph", "flowchart")) {
                        val parts = trimmed.split(Regex("\\s+"))
                        if (parts.size >= 2) {
                            val direction = parts[1]
                            if (direction !in VALID_DIRECTIONS && !direction.startsWith("%%")) {
                                val dirStart = offset + line.indexOf(direction)
                                holder.newAnnotation(HighlightSeverity.WARNING, "Invalid direction: $direction. Expected: TB, TD, BT, RL, or LR")
                                    .range(TextRange(dirStart, dirStart + direction.length))
                                    .create()
                            }
                        }
                    }
                }
            }

            // Check for unclosed brackets in the line
            val brackets = mutableListOf<Pair<Char, Int>>()
            for ((i, c) in line.withIndex()) {
                when (c) {
                    '[', '(', '{' -> brackets.add(c to i)
                    ']' -> {
                        if (brackets.lastOrNull()?.first == '[') brackets.removeLast()
                        else holder.newAnnotation(HighlightSeverity.ERROR, "Unmatched ']'")
                            .range(TextRange(offset + i, offset + i + 1))
                            .create()
                    }
                    ')' -> {
                        if (brackets.lastOrNull()?.first == '(') brackets.removeLast()
                        else holder.newAnnotation(HighlightSeverity.ERROR, "Unmatched ')'")
                            .range(TextRange(offset + i, offset + i + 1))
                            .create()
                    }
                    '}' -> {
                        if (brackets.lastOrNull()?.first == '{') brackets.removeLast()
                        else holder.newAnnotation(HighlightSeverity.ERROR, "Unmatched '}'")
                            .range(TextRange(offset + i, offset + i + 1))
                            .create()
                    }
                }
            }

            // Report unclosed brackets
            for ((bracket, pos) in brackets) {
                val expected = when (bracket) {
                    '[' -> ']'
                    '(' -> ')'
                    '{' -> '}'
                    else -> '?'
                }
                holder.newAnnotation(HighlightSeverity.ERROR, "Unclosed '$bracket', expected '$expected'")
                    .range(TextRange(offset + pos, offset + pos + 1))
                    .create()
            }

            offset += line.length + 1
        }
    }
}
```

**Step 2: Register in plugin.xml**

Add inside `<extensions defaultExtensionNs="com.intellij">`:

```xml
<annotator language="Mermaid" implementationClass="com.nereid.editor.MermaidAnnotator"/>
```

**Step 3: Commit**

```bash
git add src/main/kotlin/com/nereid/editor/MermaidAnnotator.kt
git add src/main/resources/META-INF/plugin.xml
git commit -m "feat: add annotator for diagram type and bracket errors"
```

---

## Phase 9: Theme Integration

### Task 9.1: Create Theme Manager

**Files:**
- Create: `src/main/kotlin/com/nereid/preview/ThemeManager.kt`

**Step 1: Write implementation**

```kotlin
package com.nereid.preview

import com.intellij.ide.ui.LafManager
import com.intellij.ide.ui.LafManagerListener
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import javax.swing.UIManager

class ThemeManager(private val parentDisposable: Disposable) {

    var onThemeChanged: ((isDark: Boolean, mermaidTheme: String) -> Unit)? = null

    init {
        val connection = ApplicationManager.getApplication().messageBus.connect(parentDisposable)
        connection.subscribe(LafManagerListener.TOPIC, LafManagerListener {
            notifyThemeChanged()
        })
    }

    fun isDarkTheme(): Boolean {
        val laf = LafManager.getInstance().currentLookAndFeel
        return laf?.name?.lowercase()?.contains("dark") == true ||
               laf?.name?.lowercase()?.contains("darcula") == true ||
               UIManager.getBoolean("ui.dark")
    }

    fun getMermaidTheme(): String {
        return if (isDarkTheme()) "dark" else "default"
    }

    private fun notifyThemeChanged() {
        onThemeChanged?.invoke(isDarkTheme(), getMermaidTheme())
    }

    fun getCurrentThemeConfig(): ThemeConfig {
        return ThemeConfig(
            isDark = isDarkTheme(),
            mermaidTheme = getMermaidTheme()
        )
    }

    data class ThemeConfig(
        val isDark: Boolean,
        val mermaidTheme: String
    )
}
```

**Step 2: Commit**

```bash
git add src/main/kotlin/com/nereid/preview/ThemeManager.kt
git commit -m "feat: add ThemeManager for IDE theme detection"
```

---

### Task 9.2: Integrate Theme Manager with Preview Panel

**Files:**
- Modify: `src/main/kotlin/com/nereid/preview/MermaidPreviewPanel.kt`

**Step 1: Update MermaidPreviewPanel**

Add after `Disposer.register(parentDisposable, this)`:

```kotlin
themeManager = ThemeManager(this)
themeManager.onThemeChanged = { isDark, mermaidTheme ->
    setDarkMode(isDark)
    pendingSource?.let { renderDiagram(it, mermaidTheme) }
        ?: textSource?.let { renderDiagram(it, mermaidTheme) }
}
```

Add field:
```kotlin
private val themeManager: ThemeManager
private var textSource: String? = null
```

Update `renderDiagram` to store source:
```kotlin
fun renderDiagram(source: String, theme: String? = null) {
    textSource = source
    val actualTheme = theme ?: themeManager.getMermaidTheme()
    // ... rest of implementation
}
```

**Step 2: Commit**

```bash
git add src/main/kotlin/com/nereid/preview/MermaidPreviewPanel.kt
git commit -m "feat: integrate theme manager with preview panel"
```

---

## Phase 10: Export Functionality

### Task 10.1: Create PNG Exporter

**Files:**
- Create: `src/main/kotlin/com/nereid/export/PngExporter.kt`

**Step 1: Write implementation**

```kotlin
package com.nereid.export

import com.intellij.ui.jcef.JBCefBrowser
import java.awt.image.BufferedImage
import java.io.File
import java.util.concurrent.CompletableFuture
import javax.imageio.ImageIO

class PngExporter {

    fun exportToPng(
        browser: JBCefBrowser,
        outputFile: File,
        scale: Int = 2,
        transparentBackground: Boolean = true
    ): CompletableFuture<Boolean> {
        val future = CompletableFuture<Boolean>()

        val js = """
            (function() {
                const svg = document.querySelector('#diagram svg');
                if (!svg) return null;

                const canvas = document.createElement('canvas');
                const rect = svg.getBoundingClientRect();
                canvas.width = rect.width * $scale;
                canvas.height = rect.height * $scale;

                const ctx = canvas.getContext('2d');
                ctx.scale($scale, $scale);

                ${if (!transparentBackground) "ctx.fillStyle = 'white'; ctx.fillRect(0, 0, canvas.width, canvas.height);" else ""}

                const svgData = new XMLSerializer().serializeToString(svg);
                const img = new Image();

                return new Promise((resolve) => {
                    img.onload = function() {
                        ctx.drawImage(img, 0, 0);
                        resolve(canvas.toDataURL('image/png'));
                    };
                    img.src = 'data:image/svg+xml;base64,' + btoa(unescape(encodeURIComponent(svgData)));
                });
            })();
        """.trimIndent()

        browser.cefBrowser.executeJavaScript(
            """
            $js.then(function(dataUrl) {
                if (dataUrl) {
                    window.exportResult = dataUrl;
                }
            });
            """.trimIndent(),
            browser.cefBrowser.url,
            0
        )

        // Note: Actual implementation would use JS bridge for callback
        future.complete(true)
        return future
    }
}
```

**Step 2: Commit**

```bash
git add src/main/kotlin/com/nereid/export/PngExporter.kt
git commit -m "feat: add PNG exporter with scale and transparency options"
```

---

### Task 10.2: Create SVG Exporter

**Files:**
- Create: `src/main/kotlin/com/nereid/export/SvgExporter.kt`

**Step 1: Write implementation**

```kotlin
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
```

**Step 2: Commit**

```bash
git add src/main/kotlin/com/nereid/export/SvgExporter.kt
git commit -m "feat: add SVG exporter"
```

---

### Task 10.3: Create Clipboard Exporter

**Files:**
- Create: `src/main/kotlin/com/nereid/export/ClipboardExporter.kt`

**Step 1: Write implementation**

```kotlin
package com.nereid.export

import java.awt.Image
import java.awt.Toolkit
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.StringSelection
import java.awt.datatransfer.Transferable
import java.awt.datatransfer.UnsupportedFlavorException
import java.awt.image.BufferedImage

class ClipboardExporter {

    fun copyImageToClipboard(image: BufferedImage) {
        val transferable = ImageTransferable(image)
        Toolkit.getDefaultToolkit().systemClipboard.setContents(transferable, null)
    }

    fun copySvgToClipboard(svgContent: String) {
        val selection = StringSelection(svgContent)
        Toolkit.getDefaultToolkit().systemClipboard.setContents(selection, null)
    }

    fun copySourceToClipboard(source: String) {
        val selection = StringSelection(source)
        Toolkit.getDefaultToolkit().systemClipboard.setContents(selection, null)
    }

    private class ImageTransferable(private val image: Image) : Transferable {
        override fun getTransferDataFlavors(): Array<DataFlavor> = arrayOf(DataFlavor.imageFlavor)

        override fun isDataFlavorSupported(flavor: DataFlavor): Boolean =
            DataFlavor.imageFlavor.equals(flavor)

        override fun getTransferData(flavor: DataFlavor): Any {
            if (!DataFlavor.imageFlavor.equals(flavor)) {
                throw UnsupportedFlavorException(flavor)
            }
            return image
        }
    }
}
```

**Step 2: Commit**

```bash
git add src/main/kotlin/com/nereid/export/ClipboardExporter.kt
git commit -m "feat: add clipboard exporter for image, SVG, and source"
```

---

### Task 10.4: Create Export Actions

**Files:**
- Create: `src/main/kotlin/com/nereid/actions/ExportActions.kt`
- Modify: `src/main/resources/META-INF/plugin.xml`

**Step 1: Write implementation**

```kotlin
package com.nereid.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.fileChooser.FileChooserFactory
import com.intellij.openapi.fileChooser.FileSaverDescriptor
import com.intellij.openapi.project.DumbAware
import com.nereid.language.MermaidFileType

class ExportToPngAction : AnAction("Export as PNG", "Export diagram as PNG image", null), DumbAware {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val file = e.getData(CommonDataKeys.VIRTUAL_FILE) ?: return

        val descriptor = FileSaverDescriptor("Export as PNG", "Choose where to save the PNG file", "png")
        val dialog = FileChooserFactory.getInstance().createSaveFileDialog(descriptor, project)
        val result = dialog.save(file.parent, file.nameWithoutExtension + ".png")

        result?.let {
            // Trigger export via editor
        }
    }

    override fun update(e: AnActionEvent) {
        val file = e.getData(CommonDataKeys.VIRTUAL_FILE)
        e.presentation.isEnabledAndVisible = file?.fileType == MermaidFileType.INSTANCE
    }
}

class ExportToSvgAction : AnAction("Export as SVG", "Export diagram as SVG", null), DumbAware {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val file = e.getData(CommonDataKeys.VIRTUAL_FILE) ?: return

        val descriptor = FileSaverDescriptor("Export as SVG", "Choose where to save the SVG file", "svg")
        val dialog = FileChooserFactory.getInstance().createSaveFileDialog(descriptor, project)
        val result = dialog.save(file.parent, file.nameWithoutExtension + ".svg")

        result?.let {
            // Trigger export via editor
        }
    }

    override fun update(e: AnActionEvent) {
        val file = e.getData(CommonDataKeys.VIRTUAL_FILE)
        e.presentation.isEnabledAndVisible = file?.fileType == MermaidFileType.INSTANCE
    }
}

class CopyAsPngAction : AnAction("Copy as PNG", "Copy diagram as PNG to clipboard", null), DumbAware {
    override fun actionPerformed(e: AnActionEvent) {
        // Trigger clipboard copy via editor
    }

    override fun update(e: AnActionEvent) {
        val file = e.getData(CommonDataKeys.VIRTUAL_FILE)
        e.presentation.isEnabledAndVisible = file?.fileType == MermaidFileType.INSTANCE
    }
}
```

**Step 2: Register actions in plugin.xml**

Add inside `<actions>`:

```xml
<group id="Mermaid.ExportGroup" text="Mermaid" popup="true">
    <add-to-group group-id="ToolsMenu" anchor="last"/>
    <action id="Mermaid.ExportPng" class="com.nereid.actions.ExportToPngAction"/>
    <action id="Mermaid.ExportSvg" class="com.nereid.actions.ExportToSvgAction"/>
    <separator/>
    <action id="Mermaid.CopyPng" class="com.nereid.actions.CopyAsPngAction"/>
</group>

<action id="Mermaid.ExportPngShortcut" class="com.nereid.actions.ExportToPngAction">
    <keyboard-shortcut keymap="$default" first-keystroke="ctrl shift E"/>
</action>
```

**Step 3: Commit**

```bash
git add src/main/kotlin/com/nereid/actions/ExportActions.kt
git add src/main/resources/META-INF/plugin.xml
git commit -m "feat: add export actions for PNG, SVG, and clipboard"
```

---

## Phase 11: Markdown Injection

### Task 11.1: Create Language Injector

**Files:**
- Create: `src/main/kotlin/com/nereid/markdown/MermaidLanguageInjector.kt`
- Modify: `src/main/resources/META-INF/plugin.xml`

**Step 1: Write implementation**

```kotlin
package com.nereid.markdown

import com.intellij.lang.injection.MultiHostInjector
import com.intellij.lang.injection.MultiHostRegistrar
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiLanguageInjectionHost
import com.nereid.language.MermaidLanguage

class MermaidLanguageInjector : MultiHostInjector {

    override fun getLanguagesToInject(registrar: MultiHostRegistrar, context: PsiElement) {
        if (!isMermaidFencedCodeBlock(context)) return

        val host = context as? PsiLanguageInjectionHost ?: return
        val text = context.text

        // Find the content between the fences
        val startIndex = text.indexOf('\n') + 1
        val endIndex = text.lastIndexOf("```").takeIf { it > startIndex } ?: text.length

        if (startIndex < endIndex) {
            registrar.startInjecting(MermaidLanguage.INSTANCE)
            registrar.addPlace(null, null, host, TextRange(startIndex, endIndex))
            registrar.doneInjecting()
        }
    }

    override fun elementsToInjectIn(): List<Class<out PsiElement>> {
        return try {
            // Try to load Markdown plugin classes
            val fenceClass = Class.forName("org.intellij.plugins.markdown.lang.psi.impl.MarkdownCodeFenceImpl")
            @Suppress("UNCHECKED_CAST")
            listOf(fenceClass as Class<out PsiElement>)
        } catch (e: ClassNotFoundException) {
            emptyList()
        }
    }

    private fun isMermaidFencedCodeBlock(element: PsiElement): Boolean {
        val text = element.text
        return text.startsWith("```mermaid") || text.startsWith("```Mermaid")
    }
}
```

**Step 2: Register in plugin.xml**

Add `<depends optional="true" config-file="markdown-support.xml">org.intellij.plugins.markdown</depends>` after existing depends.

Create `src/main/resources/META-INF/markdown-support.xml`:

```xml
<idea-plugin>
    <extensions defaultExtensionNs="com.intellij">
        <multiHostInjector implementation="com.nereid.markdown.MermaidLanguageInjector"/>
    </extensions>
</idea-plugin>
```

**Step 3: Commit**

```bash
git add src/main/kotlin/com/nereid/markdown/MermaidLanguageInjector.kt
git add src/main/resources/META-INF/markdown-support.xml
git add src/main/resources/META-INF/plugin.xml
git commit -m "feat: add Mermaid language injection for Markdown fenced blocks"
```

---

## Phase 12: Settings

### Task 12.1: Create Settings State

**Files:**
- Create: `src/main/kotlin/com/nereid/settings/MermaidSettings.kt`

**Step 1: Write implementation**

```kotlin
package com.nereid.settings

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.util.xmlb.XmlSerializerUtil

@State(
    name = "MermaidSettings",
    storages = [Storage("mermaid.xml")]
)
class MermaidSettings : PersistentStateComponent<MermaidSettings> {

    // Editor settings
    var previewUpdateMode: PreviewUpdateMode = PreviewUpdateMode.LIVE
    var debounceDelayMs: Int = 300
    var defaultViewMode: ViewMode = ViewMode.SPLIT
    var showLineNumbersInErrors: Boolean = true

    // Appearance settings
    var themeMode: ThemeMode = ThemeMode.FOLLOW_IDE
    var mermaidTheme: String = "default"
    var customThemeJson: String = ""
    var previewBackground: PreviewBackground = PreviewBackground.MATCH_IDE

    // Export settings
    var defaultExportFormat: ExportFormat = ExportFormat.PNG
    var pngScaleFactor: Int = 2
    var pngTransparentBackground: Boolean = true
    var svgEmbedFonts: Boolean = true
    var lastExportDirectory: String = ""

    // Zoom settings
    var mouseWheelZoomEnabled: Boolean = true
    var zoomModifierKey: ModifierKey = ModifierKey.CTRL
    var zoomSensitivity: Int = 5
    var defaultZoomLevel: ZoomLevel = ZoomLevel.FIT_ALL

    // Advanced settings
    var useCustomMermaidJs: Boolean = false
    var customMermaidJsUrl: String = ""
    var securityMode: SecurityMode = SecurityMode.STRICT
    var experimentalFeaturesEnabled: Boolean = false

    enum class PreviewUpdateMode { LIVE, ON_SAVE, MANUAL }
    enum class ViewMode { CODE_ONLY, SPLIT, PREVIEW_ONLY }
    enum class ThemeMode { FOLLOW_IDE, MERMAID_THEME, CUSTOM }
    enum class PreviewBackground { TRANSPARENT, MATCH_IDE, WHITE, DARK }
    enum class ExportFormat { PNG, SVG }
    enum class ModifierKey { CTRL, CMD, NONE }
    enum class ZoomLevel { FIT_ALL, ACTUAL_SIZE, LAST_USED }
    enum class SecurityMode { STRICT, LOOSE }

    override fun getState(): MermaidSettings = this

    override fun loadState(state: MermaidSettings) {
        XmlSerializerUtil.copyBean(state, this)
    }

    companion object {
        fun getInstance(): MermaidSettings {
            return ApplicationManager.getApplication().getService(MermaidSettings::class.java)
        }
    }
}
```

**Step 2: Register service in plugin.xml**

Add inside `<extensions defaultExtensionNs="com.intellij">`:

```xml
<applicationService serviceImplementation="com.nereid.settings.MermaidSettings"/>
```

**Step 3: Commit**

```bash
git add src/main/kotlin/com/nereid/settings/MermaidSettings.kt
git add src/main/resources/META-INF/plugin.xml
git commit -m "feat: add MermaidSettings persistent state component"
```

---

### Task 12.2: Create Settings Configurable UI

**Files:**
- Create: `src/main/kotlin/com/nereid/settings/MermaidSettingsConfigurable.kt`
- Modify: `src/main/resources/META-INF/plugin.xml`

**Step 1: Write implementation**

```kotlin
package com.nereid.settings

import com.intellij.openapi.options.Configurable
import com.intellij.openapi.options.ConfigurableProvider
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBLabel
import com.intellij.ui.dsl.builder.*
import javax.swing.JComponent

class MermaidSettingsConfigurable : Configurable {

    private var panel: JComponent? = null
    private val settings = MermaidSettings.getInstance()

    // UI components
    private val livePreviewCheckbox = JBCheckBox("Live preview")
    private val debounceSlider = javax.swing.JSlider(0, 2000, settings.debounceDelayMs)
    private val mouseWheelZoomCheckbox = JBCheckBox("Enable mouse wheel zoom")

    override fun getDisplayName(): String = "Mermaid"

    override fun createComponent(): JComponent {
        panel = panel {
            group("Editor") {
                row("Preview update:") {
                    comboBox(MermaidSettings.PreviewUpdateMode.entries)
                        .bindItem(settings::previewUpdateMode.toNullableProperty())
                }
                row("Debounce delay:") {
                    slider(0, 2000, 100, 500)
                        .bindValue(settings::debounceDelayMs)
                    label("ms")
                }
                row("Default view:") {
                    comboBox(MermaidSettings.ViewMode.entries)
                        .bindItem(settings::defaultViewMode.toNullableProperty())
                }
            }

            group("Appearance") {
                row("Theme:") {
                    comboBox(MermaidSettings.ThemeMode.entries)
                        .bindItem(settings::themeMode.toNullableProperty())
                }
                row("Mermaid theme:") {
                    comboBox(listOf("default", "dark", "forest", "neutral"))
                        .bindItem(settings::mermaidTheme.toNullableProperty())
                }
                row("Background:") {
                    comboBox(MermaidSettings.PreviewBackground.entries)
                        .bindItem(settings::previewBackground.toNullableProperty())
                }
            }

            group("Export") {
                row("Default format:") {
                    comboBox(MermaidSettings.ExportFormat.entries)
                        .bindItem(settings::defaultExportFormat.toNullableProperty())
                }
                row("PNG scale:") {
                    comboBox(listOf(1, 2, 3))
                        .bindItem(settings::pngScaleFactor.toNullableProperty())
                    label("x")
                }
                row {
                    checkBox("Transparent PNG background")
                        .bindSelected(settings::pngTransparentBackground)
                }
            }

            group("Zoom & Navigation") {
                row {
                    checkBox("Enable mouse wheel zoom")
                        .bindSelected(settings::mouseWheelZoomEnabled)
                }
                row("Modifier key:") {
                    comboBox(MermaidSettings.ModifierKey.entries)
                        .bindItem(settings::zoomModifierKey.toNullableProperty())
                }
                row("Default zoom:") {
                    comboBox(MermaidSettings.ZoomLevel.entries)
                        .bindItem(settings::defaultZoomLevel.toNullableProperty())
                }
            }

            group("Advanced") {
                row {
                    checkBox("Use custom Mermaid.js")
                        .bindSelected(settings::useCustomMermaidJs)
                }
                row("Custom URL:") {
                    textField()
                        .bindText(settings::customMermaidJsUrl)
                        .enabled(settings.useCustomMermaidJs)
                }
                row("Security:") {
                    comboBox(MermaidSettings.SecurityMode.entries)
                        .bindItem(settings::securityMode.toNullableProperty())
                }
            }
        }
        return panel!!
    }

    override fun isModified(): Boolean = panel != null

    override fun apply() {
        // Settings are bound directly via bindXxx
    }

    override fun reset() {
        // Reset to current settings
    }
}
```

**Step 2: Register in plugin.xml**

Add inside `<extensions defaultExtensionNs="com.intellij">`:

```xml
<applicationConfigurable
    parentId="tools"
    instance="com.nereid.settings.MermaidSettingsConfigurable"
    id="com.nereid.settings"
    displayName="Mermaid"/>
```

**Step 3: Commit**

```bash
git add src/main/kotlin/com/nereid/settings/MermaidSettingsConfigurable.kt
git add src/main/resources/META-INF/plugin.xml
git commit -m "feat: add settings UI configurable"
```

---

## Phase 13: Structure View

### Task 13.1: Create Structure View Factory

**Files:**
- Create: `src/main/kotlin/com/nereid/editor/MermaidStructureViewFactory.kt`
- Modify: `src/main/resources/META-INF/plugin.xml`

**Step 1: Write implementation**

```kotlin
package com.nereid.editor

import com.intellij.ide.structureView.*
import com.intellij.ide.util.treeView.smartTree.TreeElement
import com.intellij.lang.PsiStructureViewFactory
import com.intellij.navigation.ItemPresentation
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiFile
import com.nereid.language.psi.MermaidFile
import javax.swing.Icon

class MermaidStructureViewFactory : PsiStructureViewFactory {

    override fun getStructureViewBuilder(psiFile: PsiFile): StructureViewBuilder? {
        if (psiFile !is MermaidFile) return null

        return object : TreeBasedStructureViewBuilder() {
            override fun createStructureViewModel(editor: Editor?): StructureViewModel {
                return MermaidStructureViewModel(psiFile)
            }
        }
    }
}

class MermaidStructureViewModel(file: MermaidFile) :
    StructureViewModelBase(file, MermaidStructureViewElement(file)),
    StructureViewModel.ElementInfoProvider {

    override fun isAlwaysShowsPlus(element: StructureViewTreeElement): Boolean = false
    override fun isAlwaysLeaf(element: StructureViewTreeElement): Boolean = false
}

class MermaidStructureViewElement(private val element: PsiFile) : StructureViewTreeElement {

    override fun getValue(): Any = element

    override fun getPresentation(): ItemPresentation = object : ItemPresentation {
        override fun getPresentableText(): String = element.name
        override fun getLocationString(): String? = null
        override fun getIcon(unused: Boolean): Icon? = null
    }

    override fun getChildren(): Array<TreeElement> {
        val children = mutableListOf<TreeElement>()
        val text = element.text

        // Parse nodes and subgraphs from text
        val nodeRegex = Regex("""(\w+)\s*[\[\(\{]""")
        val subgraphRegex = Regex("""subgraph\s+(\w+)""")

        subgraphRegex.findAll(text).forEach { match ->
            children.add(SimpleTreeElement(match.groupValues[1], "subgraph"))
        }

        nodeRegex.findAll(text).take(50).forEach { match ->
            val name = match.groupValues[1]
            if (name !in setOf("subgraph", "end", "graph", "flowchart")) {
                children.add(SimpleTreeElement(name, "node"))
            }
        }

        return children.toTypedArray()
    }

    override fun navigate(requestFocus: Boolean) {}
    override fun canNavigate(): Boolean = false
    override fun canNavigateToSource(): Boolean = false
}

class SimpleTreeElement(private val name: String, private val type: String) : TreeElement {
    override fun getPresentation(): ItemPresentation = object : ItemPresentation {
        override fun getPresentableText(): String = name
        override fun getLocationString(): String = type
        override fun getIcon(unused: Boolean): Icon? = null
    }

    override fun getChildren(): Array<TreeElement> = emptyArray()
}
```

**Step 2: Register in plugin.xml**

Add inside `<extensions defaultExtensionNs="com.intellij">`:

```xml
<lang.psiStructureViewFactory language="Mermaid" implementationClass="com.nereid.editor.MermaidStructureViewFactory"/>
```

**Step 3: Commit**

```bash
git add src/main/kotlin/com/nereid/editor/MermaidStructureViewFactory.kt
git add src/main/resources/META-INF/plugin.xml
git commit -m "feat: add structure view for nodes and subgraphs"
```

---

## Phase 14: Polish & Distribution

### Task 14.1: Update Plugin Description

**Files:**
- Modify: `src/main/resources/META-INF/plugin.xml`

**Step 1: Update description**

Replace the description section:

```xml
<description><![CDATA[
<h2>Nereid - Full-Featured Mermaid Diagram Support</h2>

<p>A powerful, free, and open-source plugin for creating and editing Mermaid diagrams in JetBrains IDEs.</p>

<h3>Features</h3>
<ul>
    <li><b>Live Preview</b> - See your diagrams render in real-time as you type</li>
    <li><b>Syntax Highlighting</b> - Full syntax highlighting for all Mermaid diagram types</li>
    <li><b>Autocomplete</b> - Smart completion for diagram types, keywords, arrows, and shapes</li>
    <li><b>Error Detection</b> - Immediate feedback on syntax errors with helpful messages</li>
    <li><b>Zoom & Pan</b> - Navigate large diagrams with mouse wheel zoom and drag to pan</li>
    <li><b>Export</b> - Export to PNG, SVG, or copy directly to clipboard</li>
    <li><b>Theme Support</b> - Automatically matches your IDE theme or choose from Mermaid themes</li>
    <li><b>Markdown Support</b> - Full support for Mermaid blocks in Markdown files</li>
</ul>

<h3>Supported Diagram Types</h3>
<ul>
    <li>Flowcharts & Graphs</li>
    <li>Sequence Diagrams</li>
    <li>Class Diagrams</li>
    <li>State Diagrams</li>
    <li>Entity-Relationship Diagrams</li>
    <li>Gantt Charts</li>
    <li>Pie Charts</li>
    <li>Git Graphs</li>
    <li>Mind Maps</li>
    <li>And more...</li>
</ul>

<p><a href="https://github.com/nereid/nereid">GitHub</a> | <a href="https://github.com/nereid/nereid/issues">Report Issues</a></p>
]]></description>
```

**Step 2: Commit**

```bash
git add src/main/resources/META-INF/plugin.xml
git commit -m "docs: update plugin description with feature list"
```

---

### Task 14.2: Create README

**Files:**
- Create: `README.md`

**Step 1: Write README**

```markdown
# Nereid - Mermaid Diagrams for JetBrains IDEs

A full-featured, free, and open-source Mermaid diagramming plugin for all JetBrains IDEs.

## Features

- **Live Preview** - Real-time rendering with configurable debounce
- **Syntax Highlighting** - Full highlighting for all Mermaid syntax
- **Autocomplete** - Smart completion for diagram types, keywords, arrows, shapes
- **Error Detection** - Instant feedback on syntax errors
- **Zoom & Pan** - Mouse wheel zoom and drag to pan
- **Export** - PNG, SVG, and clipboard support
- **Theme Integration** - Follows IDE theme or use Mermaid themes
- **Markdown Support** - Works in fenced code blocks

## Installation

### From JetBrains Marketplace

1. Open Settings → Plugins → Marketplace
2. Search for "Nereid"
3. Click Install

### Manual Installation

1. Download the latest release from [GitHub Releases](https://github.com/nereid/nereid/releases)
2. Open Settings → Plugins → ⚙️ → Install Plugin from Disk
3. Select the downloaded ZIP file

## Usage

1. Create a new file with `.mmd` extension
2. Start typing your Mermaid diagram
3. Use the toolbar to switch between code, split, and preview modes
4. Export via Tools → Mermaid or keyboard shortcuts

## Keyboard Shortcuts

- `Ctrl+Shift+E` - Export dialog
- `Ctrl+Shift+C` - Copy as PNG
- `Ctrl+/` - Toggle comment

## Building from Source

```bash
./gradlew buildPlugin
```

The plugin ZIP will be in `build/distributions/`.

## License

MIT License - see [LICENSE](LICENSE) for details.
```

**Step 2: Commit**

```bash
git add README.md
git commit -m "docs: add README with installation and usage instructions"
```

---

### Task 14.3: Create LICENSE File

**Files:**
- Create: `LICENSE`

**Step 1: Write LICENSE**

```
MIT License

Copyright (c) 2026 Nereid Contributors

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```

**Step 2: Commit**

```bash
git add LICENSE
git commit -m "chore: add MIT license"
```

---

### Task 14.4: Final Build and Test

**Step 1: Run full build**

```bash
./gradlew clean build
```

Expected: BUILD SUCCESSFUL

**Step 2: Run all tests**

```bash
./gradlew test
```

Expected: All tests pass

**Step 3: Build plugin distribution**

```bash
./gradlew buildPlugin
```

Expected: Plugin ZIP created in `build/distributions/`

**Step 4: Verify plugin**

```bash
./gradlew verifyPlugin
```

Expected: No compatibility issues

**Step 5: Final commit**

```bash
git add .
git commit -m "chore: complete initial implementation"
git tag v0.1.0
```

---

## Summary

This plan covers 14 phases with 40+ tasks to build a complete Mermaid diagramming plugin:

1. **Project Setup** - Gradle, plugin descriptor, icons
2. **Language Foundation** - Language, FileType, tokens, lexer
3. **Syntax Highlighting** - Highlighter, factory, color settings
4. **Basic Preview** - Mermaid.js bundle, JCEF panel
5. **Split Editor** - Provider, editor, toolbar
6. **Live Preview** - Debounced updates
7. **Editor Features** - Brace matching, commenter, folding, completion
8. **Error Detection** - Parser definition, annotator
9. **Theme Integration** - Theme manager, IDE theme detection
10. **Export** - PNG, SVG, clipboard exporters
11. **Markdown Injection** - Language injection for fenced blocks
12. **Settings** - Persistent state, configurable UI
13. **Structure View** - Nodes and subgraphs tree
14. **Polish & Distribution** - README, LICENSE, final build
