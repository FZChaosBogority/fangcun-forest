package com.fangcun.forest.storefront.controller;

import com.fangcun.forest.storefront.common.PageRequest;
import com.fangcun.forest.storefront.entity.Product;
import com.fangcun.forest.storefront.enums.ProductSort;
import com.fangcun.forest.storefront.service.CatalogService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import java.util.stream.Collectors;

@Controller
public class CatalogController {

    private final CatalogService catalogService;

    public CatalogController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @GetMapping("/main")
    public String home(Model model) {
        var categories = catalogService.findAllCategories();
        model.addAttribute("categories", categories.stream().filter(c -> c.getParentId() == null).toList());
        model.addAttribute("categoryGroups", categories.stream().filter(c -> c.getParentId() != null)
                .collect(Collectors.groupingBy(com.fangcun.forest.storefront.entity.Category::getParentId)));
        model.addAttribute("products", catalogService.findFeaturedProducts());
        model.addAttribute("newest", catalogService.findNewestProducts());
        return "catalog/home";
    }

    @GetMapping("/category")
    public String products(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "6") long size,
            @RequestParam(defaultValue = "DEFAULT") ProductSort sort,
            @RequestParam(defaultValue = "false") boolean inStock,
            Model model) {
        model.addAttribute("result",
                catalogService.findProducts(categoryId, keyword, sort, inStock, new PageRequest(page, size)));
        var categories = catalogService.findAllCategories();
        model.addAttribute("categories", categories);
        model.addAttribute("rootCategories", categories.stream().filter(c -> c.getParentId() == null).toList());
        model.addAttribute("categoryGroups", categories.stream().filter(c -> c.getParentId() != null)
                .collect(Collectors.groupingBy(com.fangcun.forest.storefront.entity.Category::getParentId)));
        model.addAttribute("sort", sort); model.addAttribute("sorts", ProductSort.values());
        model.addAttribute("inStock", inStock);
        model.addAttribute("categoryId", categoryId);
        model.addAttribute("keyword", keyword.strip());
        return "catalog/list";
    }

    @GetMapping("/product")
    public String product(@RequestParam long id, Model model) {
        Product product = catalogService.getOnSaleProduct(id);
        model.addAttribute("product", product);
        model.addAttribute("logoutReturnTo", "/product?id=" + id);
        model.addAttribute("category", catalogService.getCategory(product.getCategoryId()));
        model.addAttribute("images", catalogService.findImages(id));
        return "catalog/detail";
    }
}
