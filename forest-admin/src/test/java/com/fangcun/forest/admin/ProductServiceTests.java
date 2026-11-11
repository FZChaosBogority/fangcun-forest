package com.fangcun.forest.admin;

import com.fangcun.forest.admin.common.BusinessException;
import com.fangcun.forest.admin.entity.*;
import com.fangcun.forest.admin.enums.ProductStatus;
import com.fangcun.forest.admin.mapper.*;
import com.fangcun.forest.admin.service.ProductService;
import com.fangcun.forest.admin.web.ProductForm;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class ProductServiceTests {
    final ProductMapper products = mock(ProductMapper.class);
    final CategoryMapper categories = mock(CategoryMapper.class);
    final ProductImageMapper images = mock(ProductImageMapper.class);
    final CartItemMapper carts = mock(CartItemMapper.class);
    final SalesOrderItemMapper items = mock(SalesOrderItemMapper.class);
    final ProductService service = new ProductService(products,categories,images,carts,items);
    Product product(ProductStatus status) { Product p = new Product(); p.setId(1L); p.setStatus(status); p.setVersion(2); p.setStock(8); p.setCategoryId(11L); return p; }
    @Test void referencedProductIsKeptInsteadOfBreakingCartReferences() {
        when(products.lockById(1)).thenReturn(product(ProductStatus.DRAFT)); when(carts.selectCount(any())).thenReturn(1L);
        assertThrows(BusinessException.class, () -> service.delete(1,2)); verify(products,never()).delete(any());
    }
    @Test void onSaleProductMustBeTakenOffSaleBeforeDeletion() {
        when(products.lockById(1)).thenReturn(product(ProductStatus.ON_SALE));
        assertThrows(BusinessException.class, () -> service.delete(1,2)); verify(products,never()).delete(any());
    }
    @Test void staleVersionDoesNotRestockOrOverwriteInventory() {
        when(products.lockById(1)).thenReturn(product(ProductStatus.ON_SALE));
        assertThrows(BusinessException.class, () -> service.restock(1,1,10)); verify(products,never()).restock(anyLong(),anyInt(),anyInt());
    }
    @Test void newlyCreatedProductIsDraftWithExplicitInitialStock() {
        Category leaf = new Category(); leaf.setId(11L); leaf.setParentId(1L); when(categories.lockById(11)).thenReturn(leaf);
        ProductForm form = new ProductForm(); form.setSku("FC-NEW"); form.setName("新补给"); form.setCategoryId(11L); form.setPrice(new BigDecimal("19.90")); form.setInitialStock(7);
        doAnswer(call -> { ((Product)call.getArgument(0)).setId(20L); return 1; }).when(products).insert(any(Product.class));
        service.create(form); ArgumentCaptor<Product> saved = ArgumentCaptor.forClass(Product.class); verify(products).insert(saved.capture());
        assertEquals(ProductStatus.DRAFT,saved.getValue().getStatus()); assertEquals(7,saved.getValue().getStock());
    }
}
