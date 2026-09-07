# Changelog

All notable changes to Nereid are documented here.

The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.3.0]

The settings screen finally does what it says, and neither preview will let a diagram file run script any more. Almost everything here is behaviour that was advertised and quietly absent.

### Changed

- **Markdown previews now respect the Security mode setting.** They previously ignored it and always rendered permissively. They now default to Strict, matching the dedicated preview. If you rely on HTML labels or clickable nodes in Markdown previews, set Settings, Nereid, Security mode to Loose.
- **Seven settings that were saved but never read now take effect**: Security mode, Mouse wheel zoom, Debounce delay, Default zoom level, Preview update mode, Default view mode and Default export format. Each was shown on the settings screen and written to your settings file while changing nothing.
- **The Security mode dropdown now says what Loose permits**, so choosing it is a deliberate decision rather than a guess.

### Fixed

- **The preview could settle on an older version of your diagram.** Renders were dispatched without waiting for the previous one, so a slow render of an earlier edit could land on top of a newer one and stay there until the next keystroke. Typing quickly on a large diagram could also leave the preview blank while reporting success. Superseded renders are now discarded, which also makes the preview settle faster during rapid edits.
- **Valid diagrams were reported as errors.** The bracket check scanned the text character by character with no notion of quoting or comments, so a label like `A["array[0"]` or a `%%` comment containing `[` produced a spurious "unclosed bracket". Both checks now work from the parser's own tokens.
- **Unknown diagram types were judged against a stale list.** The editor kept its own hardcoded set of diagram types, which had drifted from the one the parser recognises, so valid diagrams could be marked unknown.
- **Two Kotlin standard libraries could end up on the plugin's classpath**, which surfaces in your IDE as a ClassNotFoundException or a version mismatch rather than at build time. It shipped that way in 1.1.0 and 1.2.0.

### Security

- **Diagram files can no longer run script in the preview.** With Security mode set to Loose, a diagram using Mermaid's `click` directive with a `javascript:` URL executed that script when the node was clicked, reaching plugin code inside the IDE. Both previews now strip script URLs out of the rendered diagram at every security level. Ordinary `click` links to `https:` and `mailto:` addresses are unaffected.
- **PNG exports are checked before they are written.** The image data crossing from the preview is now validated as a PNG, so content a diagram supplied itself cannot be written to the file you picked in the save dialog.

## [1.2.0]

Custom CSS arrives, panning is fixed, and the settings screen finally does what it says. Several of the bugs here failed silently, which is why they lasted so long.

### Added

- **Custom CSS.** Inject your own CSS into the diagram preview from Settings, Nereid, Advanced. Useful for overriding stroke widths, label fonts and colours that the built-in Mermaid themes do not expose. Clearing the field removes the rules again.

### Fixed

- **"Report this issue" did nothing.** The link in the render error overlay had never worked since it was added: the dialog was created on the browser's own thread, where the IDE refuses to show it, and the resulting error was swallowed.
- **Diagram text blurred after panning.** The preview animated every pan, so the diagram continuously lagged the cursor and its text was resampled part-way through the animation. Panning is now immediate; the zoom buttons still animate.
- **Dragging to pan selected the diagram's text.** A single drag highlighted every label. Error messages stay selectable, so you can still copy them.
- **Settings had no effect until the file was reopened.** Nothing told an open preview that a setting had changed, so changes made on the settings screen appeared to do nothing. Changing the theme from the toolbar also left any other open diagram on the old theme.
- **The preview needed an internet connection.** Mermaid was fetched from a CDN at an unpinned version, so the preview failed offline and could change under you with no plugin update. It is now bundled with the plugin.
- **Diagnostic reports named the wrong Mermaid version.** The version was hardcoded and had drifted from the library actually shipped.

### Changed

- **One settings screen instead of two.** The toolbar's gear button now opens Settings, Nereid, rather than a separate dialog that bound the same settings a different way. Both had independently developed the same save bug. The page is regrouped as Preview, Editor, Export and Advanced.
- **PNG export scale can be set up to 8x.** The dropdown stopped at 3, although exports already supported 8.
- **Mermaid updated to a stock 11.16.1 build**, replacing a patched bundle, so diagram rendering matches upstream.

### Removed

- **Nine settings that never did anything.** They were saved to disk and read by nothing. Three had visible controls: "Use custom Mermaid.js", "Custom URL" and "Modifier key". Existing settings files load fine; the values are simply ignored.

## [1.1.0]

Five user-facing bugs are fixed in this release, several of which failed silently.

### Fixed

- PNG export produced no file and reported no error. The rendered diagram was passed to the image loader as a blob URL, which cannot load from the preview document's origin, so the export aborted before it began.
- PNG export failures are now reported. Three separate code paths previously discarded errors with no message and no log entry, which is why the bug above went unnoticed. Export, clipboard copy and SVG export all surface failures now.
- The PNG scale and transparent-background settings had no effect. Both were saved and shown in the settings UI but never read when exporting; the scale was fixed at 2x and a background was always drawn.
- No setting on the settings screen was ever saved. This affected all fifteen settings, not only the export ones, in both the IDE settings page and the toolbar's settings dialog. Apply also stayed permanently enabled, and Cancel did not undo changes.
- Export actions crashed and disabled the Tools menu. Opening Tools then Nereid logged an error and greyed out every entry in the group.

### Changed

- **PNG exports are now transparent by default.** The transparent-background setting has always defaulted to on, but was ignored until now, so exports always had a solid background. This applies to Copy as PNG too. Turn it off in Settings, Nereid, Export if you prefer a solid background.
- Dark theme detection is more reliable. It previously matched on theme names containing "dark" or "darcula", so any dark theme named otherwise (Nord, Gradianto, Monokai Pro and most third-party themes) was treated as light, giving a light diagram on a dark background.
- The settings screen and dialog are now both titled "Nereid" rather than "Mermaid".

### Internal

- Build upgraded to Gradle 9.5.1, Kotlin 2.4.10 and IntelliJ Platform Gradle Plugin 2.18.1.
- Plugin verification extended from 2024.3 to 2026.2, now covering eleven IDE builds across IntelliJ IDEA, WebStorm and PyCharm. The verified range is resolved dynamically so it tracks new IDE releases instead of going stale.
- Replaced platform APIs scheduled for removal (`AlarmFactory`, `LafManager.getCurrentLookAndFeel()`) and the internal plugin-descriptor lookup that became unavailable in 2026.2.

## Earlier releases

Releases before 1.1.0 predate this changelog. See the
[release history](https://github.com/mirkozagami/nereid/releases) for details.
