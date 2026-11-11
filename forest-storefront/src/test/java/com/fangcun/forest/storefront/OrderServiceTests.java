package com.fangcun.forest.storefront;

import com.fangcun.forest.storefront.common.BusinessException;
import com.fangcun.forest.storefront.entity.*;
import com.fangcun.forest.storefront.enums.OrderStatus;
import com.fangcun.forest.storefront.enums.ProductStatus;
import com.fangcun.forest.storefront.mapper.*;
import com.fangcun.forest.storefront.service.*;
import com.fangcun.forest.storefront.web.CheckoutDraft;
import com.fangcun.forest.storefront.web.ShippingForm;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Mapper mocks check business decisions; actual rollback/locking must be checked on MySQL. */
class OrderServiceTests {
    private final SalesOrderMapper orders = mock(SalesOrderMapper.class);
    private final SalesOrderItemMapper items = mock(SalesOrderItemMapper.class);
    private final CartItemMapper carts = mock(CartItemMapper.class);
    private final ProductMapper products = mock(ProductMapper.class);
    private final OrderService service = new OrderService(orders, items, carts, products,
            mock(CartService.class), mock(CustomerLock.class));
    private CheckoutDraft draft() {
        return new CheckoutDraft(7, "FCtest", Instant.now(), List.of(new CheckoutDraft.Line(10, 1, 2, 0,
                new BigDecimal("89.00"), "旧名称", "/old.svg")));
    }
    private ShippingForm shipping() {
        ShippingForm form = new ShippingForm(); form.setReceiverName("旅人"); form.setReceiverPhone("13800000000");
        form.setReceiverAddress("测试城市森林路 1 号"); return form;
    }
    private void productAndCart(String price) {
        CartItem item = new CartItem(); item.setId(10L); item.setProductId(1L); item.setQuantity(2); item.setVersion(0);
        when(carts.selectOne(any())).thenReturn(item);
        Product product = new Product(); product.setId(1L); product.setName("当前名称"); product.setSku("FC-1");
        product.setCoverImage("/now.svg"); product.setPrice(new BigDecimal(price)); product.setStock(8);
        product.setStatus(ProductStatus.ON_SALE); product.setVersion(4); when(products.lockById(1)).thenReturn(product);
    }
    @Test
    void repeatedCheckoutReturnsTheSameOrderWithoutReservingStockAgain() {
        SalesOrder existing = new SalesOrder(); existing.setId(8L); when(orders.selectOne(any())).thenReturn(existing);
        assertSame(existing, service.place(7, draft(), shipping()));
        verifyNoInteractions(products, carts, items);
    }
    @Test
    void priceChangeRequiresTheCustomerToConfirmAgain() {
        productAndCart("99.00");
        assertThrows(BusinessException.class, () -> service.place(7, draft(), shipping()));
        verify(products, never()).reserve(anyLong(), anyInt(), anyInt()); verify(orders, never()).insert(any(SalesOrder.class));
    }
    @Test
    void stockFailureDoesNotWriteAnOrderOrRemoveCartItems() {
        productAndCart("89.00"); when(products.reserve(1, 2, 4)).thenReturn(0);
        assertThrows(BusinessException.class, () -> service.place(7, draft(), shipping()));
        verify(orders, never()).insert(any(SalesOrder.class)); verify(carts, never()).delete(any());
    }
    @Test
    void orderUsesCurrentServerProductSnapshotsAndExactDecimalTotals() {
        productAndCart("89.00"); when(products.reserve(1, 2, 4)).thenReturn(1);
        when(carts.delete(any())).thenReturn(1);
        doAnswer(call -> { ((SalesOrder) call.getArgument(0)).setId(100L); return 1; }).when(orders).insert(any(SalesOrder.class));
        SalesOrder order = service.place(7, draft(), shipping());
        assertEquals(new BigDecimal("178.00"), order.getTotalAmount());
        var saved = ArgumentCaptor.forClass(SalesOrderItem.class); verify(items).insert(saved.capture());
        assertEquals("当前名称", saved.getValue().getProductNameSnapshot());
        assertEquals("/now.svg", saved.getValue().getCoverImageSnapshot());
        assertEquals(100L, saved.getValue().getOrderId());
    }
    @Test
    void paidOrderCannotBeCancelledAndRepeatedPayDoesNotWriteAgain() {
        SalesOrder order = new SalesOrder(); order.setStatus(OrderStatus.PAID); when(orders.selectOne(any())).thenReturn(order);
        service.pay(7, 8);
        assertThrows(BusinessException.class, () -> service.cancel(7, 8));
        verify(orders, never()).update(any(), any()); verifyNoInteractions(products);
    }
    @Test
    void repeatedCancelCannotRestoreStockTwice() {
        SalesOrder order = new SalesOrder(); order.setStatus(OrderStatus.CANCELLED); when(orders.selectOne(any())).thenReturn(order);
        service.cancel(7, 8); verifyNoInteractions(products, items);
    }
    @Test
    void cancellingAPendingOrderRestoresEachSnapshotQuantity() {
        SalesOrder order = new SalesOrder(); order.setId(8L); order.setUserId(7L);
        order.setVersion(0); order.setStatus(OrderStatus.PENDING_PAYMENT);
        when(orders.selectOne(any())).thenReturn(order); when(orders.update(any(), any())).thenReturn(1);
        SalesOrderItem item = new SalesOrderItem(); item.setProductId(1L); item.setQuantity(2);
        when(items.selectList(any())).thenReturn(List.of(item)); when(products.restore(1, 2)).thenReturn(1);
        service.cancel(7, 8); verify(products).restore(1, 2);
        verify(products, never()).reserve(anyLong(), anyInt(), anyInt());
    }
    @Test
    void aLostOrderStateRaceDoesNotRestoreInventory() {
        SalesOrder order = new SalesOrder(); order.setId(8L); order.setUserId(7L);
        order.setVersion(0); order.setStatus(OrderStatus.PENDING_PAYMENT); when(orders.selectOne(any())).thenReturn(order);
        when(orders.update(any(), any())).thenReturn(0);
        assertThrows(BusinessException.class, () -> service.cancel(7, 8));
        verifyNoInteractions(products, items);
    }
    @Test
    void missingOrForeignOrderIsNotReturned() {
        assertThrows(BusinessException.class, () -> service.owned(7, 99));
        verifyNoInteractions(items);
    }
}
