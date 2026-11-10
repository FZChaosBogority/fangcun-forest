package com.fangcun.forest.admin.controller;

import com.fangcun.forest.admin.common.PageRequest;
import com.fangcun.forest.admin.service.OverviewService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class OverviewController {

    private final OverviewService overviewService;

    public OverviewController(OverviewService overviewService) {
        this.overviewService = overviewService;
    }

    @GetMapping("/")
    public String dashboard(Model model) {
        model.addAttribute("summary", overviewService.getSummary());
        return "overview/dashboard";
    }

    @GetMapping("/products")
    public String products(@RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size, Model model) {
        model.addAttribute("result", overviewService.findProducts(new PageRequest(page, size)));
        return "overview/products";
    }

    @GetMapping("/categories")
    public String categories(Model model) {
        model.addAttribute("categories", overviewService.findCategories());
        return "overview/categories";
    }
}
