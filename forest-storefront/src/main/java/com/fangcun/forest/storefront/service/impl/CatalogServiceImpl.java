package com.fangcun.forest.storefront.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fangcun.forest.storefront.common.BusinessException;
import com.fangcun.forest.storefront.common.PageRequest;
import com.fangcun.forest.storefront.entity.Category;
import com.fangcun.forest.storefront.entity.Product;
import com.fangcun.forest.storefront.entity.ProductImage;
import com.fangcun.forest.storefront.enums.ProductStatus;
import com.fangcun.forest.storefront.enums.ProductSort;
import com.fangcun.forest.storefront.mapper.CategoryMapper;
import com.fangcun.forest.storefront.mapper.ProductImageMapper;
import com.fangcun.forest.storefront.mapper.ProductMapper;
import com.fangcun.forest.storefront.service.CatalogService;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class CatalogServiceImpl implements CatalogService {

    private final CategoryMapper categoryMapper;
    private final ProductMapper productMapper;
    private final ProductImageMapper productImageMapper;

    public CatalogServiceImpl(CategoryMapper categoryMapper, ProductMapper productMapper,
            ProductImageMapper productImageMapper) {
        this.categoryMapper = categoryMapper;
        this.productMapper = productMapper;
        this.productImageMapper = productImageMapper;
    }

    @Override
    public List<Category> findRootCategories() {
        return categoryMapper.selectList(new LambdaQueryWrapper<Category>()
                .isNull(Category::getParentId)
                .orderByAsc(Category::getSortOrder, Category::getId));
    }

    @Override
    public List<Product> findFeaturedProducts() {
        return productMapper.selectPage(new Page<Product>(1, 4, false), onSaleQuery().orderByAsc(Product::getSortOrder, Product::getId))
                .getRecords();
    }

    @Override
    public List<Category> findAllCategories() {
        return categoryMapper.selectList(new LambdaQueryWrapper<Category>().orderByAsc(Category::getSortOrder, Category::getId));
    }

    @Override
    public List<Product> findNewestProducts() {
        return productMapper.selectPage(new Page<Product>(1, 3, false), onSaleQuery()
                .orderByDesc(Product::getCreatedAt, Product::getId)).getRecords();
    }

    @Override
    public Page<Product> findProducts(Long categoryId, String keyword, ProductSort sort, boolean inStock, PageRequest request) {
        LambdaQueryWrapper<Product> query = onSaleQuery();
        query.gt(inStock, Product::getStock, 0);
        switch (sort) {
            case PRICE_ASC -> query.orderByAsc(Product::getPrice, Product::getId);
            case PRICE_DESC -> query.orderByDesc(Product::getPrice).orderByAsc(Product::getId);
            case NEWEST -> query.orderByDesc(Product::getCreatedAt, Product::getId);
            default -> query.orderByAsc(Product::getSortOrder, Product::getId);
        }
        String normalized = keyword == null ? "" : keyword.strip();
        if (normalized.length() > 120) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "商品名称搜索不能超过 120 个字符。");
        }
        if (!normalized.isEmpty()) {
            // LIKE 元字符按普通输入处理，查询值由 MyBatis 参数绑定。
            String escaped = normalized.replace("\\", "\\\\").replace("%", "\\%")
                    .replace("_", "\\_");
            query.like(Product::getName, escaped);
        }
        if (categoryId != null) {
            Category category = getCategory(categoryId);
            if (category.getParentId() == null) {
                List<Long> children = categoryMapper.selectList(new LambdaQueryWrapper<Category>()
                                .eq(Category::getParentId, categoryId))
                        .stream().map(Category::getId).toList();
                if (children.isEmpty()) {
                    query.eq(Product::getCategoryId, -1L);
                } else {
                    query.in(Product::getCategoryId, children);
                }
            } else {
                query.eq(Product::getCategoryId, categoryId);
            }
        }
        return productMapper.selectPage(new Page<>(request.page(), request.size()), query);
    }

    @Override
    public Product getOnSaleProduct(long id) {
        Product product = productMapper.selectById(id);
        if (product == null || product.getStatus() != ProductStatus.ON_SALE) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "商品不存在或暂未上架。");
        }
        return product;
    }

    @Override
    public Category getCategory(long id) {
        Category category = categoryMapper.selectById(id);
        if (category == null) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "分类不存在或不可用。");
        }
        return category;
    }

    @Override
    public List<ProductImage> findImages(long productId) {
        return productImageMapper.selectList(new LambdaQueryWrapper<ProductImage>()
                .eq(ProductImage::getProductId, productId)
                .orderByAsc(ProductImage::getSortOrder, ProductImage::getId));
    }

    private LambdaQueryWrapper<Product> onSaleQuery() {
        return new LambdaQueryWrapper<Product>().eq(Product::getStatus, ProductStatus.ON_SALE);
    }
}
