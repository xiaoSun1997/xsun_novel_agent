package com.xsun_novel_factory.model.dto;

import com.xsun_novel_factory.model.enums.ReviewDecision;
import lombok.*;

/**
 * 章节审核结果传输对象
 * 包含审核决策、评分、问题列表、证据等信息
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewResult {
    /** 审核决策：通过或拒绝 */
    private ReviewDecision decision;
    /** JSON 字符串：评分 */
    private String scoreJson;
    /** JSON 字符串：问题列表 */
    private String issuesJson;
    /** JSON 字符串：证据 */
    private String evidenceJson;

    /** 审查模型或规则版本标识 */
    private String reviewerVersion;
    /** 追踪ID，用于请求追踪 */
    private String traceId;
}