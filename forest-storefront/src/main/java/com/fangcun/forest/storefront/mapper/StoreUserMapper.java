package com.fangcun.forest.storefront.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fangcun.forest.storefront.entity.StoreUser;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface StoreUserMapper extends BaseMapper<StoreUser> {
    @Select("SELECT * FROM store_user WHERE id = #{id} FOR UPDATE")
    StoreUser lockById(@Param("id") long id);
}
