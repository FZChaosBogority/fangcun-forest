package com.fangcun.forest.admin.controller;

import com.fangcun.forest.admin.common.*;
import com.fangcun.forest.admin.entity.ProductImage;
import com.fangcun.forest.admin.enums.ProductStatus;
import com.fangcun.forest.admin.service.CategoryService;
import com.fangcun.forest.admin.service.ProductService;
import com.fangcun.forest.admin.web.ProductForm;
import jakarta.validation.Valid;
import java.util.stream.Collectors;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/products")
public class ProductController {
    private final ProductService products;
    private final CategoryService categories;
    public ProductController(ProductService products, CategoryService categories) { this.products = products; this.categories = categories; }
    @GetMapping
    public String list(@RequestParam(defaultValue = "") String keyword, @RequestParam(required = false) Long categoryId,
            @RequestParam(defaultValue = "ALL") String status, @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size, Model model) {
        ProductStatus selected = QueryParameters.optionalEnum(ProductStatus.class, status);
        model.addAttribute("result", products.search(keyword, categoryId, selected, new PageRequest(page, size)));
        model.addAttribute("categories", categories.all()); model.addAttribute("keyword", keyword.strip());
        model.addAttribute("categoryId", categoryId); model.addAttribute("status", selected); return "overview/products";
    }
    @GetMapping("/new") public String create(Model model) { model.addAttribute("form", new ProductForm()); return form(null, model); }
    @GetMapping("/{id}/edit") public String edit(@PathVariable long id, Model model) {
        model.addAttribute("form", ProductForm.from(products.get(id), products.images(id).stream().map(ProductImage::getImagePath).collect(Collectors.joining("\n"))));
        return form(id, model);
    }
    @PostMapping("/new")
    public String create(@Valid @ModelAttribute("form") ProductForm form, BindingResult errors, Model model, RedirectAttributes redirect) {
        if (!errors.hasErrors()) try { long id = products.create(form); redirect.addFlashAttribute("success", "商品已保存为未上架，可检查后上架。"); return "redirect:/products/" + id + "/edit"; }
        catch (BusinessException exception) { errors.reject("business", exception.getMessage()); }
        return form(null, model);
    }
    @PostMapping("/{id}/edit")
    public String edit(@PathVariable long id, @Valid @ModelAttribute("form") ProductForm form, BindingResult errors, Model model, RedirectAttributes redirect) {
        if (!errors.hasErrors()) try { products.update(id, form); redirect.addFlashAttribute("success", "商品资料已更新，库存未被覆盖。"); return "redirect:/products/" + id + "/edit"; }
        catch (BusinessException exception) { errors.reject("business", exception.getMessage()); }
        return form(id, model);
    }
    @PostMapping("/{id}/status")
    public String status(@PathVariable long id, @RequestParam int version, @RequestParam ProductStatus status, RedirectAttributes redirect) {
        try { products.status(id, version, status); redirect.addFlashAttribute("success", status == ProductStatus.ON_SALE ? "商品已上架。" : "商品已下架。"); }
        catch (BusinessException exception) { redirect.addFlashAttribute("error", exception.getMessage()); }
        return "redirect:/products/" + id + "/edit";
    }
    @PostMapping("/{id}/restock")
    public String restock(@PathVariable long id, @RequestParam int quantity, RedirectAttributes redirect) {
        try { products.restock(id, quantity); redirect.addFlashAttribute("success", "补货已完成。"); }
        catch (BusinessException exception) { redirect.addFlashAttribute("error", exception.getMessage()); }
        return "redirect:/products/" + id + "/edit";
    }
    @PostMapping("/{id}/delete")
    public String delete(@PathVariable long id, @RequestParam int version, RedirectAttributes redirect) {
        try { products.delete(id, version); redirect.addFlashAttribute("success", "商品已删除。"); return "redirect:/products"; }
        catch (BusinessException exception) { redirect.addFlashAttribute("error", exception.getMessage()); return "redirect:/products/" + id + "/edit"; }
    }
    private String form(Long id, Model model) {
        model.addAttribute("id", id); model.addAttribute("product", id == null ? null : products.get(id));
        model.addAttribute("categories", categories.all().stream().filter(c -> c.getParentId() != null).toList()); return "products/form";
    }
}
