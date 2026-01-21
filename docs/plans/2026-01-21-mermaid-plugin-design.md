# Nereid: Mermaid Diagramming Plugin for JetBrains IDEs

## Overview

Nereid is a full-featured, open-source Mermaid diagramming plugin for JetBrains IDEs (IntelliJ, WebStorm, PyCharm, RustRover, etc.). It provides reliable rendering of the latest Mermaid spec, comprehensive editor support, and versatile export options—all without requiring a paid subscription.

### Motivation

Existing Mermaid plugins for JetBrains IDEs are either:
- Limited in features (outdated spec support, no zoom, poor syntax highlighting, no export)
- Locked behind paid subscriptions

Nereid aims to fill this gap with a free, MIT-licensed alternative that provides a first-class experience.

### Target Use Cases

- Technical documentation (READMEs, wikis, markdown docs)
- Software architecture (system diagrams, dependencies, class diagrams)
- Process/workflow modeling (flowcharts, sequence diagrams, state machines)

---

## Architecture

### High-Level Structure

```
┌─────────────────────────────────────────────────────────┐
│                     Nereid Plugin                        │
├─────────────────┬─────────────────┬─────────────────────┤
│   Editor Layer  │  Preview Layer  │   Export Layer      │
│                 │                 │                     │
│ • Language PSI  │ • JCEF Browser  │ • PNG Renderer      │
│ • Lexer/Parser  │ • Mermaid.js    │ • SVG Extractor     │
│ • Annotators    │ • Theme Bridge  │ • Clipboard Handler │
│ • Completions   │ • Zoom/Pan      │                     │
└─────────────────┴─────────────────┴─────────────────────┘
```

### Core Components

1. **Mermaid Language Support** - Custom `Language` and `FileType` for `.mmd` files with full PSI (Program Structure Interface) tree for IDE features.

