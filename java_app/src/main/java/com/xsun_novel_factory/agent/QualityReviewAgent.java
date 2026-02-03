package com.xsun_novel_factory.agent;

import com.xsun_novel_factory.model.dto.ReviewResult;

/**
 * 质量审查agent
 * @author xiaosun
 */
public interface QualityReviewAgent {

    /**
     * 审核章节草稿
     */
    ReviewResult reviewDraft(Long draftId);
}
