package com.fangcun.forest.storefront.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fangcun.forest.storefront.common.PageRequest;
import com.fangcun.forest.storefront.entity.Category;
import com.fangcun.forest.storefront.entity.Product;
import com.fangcun.forest.storefront.entity.ProductImage;
import com.fangcun.forest.storefront.enums.ProductSort;
import java.util.List;

public interface CatalogService {

    List<Category> findRootCategories();
    List<Category> findAllCategories();
    List<Product> findFeaturedProducts();
    List<Product> findNewestProducts();
    Page<Product> findProducts(Long categoryId, String keyword, ProductSort sort, boolean inStock, PageRequest request);
    Product getOnSaleProduct(long id);
    Category getCategory(long id);
    List<ProductImage> findImages(long productId);
}
