package com.xsun_novel_factory.model.dto.milvus;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.Map;

/**
 * 检索答案对象
 */
@Data
public class Answer {
    /**
     * 文本内容
     */
    private String text;
    
    /**
     * 相似度分数
     */
    private Double score;
    
    /**
     * 元数据
     */
    private Map<String, Object> meta;
}