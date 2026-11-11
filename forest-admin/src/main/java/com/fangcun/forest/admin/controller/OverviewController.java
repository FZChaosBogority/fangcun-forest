package com.fangcun.forest.admin.controller;

import com.fangcun.forest.admin.service.OverviewService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

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

}
