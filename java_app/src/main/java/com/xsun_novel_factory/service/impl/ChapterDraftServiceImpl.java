package com.xsun_novel_factory.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xsun_novel_factory.model.enums.DraftStatus;
import com.xsun_novel_factory.model.enums.OutboxStatus;
import com.xsun_novel_factory.model.enums.ReviewDecision;
import com.xsun_novel_factory.model.dto.DraftContent;
import com.xsun_novel_factory.model.dto.PublishResult;
import com.xsun_novel_factory.model.dto.ReviewResult;
import com.xsun_novel_factory.model.entity.Chapter;
import com.xsun_novel_factory.model.entity.ChapterDraft;
import com.xsun_novel_factory.model.entity.ChapterReview;
import com.xsun_novel_factory.model.entity.OutboxEvent;
import com.xsun_novel_factory.model.mapper.ChapterDraftMapper;
import com.xsun_novel_factory.model.mapper.ChapterMapper;
import com.xsun_novel_factory.model.mapper.ChapterReviewMapper;
import com.xsun_novel_factory.model.mapper.OutboxEventMapper;
import com.xsun_novel_factory.service.ChapterDraftService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 章节草稿服务实现类
 * 负责管理小说章节草稿的创建、审核和发布流程
 * 与内容生成代理(Writer)和质量审查代理(Reviewer)协作
 */
