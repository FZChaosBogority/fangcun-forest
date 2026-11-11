package com.fangcun.forest.admin;

import com.fangcun.forest.admin.common.BusinessException;
import com.fangcun.forest.admin.entity.*;
import com.fangcun.forest.admin.enums.OrderStatus;
import com.fangcun.forest.admin.mapper.*;
import com.fangcun.forest.admin.service.OrderService;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

/** Mapper mocks do not prove real database rollback or cross-process locking. */
class OrderServiceTests {
    final SalesOrderMapper orders=mock(SalesOrderMapper.class);
    final SalesOrderItemMapper items=mock(SalesOrderItemMapper.class);
    final StoreUserMapper users=mock(StoreUserMapper.class);
    final ProductMapper products=mock(ProductMapper.class);
    final OrderService service=new OrderService(orders,items,users,products);
    void order(OrderStatus status) {
        SalesOrder order=new SalesOrder(); order.setId(1L); order.setUserId(7L); order.setVersion(2); order.setStatus(status);
        when(orders.selectById(1L)).thenReturn(order); when(orders.lockById(1)).thenReturn(order);
        StoreUser user=new StoreUser(); user.setId(7L); when(users.lockById(7)).thenReturn(user);
    }
    @Test void onlyPaidOrdersCanShip() {
        order(OrderStatus.PENDING_PAYMENT); assertThrows(BusinessException.class,()->service.ship(1,2)); verify(orders,never()).update(any(),any());
    }
    @Test void shippingDoesNotDeductStockAgain() {
        order(OrderStatus.PAID); when(orders.update(any(),any())).thenReturn(1); service.ship(1,2); verifyNoInteractions(products);
    }
    @Test void paidOrdersCannotBeDeleted() {
        order(OrderStatus.PAID); assertThrows(BusinessException.class,()->service.delete(1,2)); verify(items,never()).delete(any()); verify(orders,never()).delete(any());
    }
    @Test void staleVersionCannotShipAnOrder() {
        order(OrderStatus.PAID); assertThrows(BusinessException.class,()->service.ship(1,1)); verify(orders,never()).update(any(),any());
    }
    @Test void cancellingAnAlreadyCancelledOrderNeverRestoresTwice() {
        order(OrderStatus.CANCELLED); service.cancel(1,2); verifyNoInteractions(products,items);
    }
}
