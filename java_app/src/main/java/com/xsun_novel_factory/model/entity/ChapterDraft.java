package com.xsun_novel_factory.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;
import java.util.Date;
import lombok.Data;

/**
 * 章节草稿表（写作/修改/待审阶段）
 * @TableName chapter_draft
 */
@TableName(value ="chapter_draft")
@Data
public class ChapterDraft {
    /**
     * 草稿ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 所属小说ID
     */
    private Long novelId;

    /**
     * 章节序号
     */
    private Integer chapterNo;

    /**
     * 草稿章节标题
     */
    private String title;

    /**
     * 草稿正文内容
     */
    private String draftContent;

    /**
     * 草稿阶段结构化信息（模型抽取/分析结果）
     */
    private Object draftMeta;

    /**
     * 草稿状态（DRAFT/SUBMITTED/REVIEWING/REJECTED/APPROVED）
     */
    private String status;

    /**
     * 创建人（用户ID/系统标识）
     */
    private String createdBy;

    /**
     * 乐观锁版本号（并发控制）
     */
    private Integer lockVersion;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}