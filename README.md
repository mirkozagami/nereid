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

1. Open Settings -> Plugins -> Marketplace
2. Search for "Nereid"
3. Click Install

### Manual Installation

1. Download the latest release from [GitHub Releases](https://github.com/nereid/nereid/releases)
2. Open Settings -> Plugins -> (gear icon) -> Install Plugin from Disk
3. Select the downloaded ZIP file

## Usage

1. Create a new file with `.mmd` extension
2. Start typing your Mermaid diagram
3. Use the toolbar to switch between code, split, and preview modes
4. Export via Tools -> Mermaid or keyboard shortcuts

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
