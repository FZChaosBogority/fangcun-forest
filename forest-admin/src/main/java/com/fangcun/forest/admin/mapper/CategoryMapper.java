package com.fangcun.forest.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fangcun.forest.admin.entity.Category;
import java.util.List;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface CategoryMapper extends BaseMapper<Category> {
    @Select("SELECT * FROM category ORDER BY id FOR UPDATE")
    List<Category> lockTree();
    @Select("SELECT * FROM category WHERE id = #{id} FOR UPDATE")
    Category lockById(@Param("id") long id);
}
