package com.fangcun.forest.storefront.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fangcun.forest.storefront.common.BusinessException;
import com.fangcun.forest.storefront.entity.CartItem;
import com.fangcun.forest.storefront.entity.Product;
import com.fangcun.forest.storefront.enums.ProductStatus;
import com.fangcun.forest.storefront.mapper.CartItemMapper;
import com.fangcun.forest.storefront.mapper.ProductMapper;
import com.fangcun.forest.storefront.web.CartView;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class CartService {
    public static final int MAX_KINDS = 20;
    private final CartItemMapper carts;
    private final ProductMapper products;
    private final CustomerLock customers;
    public CartService(CartItemMapper carts, ProductMapper products, CustomerLock customers) {
        this.carts = carts; this.products = products; this.customers = customers;
    }

    public CartView view(long userId) {
        List<CartItem> items = carts.selectList(new LambdaQueryWrapper<CartItem>()
                .eq(CartItem::getUserId, userId).orderByDesc(CartItem::getId));
        if (items.isEmpty()) return new CartView(List.of(), BigDecimal.ZERO, 0);
        Map<Long, Product> byId = products.selectByIds(items.stream().map(CartItem::getProductId).toList())
                .stream().collect(Collectors.toMap(Product::getId, Function.identity()));
        List<CartView.Line> lines = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        int quantity = 0;
        for (CartItem item : items) {
            Product product = byId.get(item.getProductId());
            if (product == null) throw new BusinessException(HttpStatus.CONFLICT, "购物车中的商品资料缺失，请联系管理员。");
            String issue = product.getStatus() != ProductStatus.ON_SALE ? "商品已下架"
                    : product.getStock() < item.getQuantity() ? "库存不足，请调整数量" : "";
            CartView.Line line = new CartView.Line(item, product, issue.isEmpty(), issue);
            lines.add(line);
            if (line.available()) total = total.add(line.getAmount());
            quantity += item.getQuantity();
        }
        return new CartView(List.copyOf(lines), total, quantity);
    }

    public int quantity(long userId) {
        return carts.selectList(new LambdaQueryWrapper<CartItem>().eq(CartItem::getUserId, userId))
                .stream().mapToInt(CartItem::getQuantity).sum();
    }

    @Transactional
    public void add(long userId, long productId, int quantity) {
        validateQuantity(quantity);
        customers.acquire(userId);
        Product product = onSale(productId);
        CartItem existing = carts.selectOne(new LambdaQueryWrapper<CartItem>()
                .eq(CartItem::getUserId, userId).eq(CartItem::getProductId, productId));
        int total = quantity + (existing == null ? 0 : existing.getQuantity());
        validateQuantity(total);
        if (total > product.getStock()) throw conflict("库存不足，购物车数量不能超过当前库存。");
        if (existing != null) {
            change(existing, total);
        } else {
            if (carts.selectCount(new LambdaQueryWrapper<CartItem>().eq(CartItem::getUserId, userId)) >= MAX_KINDS) {
                throw conflict("购物车最多放入 20 种商品，请先结算或移除一些商品。");
            }
            CartItem item = new CartItem();
            item.setUserId(userId); item.setProductId(productId); item.setQuantity(quantity); item.setVersion(0);
            carts.insert(item);
        }
    }

    @Transactional
    public void update(long userId, long itemId, int quantity, int version) {
        validateQuantity(quantity);
        customers.acquire(userId);
        CartItem item = owned(userId, itemId);
        if (item.getVersion() != version) throw conflict("购物车已在其他页面更新，请刷新后重试。");
        if (onSale(item.getProductId()).getStock() < quantity) throw conflict("库存不足，请减少数量。");
        change(item, quantity);
    }

    @Transactional
    public void remove(long userId, long itemId, int version) {
        customers.acquire(userId);
        CartItem item = owned(userId, itemId);
        if (item.getVersion() != version) throw conflict("购物车已变化，请刷新后再移除。");
        int deleted = carts.delete(new LambdaQueryWrapper<CartItem>().eq(CartItem::getId, itemId)
                .eq(CartItem::getUserId, userId).eq(CartItem::getVersion, version));
        if (deleted != 1) throw conflict("购物车已变化，请刷新后重试。");
    }

    private CartItem owned(long userId, long id) {
        CartItem item = carts.selectOne(new LambdaQueryWrapper<CartItem>()
                .eq(CartItem::getId, id).eq(CartItem::getUserId, userId));
        if (item == null) throw new BusinessException(HttpStatus.NOT_FOUND, "购物车条目不存在。");
        return item;
    }

    private Product onSale(long id) {
        Product product = products.lockById(id);
        if (product == null || product.getStatus() != ProductStatus.ON_SALE) throw conflict("商品不存在或已下架。");
        return product;
    }

    private void change(CartItem item, int quantity) {
        int updated = carts.update(null, new LambdaUpdateWrapper<CartItem>()
                .eq(CartItem::getId, item.getId()).eq(CartItem::getUserId, item.getUserId())
                .eq(CartItem::getVersion, item.getVersion()).set(CartItem::getQuantity, quantity)
                .set(CartItem::getVersion, item.getVersion() + 1).set(CartItem::getUpdatedAt, LocalDateTime.now()));
        if (updated != 1) throw conflict("购物车已变化，请刷新后重试。");
    }

    private void validateQuantity(int quantity) {
        if (quantity < 1 || quantity > 99) throw new BusinessException(HttpStatus.BAD_REQUEST, "每种商品数量须为 1—99。");
    }
    private BusinessException conflict(String message) { return new BusinessException(HttpStatus.CONFLICT, message); }
}
