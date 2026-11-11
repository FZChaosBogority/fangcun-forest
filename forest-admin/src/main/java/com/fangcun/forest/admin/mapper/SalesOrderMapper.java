package com.fangcun.forest.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fangcun.forest.admin.entity.SalesOrder;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface SalesOrderMapper extends BaseMapper<SalesOrder> {
    @Select("SELECT * FROM sales_order WHERE id = #{id} FOR UPDATE")
    SalesOrder lockById(@Param("id") long id);
    @Select("SELECT COALESCE(SUM(total_amount), 0) FROM sales_order WHERE status IN ('PAID', 'SHIPPED', 'COMPLETED')")
    java.math.BigDecimal paidAmount();
}
