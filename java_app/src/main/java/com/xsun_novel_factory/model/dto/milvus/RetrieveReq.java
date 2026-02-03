package com.xsun_novel_factory.model.dto.milvus;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * 检索请求对象
 */
@Data
public class RetrieveReq {
    /**
     * 问题列表
     */
    private List<String> questions;
    
    /**
     * 过滤条件
     */
    private RetrieveFilters filter = new RetrieveFilters();
    
    /**
     * 返回结果数量，默认为8，范围1-50
     */
    @JsonProperty("topK")
    @JsonAlias("top_k")
    private Integer topK = 8;
}