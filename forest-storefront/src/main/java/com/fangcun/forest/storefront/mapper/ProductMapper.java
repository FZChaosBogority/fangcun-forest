package com.fangcun.forest.storefront.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fangcun.forest.storefront.entity.Product;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

public interface ProductMapper extends BaseMapper<Product> {
    @Select("SELECT * FROM product WHERE id = #{id} FOR UPDATE")
    Product lockById(@Param("id") long id);

    @Update("UPDATE product SET stock = stock - #{quantity}, version = version + 1, "
            + "updated_at = CURRENT_TIMESTAMP WHERE id = #{id} AND status = 'ON_SALE' "
            + "AND stock >= #{quantity} AND version = #{version}")
    int reserve(@Param("id") long id, @Param("quantity") int quantity, @Param("version") int version);

    @Update("UPDATE product SET stock = stock + #{quantity}, version = version + 1, "
            + "updated_at = CURRENT_TIMESTAMP WHERE id = #{id} AND stock <= 2147483647 - #{quantity}")
    int restore(@Param("id") long id, @Param("quantity") int quantity);
}
