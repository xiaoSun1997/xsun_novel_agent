package com.xsun_novel_factory.agent;

import cn.hutool.json.JSONUtil;
import com.xsun_novel_factory.agent.ai.CreateNovelAi;
import com.xsun_novel_factory.model.dto.*;
import com.xsun_novel_factory.model.entity.ChapterDraft;
import com.xsun_novel_factory.model.entity.ChapterReview;
import com.xsun_novel_factory.model.entity.Novel;
import com.xsun_novel_factory.service.ChapterDraftService;
import com.xsun_novel_factory.service.NovelService;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChiefCoordinatorImpl implements ChiefCoordinator {

    private final ContentGenerationAgent contentGenerationAgent;
    private final QualityReviewAgent qualityReviewAgent;
    private final ChapterDraftService chapterDraftService;
    private final NovelService novelService;
    private final ChatModel chatModel;

    @Override
    public Novel createNewNovel(Integer novelId) {
        if (null != novelId) {
            Novel novel = novelService.getById(novelId);
            if (null != novel) {
                return novel;
            }
        }
        CreateNovelAi createNovelAi = AiServices.builder(CreateNovelAi.class)
                .chatModel(chatModel)
                .build();
        //TODO 参数
        String novelType = "";
        String referenceNovel = "";
        String referenceNovelStyle = "";
        String novelAndGetJson = createNovelAi.createNovelAndGetJson(novelType, referenceNovel, referenceNovelStyle);
        log.info("大纲大致为：{}",novelAndGetJson);
        NovelDto bean = JSONUtil.toBean(novelAndGetJson, NovelDto.class);
        return novelService.createNovel(bean);
    }

    @Override
    public ChapterRunResult generateAndReviewChapter(Long novelId, int chapterNo, String createdBy) {
        // 1) 生成草稿
        DraftContent draftContent = contentGenerationAgent.generateDraft(novelId, chapterNo,null,null,null);
        ChapterDraft draft = chapterDraftService.createDraft(novelId, chapterNo, draftContent, createdBy);

        // 2) 提交审查 -> 标记审查中
        chapterDraftService.submitForReview(draft.getId());
        chapterDraftService.markReviewing(draft.getId());

        // 3) 审查
        ReviewResult reviewResult = qualityReviewAgent.reviewDraft(draft.getId());
        ChapterReview review = chapterDraftService.recordReviewResult(draft.getId(), reviewResult);

        // 4) 通过则发布（写 chapter + outbox）
        PublishResult publishResult = null;
        if ("APPROVED".equals(review.getDecision())) {
            publishResult = chapterDraftService.publishIfApproved(draft.getId());
        }

        return ChapterRunResult.builder()
                .draftId(draft.getId())
                .reviewId(review.getId())
                .publishResult(publishResult)
                .build();
    }

    @Override
    public Novel createNewNovelWithParams(NovelCreationRequest request) {
        CreateNovelAi createNovelAi = AiServices.builder(CreateNovelAi.class)
                .chatModel(chatModel)
                .build();
        
        String novelType = request.getNovelType();
        String referenceNovel = request.getReferenceNovelId();
        String referenceNovelStyle = request.getReferenceNovelStyle();
        
        String novelAndGetJson = createNovelAi.createNovelAndGetJson(novelType, referenceNovel, referenceNovelStyle);
        log.info("大纲大致为：{}", novelAndGetJson);
        NovelDto bean = JSONUtil.toBean(novelAndGetJson, NovelDto.class);
        return novelService.createNovel(bean);
    }
}