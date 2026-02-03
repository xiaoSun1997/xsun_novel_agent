package com.xsun_novel_factory.service;

import com.xsun_novel_factory.agent.ChiefCoordinator;
import com.xsun_novel_factory.agent.ContentGenerationAgent;
import com.xsun_novel_factory.agent.QualityReviewAgent;
import com.xsun_novel_factory.feign.GraphRagClient;
import com.xsun_novel_factory.feign.MilvusStoreClient;
import com.xsun_novel_factory.model.dto.ChapterRunResult;
import com.xsun_novel_factory.model.dto.DraftContent;
import com.xsun_novel_factory.model.dto.NovelCreationRequest;
import com.xsun_novel_factory.model.dto.ReviewResult;
import com.xsun_novel_factory.model.dto.graph.CheckLogicReq;
import com.xsun_novel_factory.model.dto.graph.ExtractGraphReq;
import com.xsun_novel_factory.model.dto.graph.GraphResponse;
import com.xsun_novel_factory.model.dto.graph.QueryGraphReq;
import com.xsun_novel_factory.model.dto.milvus.RetrieveReq;
import com.xsun_novel_factory.model.dto.milvus.RetrieveFilters;
import com.xsun_novel_factory.model.entity.ChapterDraft;
import com.xsun_novel_factory.model.entity.ChapterReview;
import com.xsun_novel_factory.model.entity.Novel;
import com.xsun_novel_factory.service.impl.ChapterDraftServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * 小说创作工作流服务
 * 实现完整的创作流程：参考资料检索 -> 大纲生成 -> 章节创作 -> 一致性检查 -> 用户确认 -> 发布
 */
@Service
@RequiredArgsConstructor
public class NovelCreationWorkflowService {

    private final ChiefCoordinator chiefCoordinator;
    private final ContentGenerationAgent contentGenerationAgent;
    private final QualityReviewAgent qualityReviewAgent;
    private final ChapterDraftService chapterDraftService;
    private final MilvusStoreClient milvusStoreClient;
    private final GraphRagClient graphRagClient;

    /**
     * 创建新小说（完整流程）
     * 1. 从Milvus检索参考资料
     * 2. 生成大纲
     * 3. 等待用户确认
     * 4. 开始章节创作循环
     */
    public Novel createNovelWithFullWorkflow(NovelCreationRequest request) {
        // 1. 从Milvus检索参考资料
        Map<String, Object> referenceMaterials = retrieveReferenceMaterials(request.getReferenceNovelId());
        
        // 2. 生成大纲（结合检索到的参考资料）
        Novel novel = chiefCoordinator.createNewNovelWithParams(request);
        
        // 3. 这里应该有一个用户确认步骤，实际实现中可能需要异步处理
        boolean outlineConfirmed = confirmOutline(novel.getId());
        
        if (!outlineConfirmed) {
            // 如果大纲未确认，可能需要重新生成
            throw new RuntimeException("Outline was not confirmed by user");
        }
        
        return novel;
    }

    /**
     * 检索参考资料
     */
    private Map<String, Object> retrieveReferenceMaterials(String referenceNovelId) {
        RetrieveReq retrieveReq = new RetrieveReq();
        retrieveReq.setQuestions(java.util.Arrays.asList("写作技巧", "故事结构", "人物塑造", "世界观设定"));
        RetrieveFilters filters = new RetrieveFilters();
        filters.setWorkId(referenceNovelId);
        filters.setDocTypes(java.util.Arrays.asList("technique", "story", "world_setting"));
        retrieveReq.setFilter(filters);
        retrieveReq.setTopK(5);
        
        return milvusStoreClient.ragRetrieve(retrieveReq);
    }

    /**
     * 确认大纲
     */
    private boolean confirmOutline(Long novelId) {
        // 在实际实现中，这通常是一个异步过程，需要等待用户确认
        // 这里只是一个示意实现
        return true; // 假设用户确认了大纲
    }

