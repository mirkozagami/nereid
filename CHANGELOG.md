# Changelog

All notable changes to Nereid are documented here.

The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

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
