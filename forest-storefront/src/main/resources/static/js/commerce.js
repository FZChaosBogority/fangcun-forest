(() => {
  "use strict";
  const PRODUCT_DRAFT = "forest-product-draft";
  let logoutPending = false;
  let logoutPopover = null;
  let logoutAnchor = null;

  function closeLogout(restoreFocus = true) {
    if (logoutPending) return;
    if (logoutPopover) logoutPopover.hidden = true;
    logoutAnchor?.setAttribute("aria-expanded", "false");
    if (restoreFocus) logoutAnchor?.focus({ preventScroll: true });
  }

  function showLogout(form) {
    if (logoutPending) return;
    logoutPopover = form.closest(".nav-account")?.querySelector("[data-logout-popconfirm]");
    logoutAnchor = form.querySelector("[data-logout-trigger]");
    if (!logoutPopover) return;
    if (!logoutPopover.hidden) { closeLogout(); return; }
    logoutPopover.hidden = false;
    logoutPopover.querySelector("[data-logout-error]").hidden = true;
    logoutAnchor.setAttribute("aria-expanded", "true");
    logoutPopover.querySelector("[data-logout-cancel]").focus();
  }

  function publicPage() {
    const main = new URL(document.querySelector(".site-header").dataset.mainUrl, location.href);
    const base = main.pathname.replace(/\/main$/, "");
    return ["/main", "/category", "/product"].some(path => location.pathname === base + path)
      ? new URL(location.href) : main;
  }

  async function confirmLogout() {
    if (logoutPending || !logoutPopover) return;
    const form = document.querySelector("[data-logout-form]");
    const controls = logoutPopover.querySelectorAll("button");
    const notice = logoutPopover.querySelector("[data-logout-error]");
    const destination = publicPage();
    logoutPending = true;
    controls.forEach(button => { button.disabled = true; });
    let loggedOut = false;
    try {
      const response = await fetch(form.action, { method: "POST", body: new URLSearchParams(new FormData(form)),
        credentials: "same-origin", cache: "no-store", headers: { Accept: "application/json" } });
      if (response.status !== 204) throw new Error(response.status === 403 ? "页面已过期，请刷新后重试。" : "暂时无法退出，请稍后重试。");
      loggedOut = true;
      // Personal pages require authentication; after logout they return to the public catalog.
      if (destination.pathname !== location.pathname) { location.replace(destination.href); return; }
      const pageResponse = await fetch(destination, { credentials: "same-origin", cache: "no-store", headers: { Accept: "text/html" } });
      if (!pageResponse.ok) throw new Error("Guest page unavailable");
      const page = new DOMParser().parseFromString(await pageResponse.text(), "text/html");
      const incomingHeader = page.querySelector(".site-header");
      if (!incomingHeader || incomingHeader.querySelector("[data-logout-form]")) throw new Error("Missing guest controls");
      const quantity = document.querySelector("#quantity")?.value;
      const position = scrollY;
      incomingHeader.querySelectorAll("script").forEach(script => script.remove());
      incomingHeader.classList.add("is-store-visible");
      document.querySelector(".site-header").replaceWith(incomingHeader);
      const currentPurchase = document.querySelector("[data-purchase-controls]");
      const incomingPurchase = page.querySelector("[data-purchase-controls]");
      if (currentPurchase && incomingPurchase) currentPurchase.replaceWith(incomingPurchase);
      const input = document.querySelector("#quantity");
      if (input && quantity) input.value = String(Math.max(1, Math.min(Number(input.max) || 99, Number(quantity) || 1)));
      window.scrollTo({ top: position, behavior: "instant" });
      const toast = document.createElement("div");
      toast.className = "commerce-toast"; toast.setAttribute("role", "status");
      toast.textContent = "已退出登录，当前以游客身份浏览。";
      document.body.append(toast); setTimeout(() => toast.remove(), 2600);
      incomingHeader.querySelector("nav a:last-of-type")?.focus({ preventScroll: true });
    } catch (error) {
      // If logout succeeded, a normal public-page load also replaces every expired CSRF token.
      if (loggedOut) location.replace(destination.href);
      else { notice.textContent = error.message; notice.hidden = false; }
    } finally {
      logoutPending = false;
      controls.forEach(button => { button.disabled = false; });
    }
  }

  function restoreProductDraft() {
    const purchase = document.querySelector("[data-purchase-controls]");
    if (!purchase || purchase.querySelector("[data-login-required]")) return;
    try {
      const draft = JSON.parse(sessionStorage.getItem(PRODUCT_DRAFT) || "null");
      if (!draft || draft.productId !== purchase.dataset.productId || Date.now() - draft.at > 1800000) return;
      const input = purchase.querySelector("#quantity");
      if (input) input.value = String(Math.max(1, Math.min(Number(input.max) || 99, Number(draft.quantity) || 1)));
      sessionStorage.removeItem(PRODUCT_DRAFT);
      requestAnimationFrame(() => window.scrollTo({ top: draft.position || 0, behavior: "instant" }));
    } catch {}
  }

  document.addEventListener("click", event => {
    if (event.target.closest("[data-logout-cancel]")) closeLogout();
    if (event.target.closest("[data-logout-confirm]")) confirmLogout();
  });
  document.addEventListener("keydown", event => {
    if (event.key === "Escape" && logoutPopover && !logoutPopover.hidden) { event.preventDefault(); closeLogout(); }
  });
  document.addEventListener("pointerdown", event => {
    if (logoutPopover && !logoutPopover.hidden && !event.target.closest(".nav-account")) closeLogout(false);
  });
  restoreProductDraft();
  const staticScript = document.querySelector('script[src$="/js/commerce.js"]');
  const fallbackImage = staticScript ? new URL("../images/terrarium.svg", staticScript.src).href : "/images/terrarium.svg";
  function recoverImage(image) {
    if (!(image instanceof HTMLImageElement) || image.dataset.fallbackUsed) return;
    image.dataset.fallbackUsed = "true";
    image.src = fallbackImage;
  }
  document.addEventListener("error", event => recoverImage(event.target), true);
  document.querySelectorAll("img").forEach(image => {
    if (!image.getAttribute("src") || (image.complete && !image.naturalWidth)) recoverImage(image);
  });
  document.querySelectorAll("[data-gallery-image]").forEach(button => button.addEventListener("click", () => {
    const image = document.querySelector("[data-gallery-main]");
    if (image) { image.src = button.dataset.src; image.alt = button.dataset.alt; }
  }));
  document.addEventListener("submit", event => {
    const form = event.target;
    if (form.matches("[data-logout-form]")) { event.preventDefault(); showLogout(form); return; }
    if (form.matches("[data-login-required]")) {
      event.preventDefault();
      const url = new URL(form.action, location.href);
      url.searchParams.set("returnTo", form.querySelector("[name=returnTo]").value);
      const purchase = form.closest("[data-purchase-controls]");
      try { sessionStorage.setItem(PRODUCT_DRAFT, JSON.stringify({ productId: purchase.dataset.productId,
        quantity: form.querySelector("[name=quantity]").value, position: scrollY, at: Date.now() })); } catch {}
      if (window.forestPageTransition) window.forestPageTransition.leave(url.href);
      else location.assign(url.href);
      return;
    }
    if (form.dataset.confirm && !confirm(form.dataset.confirm)) { event.preventDefault(); return; }
    if (!form.hasAttribute("data-once-submit")) return;
    if (form.dataset.submitting === "true") { event.preventDefault(); return; }
    form.dataset.submitting = "true";
    form.querySelectorAll("button[type=submit]").forEach(button => { button.disabled = true; });
  });
  window.addEventListener("pageshow", () => document.querySelectorAll("[data-once-submit]").forEach(form => {
    delete form.dataset.submitting;
    form.querySelectorAll("button[type=submit]").forEach(button => { button.disabled = false; });
  }));
})();
