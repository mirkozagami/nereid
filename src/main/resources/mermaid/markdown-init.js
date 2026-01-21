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

  async function renderDiagrams() {
    if (isRendering) {
      return;
    }

    const blocks = document.querySelectorAll(
      "pre.mermaid:not([data-processed])"
    );
    if (blocks.length === 0) {
      return;
    }

    isRendering = true;

    if (observer) {
      observer.disconnect();
    }

    for (const block of blocks) {
      block.setAttribute("data-processed", "true");

      const code = block.textContent || "";
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

        block.style.display = "none";
        block.parentNode.insertBefore(container, block.nextSibling);
      } catch (e) {
        const errorDiv = document.createElement("div");
        errorDiv.className = "mermaid-error";
        errorDiv.textContent = "Mermaid error: " + (e.message || "Unknown error");
        block.parentNode.insertBefore(errorDiv, block.nextSibling);
      }
    }

    isRendering = false;

    if (observer) {
      observer.observe(document.body, { childList: true, subtree: true });
    }
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
              if (node.matches && node.matches("pre.mermaid:not([data-processed])")) {
                hasNew = true;
                break;
              }
              if (node.querySelector && node.querySelector("pre.mermaid:not([data-processed])")) {
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
