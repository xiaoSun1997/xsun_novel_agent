package com.xsun_novel_factory.model.dto.graph;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * 知识图谱查询请求对象
 */
@Data
public class QueryGraphReq {
    /**
     * 小说ID
     */
    @JsonProperty("novelId")
    @JsonAlias("novel_id")
    private String novelId;
    
    /**
     * 查询类型: character/location/organization/timeline
     */
    @JsonProperty("queryType")
    @JsonAlias("query_type")
    private String queryType;
    
    /**
     * 实体名称（可选，不填返回全部）
     */
    @JsonProperty("entityName")
    @JsonAlias("entity_name")
    private String entityName;
    
    /**
     * 限制章节范围（可选）
     */
    private Integer chapter;
}