@Service
@RequiredArgsConstructor
public class ChapterDraftServiceImpl extends ServiceImpl<ChapterDraftMapper, ChapterDraft>
    implements ChapterDraftService {

    private final ChapterDraftMapper chapterDraftMapper;
    private final ChapterReviewMapper chapterReviewMapper;
    private final ChapterMapper chapterMapper;
    private final OutboxEventMapper outboxEventMapper;

    /**
     * 创建章节草稿
     * 
     * @param novelId 小说ID
     * @param chapterNo 章节序号
     * @param draftContent 草稿内容，包含标题、正文和元数据
     * @param createdBy 创建人标识
     * @return 创建的章节草稿对象
     */
    @Override
    public ChapterDraft createDraft(Long novelId, int chapterNo, DraftContent draftContent, String createdBy) {
        ChapterDraft d = new ChapterDraft();
        d.setNovelId(novelId);
        d.setChapterNo(chapterNo);
        d.setTitle(draftContent.getTitle());
        d.setDraftContent(draftContent.getContent());
        d.setDraftMeta(draftContent.getMetaJson()); // 你的字段名若不同，改这里
        d.setStatus(DraftStatus.DRAFT.name());
        d.setCreatedBy(createdBy);
        d.setLockVersion(0);
        d.setCreatedAt(LocalDateTime.now());
        d.setUpdatedAt(LocalDateTime.now());

        chapterDraftMapper.insert(d);
        return d;
    }

    /**
     * 提交草稿进行审核
     * 将草稿状态从DRAFT或REJECTED更新为SUBMITTED，准备进入审核流程
     * 
     * @param draftId 草稿ID
     * @return 更新后的章节草稿对象
     * @throws IllegalArgumentException 当草稿不存在时抛出
     * @throws IllegalStateException 当草稿状态不允许提交时抛出（如正在审核中或已通过）
     */
    @Override
    public ChapterDraft submitForReview(Long draftId) {
        ChapterDraft d = chapterDraftMapper.selectById(draftId);
        if (d == null) {
            throw new IllegalArgumentException("draft not found: " + draftId);
        }

        String st = d.getStatus();
        if (!(DraftStatus.DRAFT.name().equals(st) || DraftStatus.REJECTED.name().equals(st))) {
            throw new IllegalStateException("draft status not allow submit: " + st);
        }

        d.setStatus(DraftStatus.SUBMITTED.name());
        d.setUpdatedAt(LocalDateTime.now());
        chapterDraftMapper.updateById(d);
        return d;
    }

    /**
     * 标记草稿为审核中状态
     * 将草稿状态从SUBMITTED更新为REVIEWING，表示已开始审核
     * 
     * @param draftId 草稿ID
     * @return 更新后的章节草稿对象
     * @throws IllegalArgumentException 当草稿不存在时抛出
     * @throws IllegalStateException 当草稿状态不是SUBMITTED时抛出
     */
    @Override
    public ChapterDraft markReviewing(Long draftId) {
        ChapterDraft d = chapterDraftMapper.selectById(draftId);
        if (d == null) {
            throw new IllegalArgumentException("draft not found: " + draftId);
        }
        if (!DraftStatus.SUBMITTED.name().equals(d.getStatus())) {
            throw new IllegalStateException("draft status not allow reviewing: " + d.getStatus());
        }

        d.setStatus(DraftStatus.REVIEWING.name());
        d.setUpdatedAt(LocalDateTime.now());
        chapterDraftMapper.updateById(d);
        return d;
    }

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
    @Override
    public ChapterReview recordReviewResult(Long draftId, ReviewResult result) {
        ChapterDraft d = chapterDraftMapper.selectById(draftId);
        if (d == null) {
            throw new IllegalArgumentException("draft not found: " + draftId);
        }
        if (!DraftStatus.REVIEWING.name().equals(d.getStatus())) {
            throw new IllegalStateException("draft status not in REVIEWING: " + d.getStatus());
        }

        ChapterReview r = new ChapterReview();
        r.setDraftId(draftId);
        r.setDecision(result.getDecision().name());
        r.setScoreJson(result.getScoreJson());
        r.setIssuesJson(result.getIssuesJson());
        r.setEvidenceJson(result.getEvidenceJson());
        r.setReviewerVersion(result.getReviewerVersion());
        r.setCreatedAt(LocalDateTime.now());
        chapterReviewMapper.insert(r);

        // 同步推进 draft 状态
        if (result.getDecision() == ReviewDecision.APPROVED) {
            d.setStatus(DraftStatus.APPROVED.name());
        } else {
            d.setStatus(DraftStatus.REJECTED.name());
        }
        d.setUpdatedAt(LocalDateTime.now());
        chapterDraftMapper.updateById(d);

        return r;
    }

    /**
     * 发布已通过审核的草稿
     * 将状态为APPROVED的草稿发布为正式章节，并生成出箱事件以更新图谱
     * 
     * @param draftId 草稿ID
     * @return 发布结果，包含章节ID、版本号和出箱事件ID
     * @throws IllegalArgumentException 当草稿不存在时抛出
     * @throws IllegalStateException 当草稿未通过审核时抛出
     */
    @Override
    @Transactional
    public PublishResult publishIfApproved(Long draftId) {
        ChapterDraft d = chapterDraftMapper.selectById(draftId);
        if (d == null) {
            throw new IllegalArgumentException("draft not found: " + draftId);
        }
        if (!DraftStatus.APPROVED.name().equals(d.getStatus())) {
            throw new IllegalStateException("draft not approved: " + d.getStatus());
        }

        // upsert chapter by (novelId, chapterNo)
        Chapter chapter = chapterMapper.selectOne(new LambdaQueryWrapper<Chapter>()
                .eq(Chapter::getNovelId, d.getNovelId())
                .eq(Chapter::getChapterNo, d.getChapterNo())
                .last("limit 1"));

        if (chapter == null) {
            chapter = new Chapter();
            chapter.setNovelId(d.getNovelId());
            chapter.setChapterNo(d.getChapterNo());
            chapter.setTitle(d.getTitle());
            chapter.setContent(d.getDraftContent());
            chapter.setMeta(d.getDraftMeta()); // 你的字段名若不同，改这里
            chapter.setApprovedFromDraftId(draftId);
            chapter.setApprovedAt(LocalDateTime.now());
            chapter.setVersion(1);
            chapterMapper.insert(chapter);
        } else {
            // 版本递增
            Integer v = chapter.getVersion() == null ? 1 : chapter.getVersion();
            chapter.setTitle(d.getTitle());
            chapter.setContent(d.getDraftContent());
            chapter.setMeta(d.getDraftMeta());
            chapter.setApprovedFromDraftId(draftId);
            chapter.setApprovedAt(LocalDateTime.now());
            chapter.setVersion(v + 1);
            chapterMapper.updateById(chapter);
        }

        // outbox event：幂等键（防重复）
        String idempotencyKey = "CHAPTER_APPROVED:" + chapter.getId() + ":v" + chapter.getVersion();

        OutboxEvent event = new OutboxEvent();
        event.setAggregateType("CHAPTER");
        event.setAggregateId(chapter.getId());
        event.setEventType("CHAPTER_APPROVED");
        event.setPayloadJson(buildUpdateGraphPayloadJson(chapter, d));
        event.setStatus(OutboxStatus.NEW.name());
        event.setRetryCount(0);
        event.setNextRetryAt(null);
        event.setErrorMessage(null);
        event.setIdempotencyKey(idempotencyKey);
        event.setCreatedAt(LocalDateTime.now());
        event.setUpdatedAt(LocalDateTime.now());

        // 注意：如果你 DB 里 idempotency_key 唯一约束命中，会抛异常
        outboxEventMapper.insert(event);

        return PublishResult.builder()
                .chapterId(chapter.getId())
                .chapterVersion(chapter.getVersion())
                .outboxEventId(event.getId())
                .build();
    }

    /**
     * 构建更新图谱的载荷JSON
     * 为出箱事件创建载荷，包含章节和草稿信息，用于Python GraphRAG服务处理
     * 
     * @param chapter 章节对象
     * @param draft 草稿对象
     * @return JSON格式的载荷字符串
     */
    private String buildUpdateGraphPayloadJson(Chapter chapter, ChapterDraft draft) {
        // 第一版先简单拼 JSON 字符串（你后续可以用 Jackson ObjectMapper）
        // 重点：让 Python 能幂等（用 chapterId + version 或 approvedFromDraftId）
        return "{"
                + "\"chapterId\":" + chapter.getId() + ","
                + "\"novelId\":" + chapter.getNovelId() + ","
                + "\"chapterNo\":" + chapter.getChapterNo() + ","
                + "\"chapterVersion\":" + chapter.getVersion() + ","
                + "\"approvedFromDraftId\":" + draft.getId()
                + "}";
    }
}




