package com.fangcun.forest.admin.controller;

import com.fangcun.forest.admin.common.BusinessException;
import com.fangcun.forest.admin.common.QueryTerms;
import com.fangcun.forest.admin.entity.Category;
import com.fangcun.forest.admin.service.CategoryService;
import com.fangcun.forest.admin.web.CategoryForm;
import jakarta.validation.Valid;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/categories")
public class CategoryController {
    private final CategoryService categories;
    public CategoryController(CategoryService categories) { this.categories = categories; }
    @GetMapping
    public String list(@RequestParam(defaultValue = "") String keyword, Model model) {
        QueryTerms.escape(keyword, 80);
        var all = categories.all();
        model.addAttribute("categories", all.stream().filter(c -> c.getName().contains(keyword.strip()) || c.getCode().contains(keyword.strip())).toList());
        model.addAttribute("names", all.stream().collect(Collectors.toMap(Category::getId, Category::getName)));
        model.addAttribute("revisions", all.stream().collect(Collectors.toMap(Category::getId, CategoryService::revision)));
        model.addAttribute("keyword", keyword.strip()); return "overview/categories";
    }
    @GetMapping("/new")
    public String create(Model model) { model.addAttribute("form", new CategoryForm()); return form(null, model); }
    @GetMapping("/{id}/edit")
    public String edit(@PathVariable long id, Model model) {
        Category category = categories.get(id); model.addAttribute("form", CategoryForm.from(category, CategoryService.revision(category)));
        return form(id, model);
    }
    @PostMapping("/new")
    public String create(@Valid @ModelAttribute("form") CategoryForm form, BindingResult errors, Model model, RedirectAttributes redirect) {
        if (!errors.hasErrors()) try { categories.create(form); redirect.addFlashAttribute("success", "分类已创建。"); return "redirect:/categories"; }
        catch (BusinessException exception) { errors.reject("business", exception.getMessage()); }
        return form(null, model);
    }
    @PostMapping("/{id}/edit")
    public String edit(@PathVariable long id, @Valid @ModelAttribute("form") CategoryForm form, BindingResult errors, Model model, RedirectAttributes redirect) {
        if (!errors.hasErrors()) try { categories.update(id, form); redirect.addFlashAttribute("success", "分类已更新。"); return "redirect:/categories"; }
        catch (BusinessException exception) { errors.reject("business", exception.getMessage()); }
        return form(id, model);
    }
    @PostMapping("/{id}/delete")
    public String delete(@PathVariable long id, @RequestParam String revision, RedirectAttributes redirect) {
        try { categories.delete(id, revision); redirect.addFlashAttribute("success", "分类已删除。"); }
        catch (BusinessException exception) { redirect.addFlashAttribute("error", exception.getMessage()); }
        return "redirect:/categories";
    }
    private String form(Long id, Model model) {
        model.addAttribute("id", id); model.addAttribute("parents", categories.all().stream().filter(c -> c.getParentId() == null && !Objects.equals(c.getId(), id)).toList());
        return "categories/form";
    }
}
