package com.fangcun.forest.admin;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.fangcun.forest.admin.mapper")
public class ForestAdminApplication {

    public static void main(String[] args) {
        SpringApplication.run(ForestAdminApplication.class, args);
    }
}
