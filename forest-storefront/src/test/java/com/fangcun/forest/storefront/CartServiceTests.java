package com.fangcun.forest.storefront;

import com.fangcun.forest.storefront.common.BusinessException;
import com.fangcun.forest.storefront.entity.CartItem;
import com.fangcun.forest.storefront.entity.Product;
import com.fangcun.forest.storefront.enums.ProductStatus;
import com.fangcun.forest.storefront.mapper.CartItemMapper;
import com.fangcun.forest.storefront.mapper.ProductMapper;
import com.fangcun.forest.storefront.service.CartService;
import com.fangcun.forest.storefront.service.CustomerLock;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CartServiceTests {
    private final CartItemMapper carts = mock(CartItemMapper.class);
    private final ProductMapper products = mock(ProductMapper.class);
    private final CustomerLock lock = mock(CustomerLock.class);
    private final CartService service = new CartService(carts, products, lock);
    @Test
    void addingExistingQuantitiesStillRespectsStock() {
        Product product = new Product(); product.setId(1L); product.setStatus(ProductStatus.ON_SALE); product.setStock(5);
        CartItem item = new CartItem(); item.setQuantity(4);
        when(products.lockById(1L)).thenReturn(product); when(carts.selectOne(any())).thenReturn(item);
        assertThrows(BusinessException.class, () -> service.add(7, 1, 2));
        verify(carts, never()).insert(any(CartItem.class)); verify(carts, never()).update(any(), any());
    }
    @Test
    void staleCartVersionIsRejectedBeforeUpdating() {
        CartItem item = new CartItem(); item.setVersion(2); when(carts.selectOne(any())).thenReturn(item);
        assertThrows(BusinessException.class, () -> service.update(7, 10, 1, 1));
        verify(carts, never()).update(any(), any());
    }
    @Test
    void missingOrForeignCartItemCannotBeRemoved() {
        when(carts.selectOne(any())).thenReturn(null);
        assertThrows(BusinessException.class, () -> service.remove(7, 999, 0));
        verify(carts, never()).delete(any());
    }
    @Test
    void invalidQuantityDoesNotTouchTheDatabase() {
        assertThrows(BusinessException.class, () -> service.add(7, 1, 100));
        verifyNoInteractions(carts, products, lock);
    }
}
