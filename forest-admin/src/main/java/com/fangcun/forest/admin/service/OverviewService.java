package com.fangcun.forest.admin.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fangcun.forest.admin.common.PageRequest;
import com.fangcun.forest.admin.dto.DashboardSummary;
import com.fangcun.forest.admin.entity.Category;
import com.fangcun.forest.admin.entity.Product;
import java.util.List;

public interface OverviewService {

    DashboardSummary getSummary();
    Page<Product> findProducts(PageRequest request);
    List<Category> findCategories();
}
