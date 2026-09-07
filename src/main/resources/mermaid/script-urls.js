// Shared by both previews: substituted into preview.html by
// MermaidPreviewPanel.buildPreviewHtml, and into markdown-init.js by
// MermaidMarkdownResourceProvider as that script is served.
//
// It lives in one file rather than being copied into each page because #44 was exactly
// that -- two render paths carrying their own copy of a security decision, which drifted
// until the same diagram rendered with different security in each preview.

/*
  Diagram source must not be able to put a script URL into the preview (#52).

  `click A href "javascript:..."` is a Mermaid directive, so that URL comes
  straight out of the .mmd file. Under securityLevel 'loose' -- selectable from
  Settings since #44 -- Mermaid passes it through to an <a xlink:href> wrapping
  the node, and clicking that node runs the payload as script in this page.

  What the payload can reach is not limited to our own helpers. window.javaBridge
  is one target, but JBCefJSQuery.inject() also emits a plain
  `window.<slot>({request: ...})` global, so a payload can drive the Kotlin
  message router directly with a request string of its choosing. Holding
  javaBridge in a closure would not change that, which is why the URL is stopped
  here instead: nothing downstream has to be trusted if the script URL never
  enters the document at all.

  Applied at every security level rather than only under 'loose'. 'strict' drops
  these URLs inside Mermaid already, and that is the point -- a guard that reads
  the setting is missing exactly when the setting is wrong.

  Schemes only, not a URL allowlist. `click A href "https://..."` is a large part
  of why anyone turns 'loose' on, and it keeps working.
*/
const SCRIPT_URL = /^(?:javascript|vbscript):/i;
// `data:` is barred for anchors alone. A data: URL under the user's click
// navigates to attacker-authored content, whereas <image href="data:image/png...">
// is an ordinary way to put a picture in a diagram and has to keep working.
const ANCHOR_URL = /^(?:javascript|vbscript|data):/i;

/**
 * True when `value` carries a scheme that `pattern` rejects.
 *
 * Whitespace and control characters come out first, because browsers ignore them
 * when parsing a URL: `jav&#9;ascript:alert(1)` navigates exactly like
 * `javascript:alert(1)`, and innerHTML has already turned that entity into a real
 * tab by the time this sees the value. Matching the raw string would leave the
 * bypass wide open.
 */
function hasUnsafeScheme(value, pattern) {
    return pattern.test(value.replace(/[\u0000-\u0020]/g, ''));
}

/**
 * Removes href attributes carrying a script URL from an already-rendered diagram.
 *
 * Runs in the same task as the innerHTML that inserted the SVG, so the document
 * never yields to an event with such an attribute still on it.
 */
function stripScriptUrls(root) {
    // '[*|href]' is href in any namespace, which catches Mermaid's xlink:href as
    // well as a plain href attribute.
    root.querySelectorAll('[*|href]').forEach(function(el) {
        const pattern = el.localName === 'a' ? ANCHOR_URL : SCRIPT_URL;
        // Collected into an array first: attributes is a live map, and removing
        // from it while iterating it skips entries.
        Array.prototype.slice.call(el.attributes)
            .filter(function(attr) {
                return attr.localName === 'href' && hasUnsafeScheme(attr.value, pattern);
            })
            .forEach(function(attr) {
                el.removeAttributeNode(attr);
            });
    });
}
