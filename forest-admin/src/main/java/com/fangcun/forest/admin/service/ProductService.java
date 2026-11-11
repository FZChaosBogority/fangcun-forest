package com.fangcun.forest.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fangcun.forest.admin.aspect.AdminAction;
import com.fangcun.forest.admin.common.*;
import com.fangcun.forest.admin.entity.*;
import com.fangcun.forest.admin.enums.ProductStatus;
import com.fangcun.forest.admin.mapper.*;
import com.fangcun.forest.admin.web.ProductForm;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ProductService {
    private final ProductMapper products;
    private final CategoryMapper categories;
    private final ProductImageMapper images;
    private final CartItemMapper carts;
    private final SalesOrderItemMapper orderItems;
    public ProductService(ProductMapper products, CategoryMapper categories, ProductImageMapper images,
            CartItemMapper carts, SalesOrderItemMapper orderItems) {
        this.products = products; this.categories = categories; this.images = images; this.carts = carts; this.orderItems = orderItems;
    }
    public Page<Product> search(String keyword, Long categoryId, ProductStatus status, PageRequest page) {
        String term = QueryTerms.escape(keyword, 120);
        var query = new LambdaQueryWrapper<Product>().eq(status != null, Product::getStatus, status);
        if (!term.isEmpty()) query.and(w -> w.like(Product::getName, term).or().like(Product::getSku, term));
        if (categoryId != null) {
            Category category = categories.selectById(categoryId);
            if (category == null) throw missing("分类不存在。");
            if (category.getParentId() != null) query.eq(Product::getCategoryId, categoryId);
            else {
                List<Long> children = categories.selectList(new LambdaQueryWrapper<Category>().eq(Category::getParentId, categoryId))
                        .stream().map(Category::getId).toList();
                if (children.isEmpty()) query.eq(Product::getCategoryId, -1L); else query.in(Product::getCategoryId, children);
            }
        }
        return products.selectPage(new Page<>(page.page(), page.size()), query.orderByDesc(Product::getCreatedAt, Product::getId));
    }
    public Product get(long id) { Product product = products.selectById(id); if (product == null) throw missing("商品不存在。"); return product; }
    public List<ProductImage> images(long id) {
        return images.selectList(new LambdaQueryWrapper<ProductImage>().eq(ProductImage::getProductId, id).orderByAsc(ProductImage::getSortOrder, ProductImage::getId));
    }
    @Transactional
    @AdminAction("product.create")
    public long create(ProductForm form) {
        leaf(form.getCategoryId()); List<String> gallery = validateImages(form);
        Product product = new Product(); product.setSku(form.getSku()); product.setCategoryId(form.getCategoryId());
        product.setName(form.getName().strip()); product.setSummary(form.getSummary()); product.setDescription(form.getDescription());
        product.setCoverImage(form.getCoverImage()); product.setPrice(form.getPrice()); product.setSortOrder(form.getSortOrder());
        product.setStock(form.getInitialStock()); product.setStatus(ProductStatus.DRAFT); product.setVersion(0);
        try { products.insert(product); } catch (DuplicateKeyException exception) { throw conflict("SKU 已存在。"); }
        saveImages(product.getId(), gallery); return product.getId();
    }
    @Transactional
    @AdminAction("product.update")
    public void update(long id, ProductForm form) {
        leaf(form.getCategoryId()); List<String> gallery = validateImages(form);
        Product product = lock(id, form.getVersion());
        try {
            int count = products.update(null, version(product).set(Product::getSku, form.getSku())
                .set(Product::getCategoryId, form.getCategoryId()).set(Product::getName, form.getName().strip())
                .set(Product::getSummary, form.getSummary()).set(Product::getDescription, form.getDescription())
                .set(Product::getCoverImage, form.getCoverImage()).set(Product::getPrice, form.getPrice())
                .set(Product::getSortOrder, form.getSortOrder()));
            if (count != 1) throw conflict("商品已变化，请刷新重试。");
        } catch (DuplicateKeyException exception) { throw conflict("SKU 已存在。"); }
        images.delete(new LambdaQueryWrapper<ProductImage>().eq(ProductImage::getProductId, id)); saveImages(id, gallery);
    }
    @Transactional
    @AdminAction("product.status")
    public void status(long id, int expected, ProductStatus status) {
        if (status == null) throw conflict("请选择商品状态。");
        Product product = lock(id, expected);
        if (product.getStatus() == status) return;
        Category category = categories.selectById(product.getCategoryId());
        if (category == null || category.getParentId() == null) throw conflict("商品必须归属二级分类。");
        if (products.update(null, version(product).set(Product::getStatus, status)) != 1) throw conflict("商品已变化，请刷新。");
    }
    @Transactional
    @AdminAction("product.restock")
    public void restock(long id, int expected, int quantity) {
        if (quantity < 1 || quantity > 1000000) throw conflict("补货数量须为 1—1000000。");
        Product product = lock(id, expected);
        if (products.restock(id, quantity, product.getVersion()) != 1) throw conflict("库存超出上限或商品已变化。");
    }
    @Transactional
    @AdminAction("product.delete")
    public void delete(long id, int expected) {
        Product product = lock(id, expected);
        if (product.getStatus() != ProductStatus.DRAFT) throw conflict("请先下架商品，再删除。");
        if (carts.selectCount(new LambdaQueryWrapper<CartItem>().eq(CartItem::getProductId, id)) > 0
                || orderItems.selectCount(new LambdaQueryWrapper<SalesOrderItem>().eq(SalesOrderItem::getProductId, id)) > 0) {
            throw conflict("商品已被购物车或订单引用，请保留下架状态。");
        }
        images.delete(new LambdaQueryWrapper<ProductImage>().eq(ProductImage::getProductId, id));
        if (products.delete(new LambdaQueryWrapper<Product>().eq(Product::getId, id).eq(Product::getVersion, expected)) != 1) throw conflict("商品已变化。");
    }
    private void leaf(long id) {
        Category category = categories.lockById(id);
        if (category == null || category.getParentId() == null) throw conflict("请选择有效的二级分类。");
    }
    private Product lock(long id, int expected) {
        Product product = products.lockById(id);
        if (product == null) throw missing("商品不存在。");
        if (product.getVersion() != expected) throw conflict("商品或库存已变化，请刷新页面后重试。");
        return product;
    }
    private LambdaUpdateWrapper<Product> version(Product product) {
        return new LambdaUpdateWrapper<Product>().eq(Product::getId, product.getId()).eq(Product::getVersion, product.getVersion())
                .set(Product::getVersion, product.getVersion() + 1).set(Product::getUpdatedAt, LocalDateTime.now());
    }
    private List<String> validateImages(ProductForm form) {
        imagePath(form.getCoverImage());
        List<String> gallery = form.getImages() == null ? List.of() : form.getImages().lines().map(String::strip).filter(s -> !s.isEmpty()).distinct().toList();
        if (gallery.size() > 8) throw conflict("相册最多 8 张图片。");
        gallery.forEach(this::imagePath); return gallery;
    }
    private void imagePath(String value) {
        if (value == null || value.length() > 255 || !value.matches("/images/[A-Za-z0-9_/-]+\\.(svg|png|jpg|jpeg|webp)")) {
            throw conflict("图片路径须为 /images/ 下的本地 svg、png、jpg 或 webp 文件。");
        }
    }
    private void saveImages(long id, List<String> gallery) {
        for (int i = 0; i < gallery.size(); i++) {
            ProductImage image = new ProductImage(); image.setProductId(id); image.setImagePath(gallery.get(i));
            image.setAltText("商品细节 " + (i + 1)); image.setSortOrder(i); images.insert(image);
        }
    }
    private BusinessException conflict(String text) { return new BusinessException(HttpStatus.CONFLICT, text); }
    private BusinessException missing(String text) { return new BusinessException(HttpStatus.NOT_FOUND, text); }
}
