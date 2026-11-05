package com.baseshop.common.excel;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ExcelColumn {

    String header(); // 헤더이름
    int order(); // 컬럼 순서
    
    int width() default 15; // 컬럼 너비

}
