package com.fangcun.forest.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fangcun.forest.admin.aspect.AdminAction;
import com.fangcun.forest.admin.common.BusinessException;
import com.fangcun.forest.admin.common.Revision;
import com.fangcun.forest.admin.entity.Category;
import com.fangcun.forest.admin.entity.Product;
import com.fangcun.forest.admin.mapper.CategoryMapper;
import com.fangcun.forest.admin.mapper.ProductMapper;
import com.fangcun.forest.admin.web.CategoryForm;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class CategoryService {
    private final CategoryMapper categories;
    private final ProductMapper products;
    public CategoryService(CategoryMapper categories, ProductMapper products) { this.categories = categories; this.products = products; }
    public List<Category> all() { return categories.selectList(new LambdaQueryWrapper<Category>().orderByAsc(Category::getSortOrder, Category::getId)); }
    public Category get(long id) {
        Category category = categories.selectById(id);
        if (category == null) throw new BusinessException(HttpStatus.NOT_FOUND, "分类不存在。");
        return category;
    }
    public static String revision(Category category) {
        return Revision.of(category.getId(), category.getCode(), category.getName(), category.getParentId(), category.getSortOrder(), category.getUpdatedAt());
    }
    @Transactional
    @AdminAction("category.create")
    public long create(CategoryForm form) {
        categories.lockTree(); validateParent(null, form.getParentId());
        Category category = new Category(); category.setCode(form.getCode()); category.setName(form.getName().strip());
        category.setParentId(form.getParentId()); category.setSortOrder(form.getSortOrder());
        try { categories.insert(category); }
        catch (DuplicateKeyException exception) { throw conflict("分类编码或同级分类名称已存在。"); }
        return category.getId();
    }
    @Transactional
    @AdminAction("category.update")
    public void update(long id, CategoryForm form) {
        List<Category> tree = categories.lockTree();
        Category category = tree.stream().filter(item -> item.getId() == id).findFirst()
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "分类不存在。"));
        if (!revision(category).equals(form.getRevision())) throw conflict("分类已被修改，请刷新后重试。");
        if (!Objects.equals(category.getCode(), form.getCode()) || !Objects.equals(category.getParentId(), form.getParentId())) {
            throw conflict("分类编码和父子关系创建后不可修改；只能维护名称与排序。");
        }
        validateParent(id, form.getParentId());
        if (form.getParentId() != null && tree.stream().anyMatch(item -> Long.valueOf(id).equals(item.getParentId()))) {
            throw conflict("有子分类的一级分类不能改为二级分类。");
        }
        if (form.getParentId() == null && products.selectCount(new LambdaQueryWrapper<Product>().eq(Product::getCategoryId, id)) > 0) {
            throw conflict("已有商品的二级分类不能改为一级分类。");
        }
        try {
            if (categories.update(null, new LambdaUpdateWrapper<Category>().eq(Category::getId, id)
                    .set(Category::getName, form.getName().strip()).set(Category::getSortOrder, form.getSortOrder())
                    .set(Category::getUpdatedAt, LocalDateTime.now())) != 1) throw conflict("分类已变化，请重试。");
        } catch (DuplicateKeyException exception) { throw conflict("分类编码或同级分类名称已存在。"); }
    }
    @Transactional
    @AdminAction("category.delete")
    public void delete(long id, String revision) {
        List<Category> tree = categories.lockTree();
        Category category = tree.stream().filter(item -> item.getId() == id).findFirst()
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "分类不存在。"));
        if (!revision(category).equals(revision)) throw conflict("分类已变化，请刷新后再删除。");
        if (tree.stream().anyMatch(item -> Long.valueOf(id).equals(item.getParentId()))
                || products.selectCount(new LambdaQueryWrapper<Product>().eq(Product::getCategoryId, id)) > 0) {
            throw conflict("分类仍有子分类或商品，请先调整关联内容。");
        }
        categories.deleteById(id);
    }
    private void validateParent(Long id, Long parentId) {
        if (parentId == null) return;
        if (parentId.equals(id)) throw conflict("分类不能以自身为父分类。");
        Category parent = get(parentId);
        if (parent.getParentId() != null) throw conflict("仅支持两级分类，请选择一级分类作为父分类。");
    }
    private BusinessException conflict(String message) { return new BusinessException(HttpStatus.CONFLICT, message); }
}
