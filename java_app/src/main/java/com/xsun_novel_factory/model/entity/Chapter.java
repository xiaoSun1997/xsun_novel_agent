package com.xsun_novel_factory.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;
import java.util.Date;
import lombok.Data;

/**
 * 正式章节表（仅存审查通过内容）
 * @TableName chapter
 */
@TableName(value ="chapter")
@Data
public class Chapter {
    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 所属小说ID
     */
    private Long novelId;

    /**
     * 章节序号（从1开始）
     */
    private Integer chapterNo;

    /**
     * 章节标题
     */
    private String title;

    /**
     * 章节正文内容（最终发布版本）
     */
    private String content;

    /**
     * 章节结构化元信息（人物/事件/伏笔/摘要等）
     */
    private Object meta;

    /**
     * 来源草稿ID（chapter_draft.id）
     */
    private Long approvedFromDraftId;

    /**
     * 审查通过并发布的时间
     */
    private LocalDateTime approvedAt;

    /**
     * 章节版本号（用于乐观锁/发布迭代）
     */
    private Integer version;
}