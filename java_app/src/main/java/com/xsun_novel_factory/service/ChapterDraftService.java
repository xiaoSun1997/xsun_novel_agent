package com.xsun_novel_factory.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xsun_novel_factory.model.dto.DraftContent;
import com.xsun_novel_factory.model.dto.PublishResult;
import com.xsun_novel_factory.model.dto.ReviewResult;
import com.xsun_novel_factory.model.entity.ChapterDraft;
import com.xsun_novel_factory.model.entity.ChapterReview;

/**
 * 章节草稿服务接口
 * 负责管理小说章节草稿的创建、审核和发布流程
 * 与内容生成代理(Writer)和质量审查代理(Reviewer)协作
 *
 * @author xiaosun
 */
public interface ChapterDraftService extends IService<ChapterDraft> {

    /**
     * 创建章节草稿
     *
     * @param novelId 小说ID
     * @param chapterNo 章节序号
     * @param draftContent 草稿内容，包含标题、正文和元数据
     * @param createdBy 创建人标识
     * @return 创建的章节草稿对象
     */
    ChapterDraft createDraft(Long novelId, int chapterNo, DraftContent draftContent, String createdBy);

    /**
     * 提交草稿进行审核
     * 将草稿状态从DRAFT或REJECTED更新为SUBMITTED，准备进入审核流程
     *
     * @param draftId 草稿ID
     * @return 更新后的章节草稿对象
     * @throws IllegalArgumentException 当草稿不存在时抛出
     * @throws IllegalStateException 当草稿状态不允许提交时抛出（如正在审核中或已通过）
     */
    ChapterDraft submitForReview(Long draftId);

    /**
     * 标记草稿为审核中状态
     * 将草稿状态从SUBMITTED更新为REVIEWING，表示已开始审核
     *
     * @param draftId 草稿ID
     * @return 更新后的章节草稿对象
     * @throws IllegalArgumentException 当草稿不存在时抛出
     * @throws IllegalStateException 当草稿状态不是SUBMITTED时抛出
     */
    ChapterDraft markReviewing(Long draftId);

    /**
     * 记录审核结果
     * 根据审核结果更新草稿状态，并保存审核记录
     *
     * @param draftId 草稿ID
     * @param result 审核结果，包含决策、评分、问题列表等信息
     * @return 创建的章节审核记录对象
     * @throws IllegalArgumentException 当草稿不存在时抛出
     * @throws IllegalStateException 当草稿状态不是REVIEWING时抛出
     */
    ChapterReview recordReviewResult(Long draftId, ReviewResult result);

    /**
     * 发布已通过审核的草稿
     * 将状态为APPROVED的草稿发布为正式章节，并生成出箱事件以更新图谱
     *
     * @param draftId 草稿ID
     * @return 发布结果，包含章节ID、版本号和出箱事件ID
     * @throws IllegalArgumentException 当草稿不存在时抛出
     * @throws IllegalStateException 当草稿未通过审核时抛出
     */
    PublishResult publishIfApproved(Long draftId);

}