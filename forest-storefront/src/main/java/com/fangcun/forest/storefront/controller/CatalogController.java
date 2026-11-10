package com.fangcun.forest.storefront.controller;

import com.fangcun.forest.storefront.common.PageRequest;
import com.fangcun.forest.storefront.entity.Product;
import com.fangcun.forest.storefront.service.CatalogService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class CatalogController {

    private final CatalogService catalogService;

    public CatalogController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @GetMapping({"/", "/main"})
    public String home(Model model) {
        model.addAttribute("categories", catalogService.findRootCategories());
        model.addAttribute("products", catalogService.findFeaturedProducts());
        return "catalog/home";
    }

    @GetMapping("/category")
    public String products(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "6") long size,
            Model model) {
        model.addAttribute("result",
                catalogService.findProducts(categoryId, keyword, new PageRequest(page, size)));
        model.addAttribute("categories", catalogService.findRootCategories());
        model.addAttribute("categoryId", categoryId);
        model.addAttribute("keyword", keyword.strip());
        return "catalog/list";
    }

    @GetMapping("/product")
    public String product(@RequestParam long id, Model model) {
        Product product = catalogService.getOnSaleProduct(id);
        model.addAttribute("product", product);
        model.addAttribute("category", catalogService.getCategory(product.getCategoryId()));
        model.addAttribute("images", catalogService.findImages(id));
        return "catalog/detail";
    }
}
