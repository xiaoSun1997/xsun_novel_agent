package com.xsun_novel_factory.model.dto.graph;

import lombok.Data;

/**
 * 知识图谱提取请求对象
 */
@Data
public class ExtractGraphReq {
    /**
     * 小说ID
     */
    private String novelId;
    
    /**
     * 章节号
     */
    private Integer chapter;
    
    /**
     * 章节正文内容
     */
    private String content;
}