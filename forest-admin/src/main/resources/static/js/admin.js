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
    if (form.dataset.coverUploadState === "uploading" || form.dataset.coverUploadState === "error") {
      event.preventDefault();
      form.querySelector("[data-cover-status]")?.focus();
      return;
    }
    if (form.dataset.confirm && !confirm(form.dataset.confirm)) { event.preventDefault(); return; }
    if (!form.hasAttribute("data-once-submit")) return;
    if (form.dataset.submitting === "true") { event.preventDefault(); return; }
    form.dataset.submitting = "true";
    form.querySelectorAll("button[type=submit]").forEach(button => { button.disabled = true; });
  });
  document.querySelectorAll("[data-cover-form]").forEach(form => {
    const picker = form.querySelector("[data-cover-file]");
    const input = form.querySelector("[data-cover-input]");
    const preview = document.querySelector("[data-cover-preview]");
    const status = form.querySelector("[data-cover-status]");
    const retry = form.querySelector("[data-cover-retry]");
    const cancel = form.querySelector("[data-cover-cancel]");
    const save = form.querySelector("[data-product-save]");
    const localImage = /^\/images\/[A-Za-z0-9_/-]+\.(svg|png|jpg|jpeg|webp)$/;
    let selected = null, controller = null, revision = 0;
    status.tabIndex = -1;
    function state(value, message) {
      form.dataset.coverUploadState = value;
      status.textContent = message;
      status.classList.toggle("danger-text", value === "error");
      status.classList.toggle("muted", value !== "error");
      picker.setAttribute("aria-invalid", String(value === "error"));
      retry.hidden = value !== "error" || !selected;
      cancel.hidden = value !== "uploading" && value !== "error";
      save.disabled = value === "uploading" || form.dataset.submitting === "true";
      form.querySelector(".cover-upload").setAttribute("aria-busy", String(value === "uploading"));
    }
    function reset() {
      revision++; controller?.abort(); controller = null; selected = null; picker.value = "";
      state("idle", "已取消本次选择，保存时使用当前封面路径。");
    }
    async function upload(file) {
      const current = ++revision;
      controller?.abort();
      controller = new AbortController();
      selected = file;
      if (!file.size || file.size > 5 * 1024 * 1024) {
        selected = null;
        state("error", "请选择不超过 5MB 的 JPG 或 PNG 图片，或取消本次选择后使用原封面。"); return;
      }
      if (!/\.(png|jpe?g)$/i.test(file.name)) {
        selected = null;
        state("error", "仅支持 JPG 或 PNG 图片，请重新选择，或取消本次选择。"); return;
      }
      const csrf = form.querySelector("input[name=_csrf]")?.value;
      if (!csrf || !form.dataset.csrfHeader) {
        state("error", "页面验证信息已失效，请刷新页面后重新上传。"); return;
      }
      state("uploading", "正在上传封面，请稍候…上传完成前不能保存商品。");
      const body = new FormData(); body.append("file", file);
      try {
        const response = await fetch(form.dataset.uploadUrl, {
          method: "POST", body, credentials: "same-origin", signal: controller.signal,
          headers: { Accept: "application/json", [form.dataset.csrfHeader]: csrf }
        });
        if (current !== revision) return;
        if (response.redirected || response.status === 401 || response.status === 403) {
          throw new Error("管理员登录或页面验证已失效，请重新登录后上传。");
        }
        const json = response.headers.get("Content-Type")?.includes("application/json") ? await response.json() : null;
        if (current !== revision) return;
        if (!response.ok) throw new Error(json?.message || (response.status === 413 ? "图片不能超过 5MB。" : "上传失败，请稍后重试。"));
        if (!json || !localImage.test(json.path)) throw new Error("上传结果无效，请重试。");
        input.value = json.path; preview.src = json.path;
        selected = null; picker.value = "";
        state("success", "封面上传成功，预览已更新。请点击保存商品资料，使封面生效。");
      } catch (error) {
        if (current !== revision || error.name === "AbortError") return;
        const message = error instanceof TypeError ? "网络连接失败，请检查连接后重试。" : error.message;
        state("error", message + " 当前封面尚未更改；可重试、重新选择或取消本次选择。");
      }
    }
    picker.addEventListener("change", () => { if (picker.files[0]) upload(picker.files[0]); });
    retry.addEventListener("click", () => { if (selected) upload(selected); });
    cancel.addEventListener("click", reset);
    input.addEventListener("input", () => { reset(); if (localImage.test(input.value)) preview.src = input.value; });
  });
  window.addEventListener("pageshow", () => document.querySelectorAll("[data-once-submit]").forEach(form => {
    delete form.dataset.submitting; form.querySelectorAll("button[type=submit]").forEach(button => { button.disabled = form.dataset.coverUploadState === "uploading"; });
  }));
})();
