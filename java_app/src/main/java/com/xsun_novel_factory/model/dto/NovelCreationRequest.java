package com.xsun_novel_factory.model.dto;

import lombok.Data;

/**
 * 小说创建请求对象
 */
@Data
public class NovelCreationRequest {
    /**
     * 小说类型（如玄幻、都市、科幻等）
     */
    private String novelType;
    
    /**
     * 参考小说ID（用于Milvus检索）
     */
    private String referenceNovelId;
    
    /**
     * 参考小说风格
     */
    private String referenceNovelStyle;
}