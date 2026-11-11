package com.fangcun.forest.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fangcun.forest.admin.entity.Product;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

public interface ProductMapper extends BaseMapper<Product> {
    @Select("SELECT * FROM product WHERE id = #{id} FOR UPDATE")
    Product lockById(@Param("id") long id);
    @Update("UPDATE product SET stock = stock + #{quantity}, version = version + 1, updated_at = CURRENT_TIMESTAMP "
            + "WHERE id = #{id} AND version = #{version} AND stock <= 2147483647 - #{quantity}")
    int restock(@Param("id") long id, @Param("quantity") int quantity, @Param("version") int version);
    @Update("UPDATE product SET stock = stock + #{quantity}, version = version + 1, updated_at = CURRENT_TIMESTAMP "
            + "WHERE id = #{id} AND stock <= 2147483647 - #{quantity}")
    int restore(@Param("id") long id, @Param("quantity") int quantity);
}
