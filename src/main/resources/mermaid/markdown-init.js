// Mermaid diagram renderer for markdown preview
(function () {
  "use strict";

  let isRendering = false;
  let observer = null;

  function initMermaid() {
    if (typeof mermaid === "undefined") {
      return false;
    }
    try {
      mermaid.initialize({
        startOnLoad: false,
        theme: "default",
        securityLevel: "loose",
      });
      return true;
    } catch (e) {
      return false;
    }
  }

  function findMermaidBlocks() {
    const blocks = [];

    // Find standard markdown code fences: <pre><code class="language-mermaid">
    document.querySelectorAll('pre > code.language-mermaid').forEach(function(codeEl) {
      const pre = codeEl.parentElement;
      if (pre && !pre.hasAttribute('data-processed')) {
        blocks.push({ element: pre, code: codeEl.textContent || "" });
      }
    });

    // Also support legacy format: <pre class="mermaid">
    document.querySelectorAll('pre.mermaid:not([data-processed])').forEach(function(pre) {
      blocks.push({ element: pre, code: pre.textContent || "" });
    });

    return blocks;
  }

  async function renderDiagrams() {
    if (isRendering) {
      return;
    }

    const blocks = findMermaidBlocks();
    if (blocks.length === 0) {
      return;
    }

    isRendering = true;

    if (observer) {
      observer.disconnect();
    }

    for (const block of blocks) {
      block.element.setAttribute("data-processed", "true");

      const code = block.code;
      if (!code.trim()) {
        continue;
      }

      const id =
        "mermaid-" + Date.now() + "-" + Math.random().toString(36).slice(2, 6);

      try {
        const { svg } = await mermaid.render(id, code);

        const container = document.createElement("div");
        container.className = "mermaid-rendered";
        container.innerHTML = svg;

        block.element.style.display = "none";
        block.element.parentNode.insertBefore(container, block.element.nextSibling);
      } catch (e) {
        const errorDiv = document.createElement("div");
        errorDiv.className = "mermaid-error";
        errorDiv.textContent = "Mermaid error: " + (e.message || "Unknown error");
        block.element.parentNode.insertBefore(errorDiv, block.element.nextSibling);
      }
    }

    isRendering = false;

    if (observer) {
      observer.observe(document.body, { childList: true, subtree: true });
    }
  }

  function hasMermaidContent(node) {
    if (!node.matches && !node.querySelector) {
      return false;
    }
    // Check for standard markdown format
    if (node.matches && node.matches('pre:not([data-processed])') && node.querySelector('code.language-mermaid')) {
      return true;
    }
    if (node.querySelector && node.querySelector('pre:not([data-processed]) > code.language-mermaid')) {
      return true;
    }
    // Check for legacy format
    if (node.matches && node.matches('pre.mermaid:not([data-processed])')) {
      return true;
    }
    if (node.querySelector && node.querySelector('pre.mermaid:not([data-processed])')) {
      return true;
    }
    return false;
  }

  function setupObserver() {
    observer = new MutationObserver(function (mutations) {
      if (isRendering) {
        return;
      }

      let hasNew = false;
      for (const mutation of mutations) {
        if (mutation.type === "childList") {
          for (const node of mutation.addedNodes) {
            if (node.nodeType === Node.ELEMENT_NODE) {
              if (node.classList && node.classList.contains("mermaid-rendered")) {
                continue;
              }
              if (hasMermaidContent(node)) {
                hasNew = true;
                break;
              }
            }
          }
        }
        if (hasNew) break;
      }

      if (hasNew) {
        setTimeout(renderDiagrams, 50);
      }
    });

    observer.observe(document.body, { childList: true, subtree: true });
  }

  function start() {
    if (!initMermaid()) {
      setTimeout(start, 100);
      return;
    }

    renderDiagrams();
    setupObserver();
  }

  if (document.readyState === "loading") {
    document.addEventListener("DOMContentLoaded", start);
  } else {
    start();
  }
})();
