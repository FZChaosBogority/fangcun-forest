(() => {
  "use strict";
  const KEY = "forest-page-entry";
  const root = document.documentElement;
  const motion = matchMedia("(prefers-reduced-motion: reduce)");
  const script = document.currentScript;
  const storeDocument = script?.hasAttribute("data-store-transitions") === true;
  const nativeOptIn = storeDocument || script?.hasAttribute("data-native-transitions") === true;
  const base = script ? new URL(script.src).pathname.replace(/\/js\/page-transition\.js$/, "") : "";
  const nativeSupported = nativeOptIn && "CSSViewTransitionRule" in window && "onpageswap" in window && "onpagereveal" in window;
  if (nativeOptIn && !nativeSupported) {
    const fallback = document.createElement("style");
    fallback.textContent = "@view-transition { navigation: none; }";
    document.head.append(fallback);
  }
  let leaving = false;
  let nativeTransition = null;
  let namedAuthExit = false;
  let authExitArrival = false;

  function isStore(url) {
    if (url.origin !== location.origin || !url.pathname.startsWith(base + "/")) return false;
    return /^\/(main|category|product|cart|checkout|account|orders(?:\/[1-9][0-9]*(?:\/pay)?)?)$/.test(url.pathname.slice(base.length));
  }
  function isAuth(url) { return url.origin === location.origin && ["/", "/index", "/login", "/register"].includes(url.pathname.slice(base.length)); }
  function markEntry(authExit = false) {
    try { sessionStorage.setItem(KEY, JSON.stringify({ at: Date.now(), authExit })); } catch {}
  }
  function clearAuthExit() {
    if (!namedAuthExit) return;
    document.querySelector(".forest-story")?.style.removeProperty("view-transition-name");
    document.querySelector(".auth-panel")?.style.removeProperty("view-transition-name");
    namedAuthExit = false;
  }
  function enter() {
    if (motion.matches) return;
    root.classList.remove("page-entering");
    void root.offsetWidth;
    root.classList.add("page-entering");
  }
  function revealFirstScreen() {
    if (root.dataset.pageArrival !== "true") return;
    document.querySelectorAll("[data-store-reveal]").forEach(item => {
      const rect = item.getBoundingClientRect();
      if (rect.top < innerHeight && rect.bottom > 0) {
        // The page fade reveals the first screen; keep below-fold scroll reveals intact.
        item.style.transition = "none";
        item.classList.add("is-store-visible");
      }
    });
  }
  try {
    const entry = JSON.parse(sessionStorage.getItem(KEY) || "null");
    sessionStorage.removeItem(KEY);
    const historyReturn = performance.getEntriesByType("navigation")[0]?.type === "back_forward";
    if (entry?.at > Date.now() - 15000 || historyReturn) {
      root.dataset.pageArrival = "true";
      authExitArrival = entry?.authExit === true && entry.at > Date.now() - 15000;
      if (!motion.matches) root.classList.add("page-entering");
    }
  } catch {}
  document.addEventListener("DOMContentLoaded", revealFirstScreen, { once: true });
  document.addEventListener("animationend", event => {
    if (event.animationName === "forest-page-enter") root.classList.remove("page-entering");
  });

  window.addEventListener("pageswap", event => {
    if (!nativeOptIn || !event.activation?.entry?.url) return;
    const next = new URL(event.activation.entry.url);
    if (isStore(next) || isAuth(next)) markEntry(namedAuthExit);
    if (namedAuthExit && event.viewTransition) {
      event.viewTransition.ready.catch(() => {});
      event.viewTransition.finished.then(clearAuthExit, clearAuthExit);
    }
  });
  window.addEventListener("pagereveal", event => {
    clearAuthExit();
    root.classList.remove("auth-exit-arrival");
    const splitAuth = authExitArrival;
    authExitArrival = false;
    if (!event.viewTransition) return;
    const current = event.viewTransition;
    if (!nativeSupported || motion.matches) {
      current.ready.catch(() => {});
      current.finished.catch(() => {});
      current.skipTransition();
      return;
    }
    root.classList.remove("page-entering");
    if (splitAuth) root.classList.add("auth-exit-arrival");
    root.dataset.pageArrival = "true";
    nativeTransition = current;
    revealFirstScreen();
    const fallback = () => {
      if (nativeTransition !== current) return;
      nativeTransition = null;
      root.classList.remove("auth-exit-arrival");
      enter();
    };
    current.ready.catch(fallback);
    current.finished.then(() => {
      if (nativeTransition === current) nativeTransition = null;
      root.classList.remove("auth-exit-arrival");
    }, fallback);
  });

  async function leave(value, options = {}) {
    const url = new URL(value, location.href);
    if (leaving || url.origin !== location.origin) return;
    leaving = true;
    if (nativeSupported && !motion.matches && options.splitAuth) {
      const scene = document.querySelector(".forest-story");
      const panel = document.querySelector(".auth-panel");
      if (scene && panel) {
        scene.style.viewTransitionName = "forest-scene";
        panel.style.viewTransitionName = "forest-panel";
        namedAuthExit = true;
      }
    }
    markEntry(namedAuthExit);
    // Keep the old document visible while the next response loads. The browser
    // starts both snapshots on one timeline, so network time cannot become a blank hold.
    // Browsers without snapshots keep the old page until hand-off, then fade the new page in.
    location.assign(url.href);
  }

  function guestDestination(url) {
    if (document.querySelector(".site-header")?.dataset.guest !== "true") return url;
    const path = url.pathname.slice(base.length);
    let target = "";
    if (path === "/cart" || path === "/checkout") target = "/cart";
    else if (/^\/orders(?:\/|$)/.test(path)) target = "/orders";
    else if (path === "/account") target = "/account";
    if (!target) return url;
    const login = new URL(base + "/login", location.origin);
    login.searchParams.set("returnTo", target);
    return login;
  }
  if (storeDocument) {
    document.addEventListener("click", event => {
      if (event.defaultPrevented || event.button !== 0 || event.ctrlKey || event.metaKey || event.shiftKey || event.altKey) return;
      const link = event.target.closest("a[href]");
      if (!link || link.hasAttribute("download") || (link.target && link.target !== "_self")) return;
      const url = new URL(link.href, location.href);
      if ((!isStore(url) && !isAuth(url)) || (url.pathname === location.pathname && url.search === location.search && url.href.includes("#"))) return;
      if (url.href === location.href) return;
      event.preventDefault();
      leave(guestDestination(url));
    });
    document.addEventListener("submit", event => {
      const form = event.target;
      if (event.defaultPrevented || form.matches("[data-login-required], [data-login-form], [data-register-form]")) return;
      const submitter = event.submitter;
      const method = submitter?.getAttribute("formmethod") || form.method;
      const target = submitter?.getAttribute("formtarget") || form.target;
      if (method.toLowerCase() !== "get" || (target && target !== "_self")) return;
      const url = new URL(submitter?.getAttribute("formaction") || form.action, location.href);
      if (!isStore(url)) return;
      const data = new FormData(form);
      if (submitter?.name) data.append(submitter.name, submitter.value);
      url.search = new URLSearchParams(data).toString();
      if (url.pathname === location.pathname && url.search === location.search && url.href.includes("#")) return;
      event.preventDefault(); leave(guestDestination(url));
    });
  }
  window.addEventListener("pageshow", event => {
    if (!event.persisted) return;
    clearAuthExit(); leaving = false;
    root.dataset.pageArrival = "true";
    root.classList.remove("page-entering");
    revealFirstScreen();
    if (!nativeTransition) enter();
  });
  motion.addEventListener("change", () => {
    if (!motion.matches) return;
    root.classList.remove("page-entering");
    nativeTransition?.skipTransition();
  });
  window.forestPageTransition = { leave, nativeSupported };
})();
