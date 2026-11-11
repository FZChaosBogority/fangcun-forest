(() => {
  "use strict";
  const motion = matchMedia("(prefers-reduced-motion: reduce)");
  if (motion.matches || !("IntersectionObserver" in window)) return;
  const root = document.documentElement;
  let fromAuth = false;
  try {
    const entry = JSON.parse(sessionStorage.getItem("forest-store-entry") || "null");
    fromAuth = entry?.source === "auth" && Date.now() - entry.at < 15000;
    sessionStorage.removeItem("forest-store-entry");
  } catch {}
  root.classList.add("store-motion-ready");
  root.dataset.storeEntry = fromAuth ? "from-auth" : "direct";

  function initialize() {
    const items = Array.from(document.querySelectorAll("[data-store-reveal]"));
    const observer = new IntersectionObserver(entries => {
      let stagger = 0;
      for (const entry of entries) {
        if (!entry.isIntersecting) continue;
        entry.target.style.setProperty("--store-reveal-delay", `${Math.min(stagger, 3) * 65}ms`);
        entry.target.classList.add("is-store-visible");
        observer.unobserve(entry.target);
        stagger += 1;
      }
    }, { threshold: 0.06, rootMargin: "0px 0px -18px 0px" });
    items.forEach(item => observer.observe(item));

    // Never leave all content hidden if initialization or observation fails.
    const recover = setTimeout(() => {
      if (!items.some(item => item.classList.contains("is-store-visible"))) {
        observer.disconnect();
        root.classList.remove("store-motion-ready");
      }
    }, 1800);
    motion.addEventListener("change", () => {
      if (motion.matches) {
        clearTimeout(recover);
        observer.disconnect();
        root.classList.remove("store-motion-ready");
      }
    });
    window.addEventListener("pageshow", event => {
      if (event.persisted) items.filter(item => item.getBoundingClientRect().top < innerHeight)
        .forEach(item => item.classList.add("is-store-visible"));
    });
  }

  if (document.readyState === "loading") document.addEventListener("DOMContentLoaded", initialize, { once: true });
  else initialize();
})();
