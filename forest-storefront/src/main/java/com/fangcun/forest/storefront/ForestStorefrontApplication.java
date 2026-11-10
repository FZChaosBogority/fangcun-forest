package com.fangcun.forest.storefront;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.fangcun.forest.storefront.mapper")
public class ForestStorefrontApplication {

    public static void main(String[] args) {
        SpringApplication.run(ForestStorefrontApplication.class, args);
    }
}
