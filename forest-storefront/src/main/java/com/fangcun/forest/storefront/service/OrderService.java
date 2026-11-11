package com.fangcun.forest.storefront.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fangcun.forest.storefront.common.BusinessException;
import com.fangcun.forest.storefront.common.PageRequest;
import com.fangcun.forest.storefront.entity.*;
import com.fangcun.forest.storefront.enums.OrderStatus;
import com.fangcun.forest.storefront.enums.ProductStatus;
import com.fangcun.forest.storefront.mapper.*;
import com.fangcun.forest.storefront.web.CartView;
import com.fangcun.forest.storefront.web.CheckoutDraft;
import com.fangcun.forest.storefront.web.ShippingForm;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class OrderService {
    private final SalesOrderMapper orders;
    private final SalesOrderItemMapper orderItems;
    private final CartItemMapper carts;
    private final ProductMapper products;
    private final CartService cartService;
    private final CustomerLock customers;
    public OrderService(SalesOrderMapper orders, SalesOrderItemMapper orderItems,
            CartItemMapper carts, ProductMapper products, CartService cartService, CustomerLock customers) {
        this.orders = orders; this.orderItems = orderItems; this.carts = carts;
        this.products = products; this.cartService = cartService; this.customers = customers;
    }

    public CheckoutDraft prepare(long userId, List<Long> selected) {
        if (selected == null || selected.isEmpty() || selected.size() > CartService.MAX_KINDS
                || new HashSet<>(selected).size() != selected.size()) throw conflict("请选择要结算的商品。");
        CartView cart = cartService.view(userId);
        List<CartView.Line> lines = cart.lines().stream().filter(line -> selected.contains(line.item().getId())).toList();
        if (lines.size() != selected.size()) throw conflict("所选购物车条目已变化，请重新选择。");
        if (lines.stream().anyMatch(line -> !line.available())) throw conflict("所选商品已下架或库存不足，请调整购物车。");
        return new CheckoutDraft(userId, "FC" + UUID.randomUUID().toString().replace("-", ""), Instant.now(),
                lines.stream().sorted(Comparator.comparing(line -> line.product().getId()))
                    .map(line -> new CheckoutDraft.Line(line.item().getId(), line.product().getId(),
                            line.item().getQuantity(), line.item().getVersion(), line.product().getPrice(),
                            line.product().getName(), line.product().getCoverImage())).toList());
    }

    @Transactional
    public SalesOrder place(long userId, CheckoutDraft draft, ShippingForm shipping) {
        if (draft.userId() != userId || draft.lines().isEmpty()) throw conflict("结算信息无效。");
        customers.acquire(userId);
        // A repeated POST for the same server-issued draft returns the first order.
        SalesOrder existing = orders.selectOne(new LambdaQueryWrapper<SalesOrder>()
                .eq(SalesOrder::getOrderNo, draft.orderNo()).eq(SalesOrder::getUserId, userId));
        if (existing != null) return existing;
        List<SalesOrderItem> snapshots = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        for (CheckoutDraft.Line line : draft.lines().stream().sorted(Comparator.comparingLong(CheckoutDraft.Line::productId)).toList()) {
            CartItem cart = carts.selectOne(new LambdaQueryWrapper<CartItem>()
                    .eq(CartItem::getId, line.cartId()).eq(CartItem::getUserId, userId));
            if (cart == null || cart.getProductId() != line.productId() || cart.getQuantity() != line.quantity()
                    || cart.getVersion() != line.cartVersion()) throw conflict("购物车已变化，请返回购物车重新结算。");
            Product product = products.lockById(line.productId());
            if (product == null || product.getStatus() != ProductStatus.ON_SALE) throw conflict("部分商品已下架，请重新结算。");
            if (product.getPrice().compareTo(line.unitPrice()) != 0) throw conflict("商品价格已变化，请返回购物车确认新价格。");
            if (products.reserve(product.getId(), line.quantity(), product.getVersion()) != 1) {
                throw conflict("“" + product.getName() + "”库存不足，请重新结算。");
            }
            SalesOrderItem item = new SalesOrderItem();
            item.setProductId(product.getId()); item.setSkuSnapshot(product.getSku());
            item.setProductNameSnapshot(product.getName()); item.setCoverImageSnapshot(product.getCoverImage());
            item.setUnitPrice(product.getPrice()); item.setQuantity(line.quantity());
            item.setLineAmount(product.getPrice().multiply(BigDecimal.valueOf(line.quantity())));
            total = total.add(item.getLineAmount()); snapshots.add(item);
        }
        if (total.signum() <= 0 || total.compareTo(new BigDecimal("9999999999.99")) > 0) throw conflict("订单金额超出允许范围。");
        SalesOrder order = new SalesOrder();
        order.setOrderNo(draft.orderNo()); order.setUserId(userId); order.setStatus(OrderStatus.PENDING_PAYMENT);
        order.setTotalAmount(total); order.setVersion(0);
        order.setReceiverName(shipping.getReceiverName().strip()); order.setReceiverPhone(shipping.getReceiverPhone().strip());
        order.setReceiverAddress(shipping.getReceiverAddress().strip());
        orders.insert(order);
        for (SalesOrderItem item : snapshots) { item.setOrderId(order.getId()); orderItems.insert(item); }
        for (CheckoutDraft.Line line : draft.lines()) {
            if (carts.delete(new LambdaQueryWrapper<CartItem>().eq(CartItem::getId, line.cartId())
                    .eq(CartItem::getUserId, userId).eq(CartItem::getVersion, line.cartVersion())) != 1) {
                throw conflict("购物车已变化，订单未提交，请重试。");
            }
        }
        return order;
    }

    public Page<SalesOrder> list(long userId, OrderStatus status, PageRequest page) {
        return orders.selectPage(new Page<>(page.page(), page.size()), new LambdaQueryWrapper<SalesOrder>()
                .eq(SalesOrder::getUserId, userId).eq(status != null, SalesOrder::getStatus, status)
                .orderByDesc(SalesOrder::getCreatedAt, SalesOrder::getId));
    }

    public SalesOrder owned(long userId, long id) {
        SalesOrder order = orders.selectOne(new LambdaQueryWrapper<SalesOrder>()
                .eq(SalesOrder::getId, id).eq(SalesOrder::getUserId, userId));
        if (order == null) throw new BusinessException(HttpStatus.NOT_FOUND, "订单不存在。");
        return order;
    }
    public List<SalesOrderItem> items(long userId, long id) {
        owned(userId, id);
        return orderItems.selectList(new LambdaQueryWrapper<SalesOrderItem>()
                .eq(SalesOrderItem::getOrderId, id).orderByAsc(SalesOrderItem::getProductId));
    }

    @Transactional
    public void pay(long userId, long id) {
        customers.acquire(userId);
        SalesOrder order = owned(userId, id);
        if (order.getStatus() == OrderStatus.PAID || order.getStatus() == OrderStatus.SHIPPED
                || order.getStatus() == OrderStatus.COMPLETED) return;
        if (order.getStatus() != OrderStatus.PENDING_PAYMENT) throw conflict("当前订单不能支付。");
        int updated = orders.update(null, pending(order)
                .set(SalesOrder::getStatus, OrderStatus.PAID).set(SalesOrder::getPaymentMethod, "MOCK")
                .set(SalesOrder::getPaymentNo, "MOCK" + UUID.randomUUID().toString().replace("-", ""))
                .set(SalesOrder::getPaidAt, LocalDateTime.now()));
        if (updated != 1) throw conflict("订单状态已变化，请刷新查看。");
    }

    @Transactional
    public void cancel(long userId, long id) {
        customers.acquire(userId);
        SalesOrder order = owned(userId, id);
        if (order.getStatus() == OrderStatus.CANCELLED) return;
        if (order.getStatus() != OrderStatus.PENDING_PAYMENT) throw conflict("只能取消待付款订单。");
        if (orders.update(null, pending(order).set(SalesOrder::getStatus, OrderStatus.CANCELLED)
                .set(SalesOrder::getCancelledAt, LocalDateTime.now())) != 1) throw conflict("订单状态已变化，请刷新查看。");
        for (SalesOrderItem item : items(userId, id)) {
            if (products.restore(item.getProductId(), item.getQuantity()) != 1) throw conflict("库存恢复失败，取消操作已回滚。");
        }
    }

    @Transactional
    public void complete(long userId, long id) {
        customers.acquire(userId);
        SalesOrder order = owned(userId, id);
        if (order.getStatus() == OrderStatus.COMPLETED) return;
        if (order.getStatus() != OrderStatus.SHIPPED) throw conflict("发货后才可以确认收货。");
        if (orders.update(null, new LambdaUpdateWrapper<SalesOrder>()
                .eq(SalesOrder::getId, id).eq(SalesOrder::getUserId, userId).eq(SalesOrder::getStatus, OrderStatus.SHIPPED)
                .eq(SalesOrder::getVersion, order.getVersion()).set(SalesOrder::getStatus, OrderStatus.COMPLETED)
                .set(SalesOrder::getCompletedAt, LocalDateTime.now()).set(SalesOrder::getVersion, order.getVersion() + 1)
                .set(SalesOrder::getUpdatedAt, LocalDateTime.now())) != 1) throw conflict("订单状态已变化，请刷新查看。");
    }

    private LambdaUpdateWrapper<SalesOrder> pending(SalesOrder order) {
        return new LambdaUpdateWrapper<SalesOrder>().eq(SalesOrder::getId, order.getId())
                .eq(SalesOrder::getUserId, order.getUserId()).eq(SalesOrder::getStatus, OrderStatus.PENDING_PAYMENT)
                .eq(SalesOrder::getVersion, order.getVersion()).set(SalesOrder::getVersion, order.getVersion() + 1)
                .set(SalesOrder::getUpdatedAt, LocalDateTime.now());
    }
    private BusinessException conflict(String message) { return new BusinessException(HttpStatus.CONFLICT, message); }
}
