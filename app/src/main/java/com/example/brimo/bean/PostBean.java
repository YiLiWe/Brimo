package com.example.brimo.bean;

import java.util.List;

import lombok.Data;

@Data
public class PostBean {
    private List<?> data;//收款数据集
    private long timestamp;//时间戳
    private String sign;//密匙验证
}
