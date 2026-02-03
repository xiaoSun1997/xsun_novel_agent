package com.xsun_novel_factory.model.dto;

import lombok.*;

/**
 * 章节发布结果传输对象
 * 包含发布章节的ID、版本号和关联的出箱事件ID
 */
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class PublishResult {
    /** 章节ID */
    private Long chapterId;
    /** 章节版本号 */
    private Integer chapterVersion;
    /** 出箱事件ID，用于图谱更新 */
    private Long outboxEventId;
}