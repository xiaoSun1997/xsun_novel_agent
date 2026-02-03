package com.xsun_novel_factory.model.dto.graph;

import lombok.Data;

/**
 * 逻辑检查请求对象
 */
@Data
public class CheckLogicReq {
    /**
     * 小说ID
     */
    private String novelId;
    
    /**
     * 当前章节号
     */
    private Integer chapter;
    
    /**
     * 待检查的正文内容
     */
    private String content;
}