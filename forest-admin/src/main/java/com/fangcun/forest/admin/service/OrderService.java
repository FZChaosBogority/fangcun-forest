package com.fangcun.forest.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fangcun.forest.admin.aspect.AdminAction;
import com.fangcun.forest.admin.common.*;
import com.fangcun.forest.admin.entity.*;
import com.fangcun.forest.admin.enums.OrderStatus;
import com.fangcun.forest.admin.mapper.*;
import com.fangcun.forest.admin.web.OrderEditForm;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class OrderService {
    private final SalesOrderMapper orders;
    private final SalesOrderItemMapper items;
    private final StoreUserMapper users;
    private final ProductMapper products;
    public OrderService(SalesOrderMapper orders, SalesOrderItemMapper items, StoreUserMapper users, ProductMapper products) {
        this.orders = orders; this.items = items; this.users = users; this.products = products;
    }
    public Page<SalesOrder> search(String orderNo, Long userId, OrderStatus status, PageRequest page) {
        String term = QueryTerms.escape(orderNo, 40);
        return orders.selectPage(new Page<>(page.page(), page.size()), new LambdaQueryWrapper<SalesOrder>()
            .like(!term.isEmpty(), SalesOrder::getOrderNo, term).eq(userId != null, SalesOrder::getUserId, userId)
            .eq(status != null, SalesOrder::getStatus, status).orderByDesc(SalesOrder::getCreatedAt, SalesOrder::getId));
    }
    public SalesOrder get(long id) {
        SalesOrder order = orders.selectById(id); if (order == null) throw missing(); return order;
    }
    public List<SalesOrderItem> items(long id) {
        return items.selectList(new LambdaQueryWrapper<SalesOrderItem>().eq(SalesOrderItem::getOrderId, id).orderByAsc(SalesOrderItem::getProductId));
    }
    @Transactional
    @AdminAction("order.updateAddress")
    public void update(long id, OrderEditForm form) {
        SalesOrder order = lock(id, form.getVersion());
        if (order.getStatus() != OrderStatus.PENDING_PAYMENT && order.getStatus() != OrderStatus.PAID) {
            throw conflict("只能修改未发货且未取消订单的收货信息。");
        }
        if (orders.update(null, version(order).set(SalesOrder::getReceiverName, form.getReceiverName().strip())
            .set(SalesOrder::getReceiverPhone, form.getReceiverPhone().strip()).set(SalesOrder::getReceiverAddress, form.getReceiverAddress().strip())) != 1) throw conflict("订单已变化，请刷新。");
    }
    @Transactional
    @AdminAction("order.ship")
    public void ship(long id, int expected) {
        SalesOrder order = lock(id, expected);
        if (order.getStatus() != OrderStatus.PAID) throw conflict("只有已付款订单可以发货。");
        if (orders.update(null, version(order).eq(SalesOrder::getStatus, OrderStatus.PAID)
            .set(SalesOrder::getStatus, OrderStatus.SHIPPED).set(SalesOrder::getShippedAt, LocalDateTime.now())) != 1) throw conflict("订单已变化，请刷新。");
    }
    @Transactional
    @AdminAction("order.cancel")
    public void cancel(long id, int expected) {
        SalesOrder order = lock(id, expected);
        if (order.getStatus() == OrderStatus.CANCELLED) return;
        if (order.getStatus() != OrderStatus.PENDING_PAYMENT) throw conflict("只能取消待付款订单。");
        if (orders.update(null, version(order).eq(SalesOrder::getStatus, OrderStatus.PENDING_PAYMENT)
            .set(SalesOrder::getStatus, OrderStatus.CANCELLED).set(SalesOrder::getCancelledAt, LocalDateTime.now())) != 1) throw conflict("订单已变化，请刷新。");
        for (SalesOrderItem item : items(id)) {
            if (products.restore(item.getProductId(), item.getQuantity()) != 1) throw conflict("库存归还失败，取消操作已回滚。");
        }
    }
    @Transactional
    @AdminAction("order.delete")
    public void delete(long id, int expected) {
        SalesOrder order = lock(id, expected);
        if (order.getStatus() != OrderStatus.CANCELLED) throw conflict("只允许删除已取消订单，请保留已支付及履约记录。");
        items.delete(new LambdaQueryWrapper<SalesOrderItem>().eq(SalesOrderItem::getOrderId, id));
        if (orders.delete(new LambdaQueryWrapper<SalesOrder>().eq(SalesOrder::getId, id)
            .eq(SalesOrder::getVersion, order.getVersion()).eq(SalesOrder::getStatus, OrderStatus.CANCELLED)) != 1) throw conflict("订单已变化，请刷新。");
    }
    private SalesOrder lock(long id, int expected) {
        SalesOrder initial = get(id);
        // Match storefront's lock order: user -> order -> product IDs ascending.
        if (users.lockById(initial.getUserId()) == null) throw missing();
        SalesOrder order = orders.lockById(id);
        if (order == null) throw missing();
        if (order.getVersion() != expected) throw conflict("订单已在其他页面更新，请刷新后重试。");
        return order;
    }
    private LambdaUpdateWrapper<SalesOrder> version(SalesOrder order) {
        return new LambdaUpdateWrapper<SalesOrder>().eq(SalesOrder::getId, order.getId()).eq(SalesOrder::getVersion, order.getVersion())
            .set(SalesOrder::getVersion, order.getVersion() + 1).set(SalesOrder::getUpdatedAt, LocalDateTime.now());
    }
    private BusinessException missing() { return new BusinessException(HttpStatus.NOT_FOUND, "订单不存在。"); }
    private BusinessException conflict(String message) { return new BusinessException(HttpStatus.CONFLICT, message); }
}
