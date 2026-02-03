package com.xsun_novel_factory.model.dto.milvus;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * 检索过滤器
 */
@Data
public class RetrieveFilters {
    /**
     * 作品ID
     */
    @JsonProperty("workId")
    @JsonAlias("work_id")
    private String workId;
    
    /**
     * 作品标题
     */
    @JsonProperty("workTitle")
    @JsonAlias("work_title")
    private String workTitle;
    
    /**
     * 文档类型列表
     */
    @JsonProperty("docTypes")
    @JsonAlias("doc_types")
    private List<String> docTypes;
    
    /**
     * 标签列表
     */
    private List<String> tags;
    
    /**
     * 章节
     */
    private String chapter;
}