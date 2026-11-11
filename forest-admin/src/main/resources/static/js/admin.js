(() => {
  "use strict";
  document.querySelectorAll("[data-password-form]").forEach(form => {
    const password = form.querySelector("[name=password]");
    const confirmation = form.querySelector("[name=confirmPassword]");
    const validate = () => confirmation.setCustomValidity(password.value === confirmation.value ? "" : "两次输入的密码不一致");
    password.addEventListener("input", validate); confirmation.addEventListener("input", validate);
  });
  document.addEventListener("submit", event => {
    const form = event.target;
    if (form.dataset.confirm && !confirm(form.dataset.confirm)) { event.preventDefault(); return; }
    if (!form.hasAttribute("data-once-submit")) return;
    if (form.dataset.submitting === "true") { event.preventDefault(); return; }
    form.dataset.submitting = "true";
    form.querySelectorAll("button[type=submit]").forEach(button => { button.disabled = true; });
  });
  document.querySelector("[data-cover-input]")?.addEventListener("input", event => {
    const value = event.target.value;
    if (/^\/images\/[A-Za-z0-9_/-]+\.(svg|png|jpg|jpeg|webp)$/.test(value)) document.querySelector("[data-cover-preview]").src = value;
  });
  window.addEventListener("pageshow", () => document.querySelectorAll("[data-once-submit]").forEach(form => {
    delete form.dataset.submitting; form.querySelectorAll("button[type=submit]").forEach(button => { button.disabled = false; });
  }));
})();
