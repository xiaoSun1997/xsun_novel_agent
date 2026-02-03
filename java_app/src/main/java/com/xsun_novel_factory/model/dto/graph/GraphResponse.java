package com.xsun_novel_factory.model.dto.graph;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 知识图谱通用响应对象
 */
@Data
public class GraphResponse {
    /**
     * 请求是否成功
     */
    private Boolean success;
    
    /**
     * 小说ID
     */
    @JsonProperty("novelId")
    @JsonAlias("novel_id")
    private String novelId;
    
    /**
     * 章节号
     */
    private Integer chapter;
    
    /**
     * 提取的实体统计信息
     */
    private ExtractedInfo extracted;
    
    /**
     * 详细数据
     */
    private Object details;
    
    /**
     * 查询类型
     */
    @JsonProperty("queryType")
    @JsonAlias("query_type")
    private String queryType;
    
    /**
     * 查询结果
     */
    private List<Object> results;
    
    /**
     * 是否存在错误
     */
    @JsonProperty("hasErrors")
    @JsonAlias("has_errors")
    private Boolean hasErrors;
    
    /**
     * 错误数量
     */
    @JsonProperty("errorsCount")
    @JsonAlias("errors_count")
    private Integer errorsCount;
    
    /**
     * 错误详情
     */
    private List<String> errors;
    
    @Data
    public static class ExtractedInfo {
        @JsonProperty("charactersCount")
        @JsonAlias("characters_count")
        private Integer charactersCount;
        
        @JsonProperty("locationsCount")
        @JsonAlias("locations_count")
        private Integer locationsCount;
        
        @JsonProperty("organizationsCount")
        @JsonAlias("organizations_count")
        private Integer organizationsCount;
        
        @JsonProperty("eventsCount")
        @JsonAlias("events_count")
        private Integer eventsCount;
        
        @JsonProperty("relationsCount")
        @JsonAlias("relations_count")
        private Integer relationsCount;
    }
}