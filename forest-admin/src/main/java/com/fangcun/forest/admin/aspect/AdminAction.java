package com.fangcun.forest.admin.aspect;

import java.lang.annotation.*;
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface AdminAction { String value(); }