    /**
     * 写作章节（带完整检查流程）
     */
    public ChapterRunResult writeChapterWithFullWorkflow(Long novelId, int chapterNo, String createdBy) {
        // 1. 查询上章人物关系图谱，用于写作参考
        Map<String, Object> characterRelations = queryCharacterRelations(novelId.toString(), chapterNo - 1);
        
        // 2. 生成章节草稿
        DraftContent draftContent = contentGenerationAgent.generateDraft(novelId, chapterNo,null,null,null);
        ChapterDraft draft = chapterDraftService.createDraft(novelId, chapterNo, draftContent, createdBy);

        // 3. 提交审查
        chapterDraftService.submitForReview(draft.getId());
        chapterDraftService.markReviewing(draft.getId());

        // 4. GraphRAG 一致性检查
        boolean consistencyPassed = checkConsistency(novelId.toString(), chapterNo, draftContent.getContent());
        
        if (!consistencyPassed) {
            // 一致性检查失败，需要重新生成
            throw new RuntimeException("Chapter consistency check failed, needs rewrite");
        }

        // 5. 质量审查
        ReviewResult reviewResult = qualityReviewAgent.reviewDraft(draft.getId());
        ChapterReview review = chapterDraftService.recordReviewResult(draft.getId(), reviewResult);

        // 6. 这里应该有用户确认步骤
        boolean chapterConfirmed = confirmChapter(draft.getId());
        
        if (!chapterConfirmed) {
            // 用户未确认，需要重新生成
            throw new RuntimeException("Chapter was not confirmed by user, needs rewrite");
        }

        // 7. 发布章节
        var publishResult = chapterDraftService.publishIfApproved(draft.getId());
        
        // 8. 更新知识图谱
        updateKnowledgeGraph(novelId.toString(), chapterNo, draftContent.getContent());

        // 9. 检查是否为卷末/书末
        String completionStatus = checkCompletionStatus(novelId, chapterNo);

        return ChapterRunResult.builder()
                .draftId(draft.getId())
                .reviewId(review.getId())
                .publishResult(publishResult)
                .build();
    }

    /**
     * 查询人物关系图谱
     */
    private Map<String, Object> queryCharacterRelations(String novelId, int chapter) {
        QueryGraphReq queryGraphReq = new QueryGraphReq();
        queryGraphReq.setNovelId(novelId);
        queryGraphReq.setQueryType("character");
        queryGraphReq.setChapter(chapter);
        
        GraphResponse response = graphRagClient.queryGraph(queryGraphReq);
        return Map.of("characters", response.getResults());
    }

    /**
     * 检查一致性
     */
    private boolean checkConsistency(String novelId, int chapter, String content) {
        CheckLogicReq checkLogicReq = new CheckLogicReq();
        checkLogicReq.setNovelId(novelId);
        checkLogicReq.setChapter(chapter);
        checkLogicReq.setContent(content);
        
        GraphResponse response = graphRagClient.checkLogic(checkLogicReq);
        return !response.getHasErrors();
    }

    /**
     * 确认章节
     */
    private boolean confirmChapter(Long draftId) {
        // 在实际实现中，这通常是一个异步过程，需要等待用户确认
        // 这里只是一个示意实现
        return true; // 假设用户确认了章节
    }

    /**
     * 更新知识图谱
     */
    private void updateKnowledgeGraph(String novelId, int chapter, String content) {
        ExtractGraphReq extractGraphReq = new ExtractGraphReq();
        extractGraphReq.setNovelId(novelId);
        extractGraphReq.setChapter(chapter);
        extractGraphReq.setContent(content);
        
        graphRagClient.extractGraph(extractGraphReq);
    }

    /**
     * 检查完成状态（是否为卷末/书末）
     */
    private String checkCompletionStatus(Long novelId, int chapterNo) {
        // 这里需要根据小说的设定来判断是否为卷末或书末
        // 示例实现，实际需要更复杂的逻辑
        if (chapterNo % 20 == 0) { // 假设每20章为一卷
            return "VOLUME_END";
        } else if (chapterNo >= 200) { // 假设200章为全书完结
            return "BOOK_END";
        } else {
            return "CONTINUE";
        }
    }
}