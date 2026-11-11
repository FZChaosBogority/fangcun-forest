(() => {
  "use strict";

  const motion = matchMedia("(prefers-reduced-motion: reduce)");
  const scene = document.querySelector(".forest-story");
  const panel = document.querySelector("[data-auth-panel]");
  const runningAnimations = new Set();
  const heldAnimations = new Set();
  const ENTRY_KEY = "forest-store-entry";
  let panelBusy = false;
  let leaving = false;

  async function animate(element, frames, options, retain = false) {
    if (motion.matches || typeof element.animate !== "function") return;
    const animation = element.animate(frames, options);
    runningAnimations.add(animation);
    try { await animation.finished; } catch { /* Cancelled by reduced-motion or navigation. */ }
    finally {
      runningAnimations.delete(animation);
      if (retain) heldAnimations.add(animation);
      else animation.cancel();
    }
  }

  // Two stationary photo layers: only the incoming alpha mask travels right.
  if (scene) {
    const stories = [
      {
        title: "把一小片森林，\n安放在你的日常。",
        description: "从一瓶苔藓开始，让忙碌的生活，\n也有一处慢慢生长的角落。",
        image: scene.dataset.storyOriginal,
        alt: "露珠落在蕨叶与苔藓上，微光照亮一方小小森林"
      },
      {
        title: "一方小小微景，\n收藏四季的呼吸。",
        description: "水彩里的溪流与苔藓，\n把大自然的温柔，留在触手可及的地方。",
        image: scene.dataset.storyWatercolor,
        alt: "纸感水彩风格的苔藓、蕨叶与林间溪流"
      },
      {
        title: "让生活慢一点，\n让绿意多一点。",
        description: "把一座微缩的绿色世界，\n安放在桌边，慢慢养成自己的小森林。",
        image: scene.dataset.storySculpture,
        alt: "极简立体风格的玻璃生态景观，内有小树、苔藓与卵石小径"
      }
    ];
    const photos = Array.from(scene.querySelectorAll("[data-story-image]"));
    const stops = Array.from(scene.querySelectorAll("[data-story-select]"));
    const progress = scene.querySelector(".story-progress");
    const cache = new Map();
    let selected = 0;
    let desired = 0;
    let switching = false;
    scene.dataset.storyIndex = "0";

    function loadImage(url) {
      if (!cache.has(url)) {
        cache.set(url, new Promise((resolve, reject) => {
          const image = new Image();
          image.onload = async () => {
            try { if (image.decode) await image.decode(); resolve(image); }
            catch (error) { reject(error); }
          };
          image.onerror = () => { cache.delete(url); reject(new Error("Background unavailable")); };
          image.src = url;
        }));
      }
      return cache.get(url);
    }

    function showStoryText(index) {
      const story = stories[index];
      scene.querySelector("[data-story-title]").textContent = story.title;
      scene.querySelector("[data-story-description]").textContent = story.description;
      scene.querySelector("[data-story-current]").textContent = String(index + 1).padStart(2, "0");
      progress?.style.setProperty("--story-progress", (index / (stories.length - 1) * 100) + "%");
      stops.forEach(stop => stop.setAttribute("aria-pressed", String(Number(stop.dataset.storySelect) === index)));
      scene.dispatchEvent(new CustomEvent("forest:storychange"));
    }

    async function selectStory(index) {
      desired = (index + stories.length) % stories.length;
      if (switching) return;
      switching = true;
      scene.setAttribute("aria-busy", "true");
      try {
        while (selected !== desired) {
          const target = desired;
          const story = stories[target];
          await loadImage(story.image);
          if (desired !== target) continue; // A newer request superseded a slow image load.
          const previous = photos.find(photo => photo.classList.contains("is-active"));
          const incoming = photos.find(photo => photo !== previous);
          incoming.src = story.image;
          incoming.alt = story.alt;
          incoming.removeAttribute("aria-hidden");
          previous.setAttribute("aria-hidden", "true");
          if (incoming.decode) await incoming.decode();
          incoming.classList.add("is-incoming");
          showStoryText(target);
          await animate(incoming, [
            { maskPosition: "100% 0%", webkitMaskPosition: "100% 0%" },
            { maskPosition: "0% 0%", webkitMaskPosition: "0% 0%" }
          ], { duration: 1100, easing: "cubic-bezier(.22,.65,.3,1)", fill: "forwards" });
          incoming.classList.add("is-active");
          incoming.classList.remove("is-incoming");
          previous.classList.remove("is-active");
          previous.alt = "";
          selected = target;
          scene.dataset.storyIndex = String(selected);
        }
      } catch {
        desired = selected;
        // Preserve the last usable image if a local asset cannot be decoded.
        photos.forEach(photo => photo.classList.remove("is-incoming"));
        const active = photos.find(photo => photo.classList.contains("is-active"));
        active?.removeAttribute("aria-hidden");
      } finally {
        switching = false;
        scene.removeAttribute("aria-busy");
      }
    }

    scene.querySelector("[data-story-previous]")?.addEventListener("click", () => selectStory(desired - 1));
    scene.querySelector("[data-story-next]")?.addEventListener("click", () => selectStory(desired + 1));
    stops.forEach(stop => stop.addEventListener("click", () => selectStory(Number(stop.dataset.storySelect))));
    progress?.addEventListener("keydown", event => {
      if (event.key === "ArrowRight" || event.key === "ArrowLeft") {
        event.preventDefault();
        selectStory(desired + (event.key === "ArrowRight" ? 1 : -1));
      }
    });
    const preload = () => stories.slice(1).forEach(story => loadImage(story.image).catch(() => {}));
    if ("requestIdleCallback" in window) requestIdleCallback(preload, { timeout: 1200 });
    else setTimeout(preload, 200);
  }

  if (!panel) return;
  const welcomeUrl = new URL(panel.dataset.welcomeUrl, location.href);
  const loginUrl = new URL(panel.dataset.loginUrl, location.href);
  const registerUrl = new URL(loginUrl.pathname.replace(/\/login$/, "/register"), location.href);
  const authPaths = new Set([welcomeUrl.pathname, loginUrl.pathname, registerUrl.pathname]);
  // Also recognize the optional /index welcome alias.
  authPaths.add(welcomeUrl.pathname.replace(/\/$/, "") + "/index");
  const candidateReturn = new URL(location.href).searchParams.get("returnTo") || "";
  const returnTo = /^(?:\/product\?id=[1-9][0-9]{0,18}|\/(?:cart|orders|account))$/.test(candidateReturn) ? candidateReturn : "";
  function preserveReturn(url) {
    if (returnTo && authPaths.has(url.pathname)) url.searchParams.set("returnTo", returnTo);
    return url;
  }
  try { history.replaceState({ ...history.state, forestAuth: true }, "", location.href); } catch {}

  function showError(message) {
    const view = panel.querySelector("[data-auth-view]");
    let notice = view.querySelector("[data-client-error]");
    if (!notice) {
      notice = document.createElement("div");
      notice.className = "auth-message auth-message-error";
      notice.dataset.clientError = "";
      notice.setAttribute("role", "alert");
      const heading = view.querySelector(".login-heading, .welcome-heading");
      heading?.insertAdjacentElement("afterend", notice);
    }
    notice.textContent = message;
  }

  async function replacePanel(html, url, replaceHistory = false, username = null) {
    preserveReturn(url);
    const page = new DOMParser().parseFromString(html, "text/html");
    const next = page.querySelector("[data-auth-view]");
    if (!next) throw new Error("Missing authentication panel");
    const current = panel.querySelector("[data-auth-view]");
    await animate(current, [{ opacity: 1 }, { opacity: 0 }],
      { duration: 170, easing: "ease-out", fill: "forwards" });
    // Keep the brand, footer, entire left scene, its canvas and its timeline alive.
    current.replaceWith(next);
    panel.scrollTop = 0;
    const inner = panel.querySelector(".auth-panel-inner");
    inner.classList.toggle("login-inner", next.dataset.authView !== "welcome");
    inner.classList.toggle("welcome-inner", next.dataset.authView === "welcome");
    panel.setAttribute("aria-labelledby", next.getAttribute("aria-labelledby"));
    document.title = page.title;
    if (username !== null) {
      const input = next.querySelector("[name=username]");
      if (input) input.value = username; // Passwords are never copied into the replacement.
    }
    try {
      history[replaceHistory ? "replaceState" : "pushState"](
        { forestAuth: true }, "", url.pathname + url.search + url.hash);
    } catch {}
    await animate(next, [
      { opacity: 0, transform: "translateY(22px) scale(.985)" },
      { opacity: 1, transform: "translateY(0) scale(1)" }
    ], { duration: 380, easing: "cubic-bezier(.16,1,.3,1)", fill: "both" });
    const heading = next.querySelector("h2");
    heading?.setAttribute("tabindex", "-1");
    heading?.focus({ preventScroll: true });
    panel.dispatchEvent(new CustomEvent("forest:auth-panel-changed", {
      detail: { view: next.dataset.authView }
    }));
  }

  async function openPanel(url, replaceHistory = false) {
    preserveReturn(url);
    if (panelBusy || leaving) return;
    if (url.pathname === location.pathname && url.search === location.search) return;
    panelBusy = true;
    panel.setAttribute("aria-busy", "true");
    try {
      const response = await fetch(url, { credentials: "same-origin", cache: "no-store",
        headers: { Accept: "text/html" } });
      const destination = new URL(response.url || url.href);
      if (destination.origin !== location.origin || !response.ok) throw new Error("Panel unavailable");
      if (!authPaths.has(destination.pathname)) { await enterStore(destination); return; }
      await replacePanel(await response.text(), destination, replaceHistory);
    } catch {
      showError("暂时无法打开登录页面，请稍后重试。");
    } finally {
      panelBusy = false;
      panel.removeAttribute("aria-busy");
    }
  }

  async function enterStore(url) {
    if (leaving || url.origin !== location.origin) return;
    leaving = true;
    if (returnTo && window.forestPageTransition) {
      await window.forestPageTransition.leave(url.href);
      return;
    }
    if (window.forestPageTransition) {
      try { sessionStorage.setItem(ENTRY_KEY, JSON.stringify({ at: Date.now(), source: "auth" })); } catch {}
      await window.forestPageTransition.leave(url.href, { splitAuth: true });
      return;
    }
    document.body.classList.add("is-entering-store");
    try { sessionStorage.setItem(ENTRY_KEY, JSON.stringify({ at: Date.now(), source: "auth" })); } catch {}
    try {
      await Promise.all([
        scene ? animate(scene, [
          { opacity: 1, transform: "translateX(0)" },
          { opacity: 0, transform: "translateX(-100%)" }
        ], { duration: 520, easing: "cubic-bezier(.4,0,.2,1)", fill: "forwards" }, true) : Promise.resolve(),
        animate(panel, [
          { opacity: 1, transform: "translateX(0)" },
          { opacity: 0, transform: "translateX(100%)" }
        ], { duration: 520, easing: "cubic-bezier(.4,0,.2,1)", fill: "forwards" }, true)
      ]);
      location.assign(url.href);
    } catch {
      leaving = false;
      document.body.classList.remove("is-entering-store");
      showError("暂时无法进入商城，请稍后重试。");
    }
  }

  document.addEventListener("click", event => {
    const refresh = event.target.closest("[data-captcha-refresh]");
    if (refresh && panel.contains(refresh)) {
      const image = refresh.querySelector("[data-captcha-image]");
      const url = new URL(image.src, location.href);
      url.searchParams.set("t", Date.now()); image.src = url.href;
      return;
    }
    const toggle = event.target.closest("[data-password-toggle]");
    if (toggle && panel.contains(toggle)) {
      const password = panel.querySelector("#password");
      if (!password) return;
      const reveal = password.type === "password";
      password.type = reveal ? "text" : "password";
      toggle.setAttribute("aria-pressed", String(reveal));
      toggle.setAttribute("aria-label", reveal ? "隐藏密码" : "显示密码");
      return;
    }
    const link = event.target.closest("a[data-auth-route], a[data-enter-store]");
    if (!link || event.defaultPrevented || event.button !== 0 || event.ctrlKey || event.metaKey || event.shiftKey || event.altKey) return;
    const url = new URL(link.href, location.href);
    if (url.origin !== location.origin) return;
    event.preventDefault();
    if (link.hasAttribute("data-enter-store") || !authPaths.has(url.pathname)) enterStore(url);
    else openPanel(url);
  });

  document.addEventListener("submit", async event => {
    const form = event.target;
    if (!form.matches("[data-login-form], [data-register-form]")) return;
    event.preventDefault();
    if (panelBusy || leaving || !form.reportValidity()) return;
    const username = form.querySelector("[name=username]").value;
    const payload = new URLSearchParams(new FormData(form));
    if (returnTo) payload.set("returnTo", returnTo);
    const submit = form.querySelector("[data-login-submit]");
    const label = form.querySelector("[data-submit-label]");
    const originalLabel = label?.textContent;
    panelBusy = true;
    panel.setAttribute("aria-busy", "true");
    if (submit) submit.disabled = true;
    if (label) label.textContent = form.hasAttribute("data-register-form") ? "正在创建账号…" : "正在走进森林…";
    try {
      // Spring Security still verifies the real POST, session and CSRF token.
      const response = await fetch(form.action, { method: "POST", body: payload,
        credentials: "same-origin", cache: "no-store", headers: { Accept: "text/html" } });
      const destination = new URL(response.url || form.action);
      if (destination.origin !== location.origin) throw new Error("Unexpected login destination");
      if (!response.ok) {
        showError(response.status === 403 ? "登录页面已过期，请重新打开账号登录后重试。" : "暂时无法完成登录，请稍后重试。");
        return;
      }
      if (!authPaths.has(destination.pathname)) await enterStore(destination);
      else await replacePanel(await response.text(), destination, true, username);
    } catch {
      showError("暂时无法完成登录，请检查连接后重试。");
    } finally {
      panelBusy = false;
      panel.removeAttribute("aria-busy");
      if (submit?.isConnected) submit.disabled = false;
      if (label?.isConnected) label.textContent = originalLabel;
    }
  });

  window.addEventListener("popstate", () => {
    const url = new URL(location.href);
    if (authPaths.has(url.pathname)) {
      // The URL has already changed, so force the correct panel without pushing history.
      const view = panel.querySelector("[data-auth-view]");
      const expected = url.pathname === loginUrl.pathname ? "login" : url.pathname === registerUrl.pathname ? "register" : "welcome";
      if (view.dataset.authView !== expected) {
        panelBusy = false;
        fetch(url, { credentials: "same-origin", cache: "no-store" })
          .then(response => { if (!response.ok) throw new Error(); return response.text(); })
          .then(html => replacePanel(html, url, true))
          .catch(() => showError("暂时无法恢复页面，请稍后重试。"));
      }
    }
  });

  window.addEventListener("pageshow", event => {
    if (!event.persisted) return;
    leaving = false;
    panelBusy = false;
    document.body.classList.remove("is-entering-store");
    panel.removeAttribute("aria-busy");
    runningAnimations.forEach(animation => animation.cancel());
    heldAnimations.forEach(animation => animation.cancel());
    heldAnimations.clear();
    panel.querySelector("[data-login-submit]")?.removeAttribute("disabled");
  });
  motion.addEventListener("change", () => {
    if (motion.matches) runningAnimations.forEach(animation => animation.finish());
  });
})();
