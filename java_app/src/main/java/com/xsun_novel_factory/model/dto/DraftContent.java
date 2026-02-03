package com.xsun_novel_factory.model.dto;

import lombok.*;

/**
 * 章节草稿内容传输对象
 * 包含章节标题、正文内容和元数据
 */
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class DraftContent {
    /** 章节标题 */
    private String title;
    /** 章节正文内容 */
    private String content;
    /** JSON 字符串，方便直接存 DB（draft_meta / meta） */
    private String metaJson;
    /** 追踪ID，用于请求追踪 */
    private String traceId;
}