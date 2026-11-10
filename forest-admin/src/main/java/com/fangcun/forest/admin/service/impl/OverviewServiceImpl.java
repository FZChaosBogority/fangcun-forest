package com.fangcun.forest.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fangcun.forest.admin.common.PageRequest;
import com.fangcun.forest.admin.dto.DashboardSummary;
import com.fangcun.forest.admin.entity.Category;
import com.fangcun.forest.admin.entity.Product;
import com.fangcun.forest.admin.enums.ProductStatus;
import com.fangcun.forest.admin.mapper.CategoryMapper;
import com.fangcun.forest.admin.mapper.ProductMapper;
import com.fangcun.forest.admin.mapper.SalesOrderMapper;
import com.fangcun.forest.admin.service.OverviewService;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class OverviewServiceImpl implements OverviewService {

    private final CategoryMapper categoryMapper;
    private final ProductMapper productMapper;
    private final SalesOrderMapper orderMapper;

    public OverviewServiceImpl(CategoryMapper categoryMapper, ProductMapper productMapper,
            SalesOrderMapper orderMapper) {
        this.categoryMapper = categoryMapper;
        this.productMapper = productMapper;
        this.orderMapper = orderMapper;
    }

    @Override
    public DashboardSummary getSummary() {
        return new DashboardSummary(
                categoryMapper.selectCount(null),
                productMapper.selectCount(null),
                productMapper.selectCount(new LambdaQueryWrapper<Product>()
                        .eq(Product::getStatus, ProductStatus.ON_SALE)),
                orderMapper.selectCount(null));
    }

    @Override
    public Page<Product> findProducts(PageRequest request) {
        return productMapper.selectPage(new Page<>(request.page(), request.size()),
                new LambdaQueryWrapper<Product>().orderByDesc(Product::getCreatedAt, Product::getId));
    }

    @Override
    public List<Category> findCategories() {
        return categoryMapper.selectList(new LambdaQueryWrapper<Category>()
                .orderByAsc(Category::getSortOrder, Category::getId));
    }
}
