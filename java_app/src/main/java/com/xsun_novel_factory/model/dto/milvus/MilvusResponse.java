package com.xsun_novel_factory.model.dto.milvus;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * Milvus Store 通用响应对象
 */
@Data
public class MilvusResponse {
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
     * 文档类型
     */
    @JsonProperty("docType")
    @JsonAlias("doc_type")
    private String docType;
    
    /**
     * 插入的块数
     */
    @JsonProperty("chunksInserted")
    @JsonAlias("chunks_inserted")
    private Integer chunksInserted;
    
    /**
     * 健康检查状态
     */
    private Boolean ok;
    
    /**
     * Milvus URI
     */
    @JsonProperty("milvusUri")
    @JsonAlias("milvus_uri")
    private String milvusUri;
    
    /**
     * 集合名称
     */
    private String collection;
    
    /**
     * 维度
     */
    private Integer dim;
    
    /**
     * 嵌入提供者
     */
    @JsonProperty("embeddingProvider")
    @JsonAlias("embedding_provider")
    private String embeddingProvider;
    
    /**
     * 检索结果映射
     */
    private Map<String, List<Answer>> results;
}