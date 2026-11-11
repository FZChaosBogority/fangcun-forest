package com.fangcun.forest.admin;

import com.fangcun.forest.admin.common.BusinessException;
import com.fangcun.forest.admin.entity.Category;
import com.fangcun.forest.admin.mapper.*;
import com.fangcun.forest.admin.service.CategoryService;
import com.fangcun.forest.admin.web.CategoryForm;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class CategoryServiceTests {
    final CategoryMapper categories = mock(CategoryMapper.class);
    final ProductMapper products = mock(ProductMapper.class);
    final CategoryService service = new CategoryService(categories, products);
    Category category(long id, Long parent) { Category c = new Category(); c.setId(id); c.setParentId(parent); c.setCode("FOREST"+id); c.setName("分类"+id); c.setSortOrder(0); return c; }
    @Test void thirdLevelCategoryIsRejected() {
        when(categories.selectById(11L)).thenReturn(category(11,1L));
        CategoryForm form = new CategoryForm(); form.setParentId(11L);
        assertThrows(BusinessException.class, () -> service.create(form)); verify(categories, never()).insert(any(Category.class));
    }
    @Test void categoryWithChildrenCannotBeDeleted() {
        Category root = category(1,null); when(categories.lockTree()).thenReturn(List.of(root,category(11,1L)));
        assertThrows(BusinessException.class, () -> service.delete(1,CategoryService.revision(root)));
        verify(categories, never()).deleteById(anyLong());
    }
    @Test void categoryReferencedByProductsCannotBeDeleted() {
        Category leaf = category(11,1L); when(categories.lockTree()).thenReturn(List.of(leaf)); when(products.selectCount(any())).thenReturn(1L);
        assertThrows(BusinessException.class, () -> service.delete(11,CategoryService.revision(leaf)));
        verify(categories, never()).deleteById(anyLong());
    }
    @Test void staleCategoryRevisionCannotOverwriteNewerFields() {
        Category root = category(1,null); when(categories.lockTree()).thenReturn(List.of(root)); CategoryForm form = new CategoryForm(); form.setRevision("old");
        assertThrows(BusinessException.class, () -> service.update(1,form)); verify(categories, never()).update(any(),any());
    }
    @Test void forgedParentOrCodeCannotMoveAnExistingCategory() {
        Category root = category(1,null); when(categories.lockTree()).thenReturn(List.of(root));
        CategoryForm form = CategoryForm.from(root,CategoryService.revision(root)); form.setParentId(9L);
        assertThrows(BusinessException.class, () -> service.update(1,form));
        verify(categories,never()).update(any(),any());
    }
}