2. **Markdown Injection** - Language injection into fenced code blocks (` ```mermaid `) within Markdown files, reusing the same PSI infrastructure.

3. **Preview Panel** - JCEF-based browser component running Mermaid.js, communicating with the editor via a JavaScript bridge for live updates.

4. **Split Editor** - Custom `FileEditor` implementation with three-mode toggle (code/split/preview), following JetBrains UI conventions.

### Platform Compatibility

Target IntelliJ Platform 2023.3+ to ensure JCEF stability and access to modern APIs. This covers all current JetBrains IDEs.

### Architectural Approach

Standalone monolithic plugin that handles everything: `.mmd` files, Markdown integration, editor features, and preview rendering. This approach provides:
- Simpler development, testing, and maintenance
- Single install for users with no dependency juggling
- Full control over the experience
- Future extensibility to other file types (AsciiDoc, reStructuredText, etc.)

---

## Editor Features

### Syntax Highlighting

Custom lexer tokenizing Mermaid syntax into distinct token types:

- **Diagram keywords** - `graph`, `sequenceDiagram`, `classDiagram`, `flowchart`, etc.
- **Directions** - `TB`, `LR`, `RL`, `BT`
- **Node identifiers** - Variable names, IDs
- **Connectors** - `-->`, `-.->`, `==>`, `--o`, `--x`, etc.
- **Labels/strings** - Text in brackets, quotes
- **Directives** - `%%{init: ...}%%` configuration blocks
- **Comments** - `%%` line comments

Colors inherit from the user's IDE color scheme with sensible semantic mappings. A dedicated "Mermaid" section in Settings → Editor → Color Scheme allows customization.

### Error Detection

A custom `Annotator` performs two levels of validation:

1. **Structural errors** - Invalid diagram type, malformed arrows, unclosed brackets
2. **Semantic warnings** - Undefined node references, duplicate IDs, deprecated syntax

Errors display as red squiggles with hover tooltips explaining the issue and suggesting fixes via quick-fix intentions where possible.

### Autocomplete

Context-aware completion provider offering:

- Diagram type keywords when starting a file
- Direction options after `graph`/`flowchart`
- Arrow types mid-connection
- Existing node IDs for references
- Directive options within `%%{init:}%%` blocks
- Shape syntax (`[]`, `()`, `{}`, `[[]]`, etc.)

### Additional IDE Features

- **Code folding** - Collapse subgraphs, large node definitions
- **Structure view** - Tree of diagram elements (nodes, subgraphs)
- **Brace matching** - Brackets, parentheses, subgraph blocks
- **Comment toggling** - Cmd/Ctrl+/ adds `%%` prefix

---

## Preview & Rendering

### JCEF Browser Component

The preview uses JetBrains' embedded Chromium (JCEF) running a minimal HTML page that loads Mermaid.js. This guarantees full spec compatibility since we're using the official library.

**JavaScript Bridge** - A bidirectional communication channel between Kotlin and the browser:
- **Kotlin → JS**: Push diagram source, theme settings, zoom commands
- **JS → Kotlin**: Report render errors, diagram dimensions, click events

**Mermaid.js Bundling** - The plugin bundles a specific Mermaid.js version (with a settings option to use a custom version for bleeding-edge users). Updates ship with plugin releases.

### Live Preview

Default behavior: re-render on every keystroke with a 300ms debounce to avoid thrashing during rapid typing. Configurable options:
- Debounce delay (0ms–2000ms)
- Update on save only
- Manual refresh only

When rendering fails, the preview shows the last successful render with an unobtrusive error indicator, rather than flashing error states during incomplete edits.

### Theme Integration

On load and IDE theme change, the plugin:
1. Detects current IDE theme (light/dark/custom)
2. Maps to appropriate Mermaid theme or generates CSS variables
3. Injects theme configuration into Mermaid's `initialize()` call

Users can override via Settings: "Follow IDE theme", select a Mermaid theme (default, dark, forest, neutral), or provide custom theme variables.

### Zoom & Navigation

- **Zoom controls** - Toolbar buttons and shortcuts (Cmd/Ctrl +/-)
- **Mouse wheel** - Ctrl+scroll zooms, configurable sensitivity
- **Pan/drag** - Click-drag to move viewport when zoomed in
- **Fit options** - "Fit to width", "Fit to height", "Actual size", "Fit all" buttons
- **Zoom level indicator** - Shows current percentage, click to reset

### Split Editor Modes

Three-mode toggle matching JetBrains conventions:
1. **Code only** - Full editor, no preview
2. **Split** - Side-by-side editor and preview
3. **Preview only** - Full preview, no editor

---

## Export Functionality

### Export Targets

**PNG Export**
- Renders the diagram at configurable resolution (1x, 2x, 3x for retina)
- Transparent or solid background (user choice)
- Default filename: `{diagram-name}-{timestamp}.png`
- "Save As" dialog with last-used directory remembered

**SVG Export**
- Extracts the raw SVG from Mermaid's render output
- Optionally embeds fonts or links to web fonts
- Preserves styling for use in design tools (Figma, Illustrator)
- Clean output - strips unnecessary JCEF/wrapper markup

**Clipboard**
- **Copy as PNG** - Bitmap to clipboard, paste directly into Slack, docs, etc.
- **Copy as SVG** - Vector markup for design tools
- **Copy source** - The Mermaid code itself (convenience shortcut)

### Export Access Points

Multiple ways to trigger export:

1. **Preview toolbar** - Buttons for each export type
2. **Editor context menu** - Right-click → Export Mermaid Diagram
3. **Keyboard shortcuts** - Configurable, suggested defaults:
   - `Cmd/Ctrl+Shift+E` → Export dialog
   - `Cmd/Ctrl+Shift+C` → Quick copy as PNG
4. **Main menu** - Tools → Mermaid → Export...

---

## Settings & Configuration

Settings location: Settings → Tools → Mermaid

### Editor Tab

| Setting | Options | Default |
|---------|---------|---------|
| Preview update mode | Live / On save / Manual | Live |
| Live preview debounce | 0–2000ms slider | 300ms |
| Default view mode | Code / Split / Preview | Split |
| Show line numbers in preview errors | On/Off | On |

### Appearance Tab

| Setting | Options | Default |
|---------|---------|---------|
| Theme mode | Follow IDE / Mermaid theme / Custom | Follow IDE |
| Mermaid theme | Default / Dark / Forest / Neutral | (follows mode) |
| Custom theme variables | JSON editor | — |
| Preview background | Transparent / Match IDE / White / Dark | Match IDE |

### Export Tab

| Setting | Options | Default |
|---------|---------|---------|
| Default export format | PNG / SVG | PNG |
| PNG scale factor | 1x / 2x / 3x | 2x |
| PNG background | Transparent / Solid | Transparent |
| SVG font handling | Embed / Link / None | Embed |
| Default export directory | Path picker | Project root |

### Zoom & Navigation Tab

| Setting | Options | Default |
|---------|---------|---------|
| Mouse wheel zoom | Enabled / Disabled | Enabled |
| Zoom modifier key | Ctrl / Cmd / None | Ctrl |
| Zoom sensitivity | 1–10 slider | 5 |
| Default zoom level | Fit all / 100% / Last used | Fit all |

### Advanced Tab

| Setting | Options | Default |
|---------|---------|---------|
| Mermaid.js version | Bundled / Custom URL | Bundled |
| Custom Mermaid.js URL | Text field | — |
| Security mode | Strict / Loose | Strict |
| Enable experimental features | On/Off | Off |

---

## Project Structure

### Technology Stack

- **Language**: Kotlin
- **Build**: Gradle with `intellij-platform-gradle-plugin`
- **Min Platform**: IntelliJ Platform 2023.3+
- **Testing**: JUnit 5 + intellij-test-framework
- **License**: MIT

### Directory Layout

```
nereid/
├── src/main/kotlin/com/nereid/
│   ├── language/           # PSI, lexer, parser
│   │   ├── MermaidLanguage.kt
│   │   ├── MermaidFileType.kt
│   │   ├── MermaidLexer.kt
│   │   ├── MermaidParser.kt
│   │   └── psi/            # PSI element types
│   ├── editor/             # Editor features
│   │   ├── MermaidSyntaxHighlighter.kt
│   │   ├── MermaidAnnotator.kt
│   │   ├── MermaidCompletionContributor.kt
│   │   ├── MermaidFoldingBuilder.kt
│   │   └── MermaidStructureViewFactory.kt
│   ├── preview/            # JCEF preview
│   │   ├── MermaidPreviewPanel.kt
│   │   ├── MermaidJsBridge.kt
│   │   └── ThemeManager.kt
│   ├── spliteditor/        # Split editor implementation
│   │   ├── MermaidSplitEditor.kt
│   │   └── MermaidEditorProvider.kt
│   ├── export/             # Export functionality
│   │   ├── PngExporter.kt
│   │   ├── SvgExporter.kt
│   │   └── ClipboardExporter.kt
│   ├── markdown/           # Markdown integration
│   │   └── MermaidLanguageInjector.kt
│   ├── settings/           # Plugin settings
│   │   ├── MermaidSettings.kt
│   │   └── MermaidSettingsConfigurable.kt
│   └── actions/            # Menu/toolbar actions
├── src/main/resources/
│   ├── META-INF/plugin.xml # Plugin descriptor
│   ├── mermaid/            # Bundled Mermaid.js + preview HTML
│   └── icons/              # Plugin icons
├── src/test/kotlin/        # Tests
├── build.gradle.kts
└── gradle.properties
```

### Key Implementation Notes

**Lexer Generation**: Use GrammarKit (JFlex + BNF) to generate lexer/parser from a grammar file, avoiding hand-written parsing complexity.

**JCEF Lifecycle**: Careful management of browser instance - create lazily, dispose on editor close, handle IDE restart gracefully.

**Markdown Injection**: Register `LanguageInjector` that recognizes ` ```mermaid ` fences and injects `MermaidLanguage`, giving full editor support within Markdown.

**Settings Persistence**: Use `PersistentStateComponent` with XML serialization for settings that survive IDE restarts.

---

## Distribution

- **JetBrains Marketplace** - Official distribution for discoverability and auto-updates
- **GitHub Releases** - Development builds and full source transparency
- **License**: MIT (fully open source)
