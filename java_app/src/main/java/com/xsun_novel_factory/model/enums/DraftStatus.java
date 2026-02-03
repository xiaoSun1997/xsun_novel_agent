package com.xsun_novel_factory.model.enums;

/**
 * 章节草稿状态枚举
 * 定义了章节草稿在其生命周期中的各种状态
 */
public enum DraftStatus {
    /** 初始草稿状态 */
    DRAFT,
    /** 已提交审核状态 */
    SUBMITTED,
    /** 正在审核中状态 */
    REVIEWING,
    /** 审核被拒绝状态 */
    REJECTED,
    /** 审核通过状态 */
    APPROVED
}