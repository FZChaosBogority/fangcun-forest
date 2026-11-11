package com.fangcun.forest.admin.controller;

import com.fangcun.forest.admin.common.*;
import com.fangcun.forest.admin.enums.AccountStatus;
import com.fangcun.forest.admin.service.UserService;
import com.fangcun.forest.admin.web.*;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/users")
public class UserController {
    private final UserService users;
    public UserController(UserService users) { this.users = users; }
    @GetMapping
    public String list(@RequestParam(defaultValue = "") String keyword, @RequestParam(defaultValue = "ALL") String status,
            @RequestParam(defaultValue = "1") long page, @RequestParam(defaultValue = "10") long size, Model model) {
        AccountStatus selected = QueryParameters.optionalEnum(AccountStatus.class, status);
        model.addAttribute("result", users.search(keyword, selected, new PageRequest(page, size)));
        model.addAttribute("keyword", keyword.strip()); model.addAttribute("status", selected); return "users/list";
    }
    @GetMapping("/{id}")
    public String edit(@PathVariable long id, Model model) {
        var user = users.get(id); model.addAttribute("form", UserForm.from(user, UserService.revision(user)));
        model.addAttribute("passwordForm", passwordForm(UserService.revision(user))); return detail(id, model);
    }
    @PostMapping("/{id}/edit")
    public String edit(@PathVariable long id, @Valid @ModelAttribute("form") UserForm form, BindingResult errors, Model model, RedirectAttributes redirect) {
        if (!errors.hasErrors()) try { users.update(id, form); redirect.addFlashAttribute("success", "用户资料已更新。"); return "redirect:/users/" + id; }
        catch (BusinessException exception) { errors.reject("business", exception.getMessage()); }
        model.addAttribute("passwordForm", passwordForm(UserService.revision(users.get(id)))); return detail(id, model);
    }
    @PostMapping("/{id}/reset-password")
    public String reset(@PathVariable long id, @Valid @ModelAttribute("passwordForm") PasswordForm form, BindingResult errors, Model model, RedirectAttributes redirect) {
        if (!errors.hasErrors()) try {
            users.resetPassword(id, form); form.clearSecrets(); redirect.addFlashAttribute("success", "密码已重置，旧登录会话将在下次请求时失效。"); return "redirect:/users/" + id;
        } catch (BusinessException exception) { errors.reject("business", exception.getMessage()); }
        form.clearSecrets(); var user = users.get(id); model.addAttribute("form", UserForm.from(user, UserService.revision(user))); return detail(id, model);
    }
    private PasswordForm passwordForm(String revision) { PasswordForm form = new PasswordForm(); form.setRevision(revision); return form; }
    private String detail(long id, Model model) { model.addAttribute("user", users.get(id)); return "users/detail"; }
}
