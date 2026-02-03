package com.xsun_novel_factory.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;
import java.util.Date;
import lombok.Data;

/**
 * 章节审查记录表（审查历史与证据）
 * @TableName chapter_review
 */
@TableName(value ="chapter_review")
@Data
public class ChapterReview {
    /**
     * 审查记录ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 被审查的草稿ID
     */
    private Long draftId;

    /**
     * 审查结论（APPROVED/REJECTED）
     */
    private String decision;

    /**
     * 审查评分（连贯性/风格一致性/剧情一致性等）
     */
    private Object scoreJson;

    /**
     * 问题列表（问题类型/严重度/位置/修改建议）
     */
    private Object issuesJson;

    /**
     * 审查证据（Chroma命中片段/GraphRAG发现）
     */
    private Object evidenceJson;

    /**
     * 审查模型或规则版本标识
     */
    private String reviewerVersion;

    /**
     * 审查时间
     */
    private LocalDateTime createdAt;
}