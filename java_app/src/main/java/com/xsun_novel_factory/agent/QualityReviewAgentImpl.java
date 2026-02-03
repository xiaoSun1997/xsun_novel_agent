package com.xsun_novel_factory.agent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xsun_novel_factory.agent.ai.ReviewAi;
import com.xsun_novel_factory.model.entity.ChapterDraft;
import com.xsun_novel_factory.model.entity.Novel;
import com.xsun_novel_factory.model.entity.Chapter;
import com.xsun_novel_factory.model.enums.ReviewDecision;
import com.xsun_novel_factory.model.dto.ReviewResult;
import com.xsun_novel_factory.model.dto.graph.QueryGraphReq;
import com.xsun_novel_factory.model.dto.graph.GraphResponse;
import com.xsun_novel_factory.model.dto.milvus.RetrieveReq;
import com.xsun_novel_factory.model.dto.milvus.RetrieveFilters;
import com.xsun_novel_factory.model.mapper.ChapterDraftMapper;
import com.xsun_novel_factory.service.NovelService;
import com.xsun_novel_factory.service.ChapterService;
import com.xsun_novel_factory.feign.GraphRagClient;
import com.xsun_novel_factory.feign.MilvusStoreClient;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class QualityReviewAgentImpl implements QualityReviewAgent {

    private final ChapterDraftMapper chapterDraftMapper;
    private final NovelService novelService;
    private final ChapterService chapterService;
    private final GraphRagClient graphRagClient;
    private final MilvusStoreClient milvusStoreClient;
    private final ChatModel chatModel;
    private final ObjectMapper objectMapper;

    @Override
    public ReviewResult reviewDraft(Long draftId) {
        // 查询草稿内容
        ChapterDraft draft = chapterDraftMapper.selectById(draftId);
        if (draft == null) {
            throw new IllegalArgumentException("draft not found: " + draftId);
        }
        
        // 获取前文总结
        String beforeSummary = getPreviousSummary(draft.getNovelId(), draft.getChapterNo());
        
        // 获取前面章节内容
        String beforeChapterInfo = getPreviousChaptersContent(draft.getNovelId(), draft.getChapterNo());
        
        // 从GraphRAG获取人物关系等信息
        String characterRelations = getCharacterRelationsFromGraph(draft.getNovelId(), draft.getChapterNo() - 1);
        
        // 从Milvus获取相关参考信息
        String referenceInfo = getReferenceInfoFromMilvus(draft.getNovelId(), draft.getDraftContent());
        
        ReviewAi reviewerAi = AiServices.builder(ReviewAi.class).chatModel(chatModel).build();
        String json = reviewerAi.reviewAsJson(
                draft.getNovelId(),
                draft.getDraftContent(),
                beforeSummary,
                beforeChapterInfo,
                characterRelations,
                referenceInfo
        );
        
        // 解析模型输出
        JsonNode root;
        try {
            root = objectMapper.readTree(json);
        } catch (Exception e) {
            // 如果模型偶发输出非 JSON：第一版直接判 REJECTED，并把原文放 evidence 方便排查
            return ReviewResult.builder()
                    .decision(ReviewDecision.REJECTED)
                    .scoreJson("{\"parse_error\":1}")
                    .issuesJson("[{\"type\":\"FORMAT\",\"severity\":\"HIGH\",\"quote\":\"\",\"suggestion\":\"模型未输出合法JSON\"}]")
                    .evidenceJson("{\"raw\":\"" + escape(json) + "\"}")
                    .reviewerVersion("reviewer-ai-json-v1")
                    .traceId(UUID.randomUUID().toString())
                    .build();
        }

        String decisionStr = safeText(root, "decision", "REJECTED");
        ReviewDecision decision = "APPROVED".equalsIgnoreCase(decisionStr)
                ? ReviewDecision.APPROVED : ReviewDecision.REJECTED;

        String scoreJson = nodeToJson(root.get("score_json"), "{}");
        String issuesJson = nodeToJson(root.get("issues_json"), "[]");
        String evidenceJson = nodeToJson(root.get("evidence_json"), "{}");

        return ReviewResult.builder()
                .decision(decision)
                .scoreJson(scoreJson)
                .issuesJson(issuesJson)
                .evidenceJson(evidenceJson)
                .reviewerVersion("reviewer-ai-json-v1")
                .traceId(UUID.randomUUID().toString())
                .build();
    }
    
    /**
     * 获取前文总结
     */
    private String getPreviousSummary(Long novelId, int currentChapterNo) {
        // 这里需要从数据库中获取最新的总结
        // 实际实现中需要查询Summary表
        return "之前章节的总结信息";
    }
    
    /**
     * 获取前面章节内容
     */
    private String getPreviousChaptersContent(Long novelId, int currentChapterNo) {
        if (currentChapterNo <= 1) {
            return ""; // 第一章没有前文
        }
        
        List<Chapter> previousChapters = chapterService.getChaptersInRange(novelId, 1, currentChapterNo - 1);
        if (previousChapters.isEmpty()) {
            return "";
        }
        
        StringBuilder sb = new StringBuilder();
        for (Chapter chapter : previousChapters) {
            sb.append("第").append(chapter.getChapterNo()).append("章:\n")
              .append(chapter.getContent()).append("\n\n");
        }
        return sb.toString();
    }
    
    /**
     * 从GraphRAG获取人物关系
     */
    private String getCharacterRelationsFromGraph(Long novelId, int chapterNo) {
        QueryGraphReq queryGraphReq = new QueryGraphReq();
        queryGraphReq.setNovelId(novelId.toString());
        queryGraphReq.setQueryType("character");
        queryGraphReq.setChapter(chapterNo);
        
        try {
            GraphResponse response = graphRagClient.queryGraph(queryGraphReq);
            return response.getResults() != null ? response.getResults().toString() : "";
        } catch (Exception e) {
            return "无法获取人物关系图谱信息";
        }
    }
    
    /**
     * 从Milvus获取参考信息
     */
    private String getReferenceInfoFromMilvus(Long novelId, String content) {
        try {
            RetrieveReq retrieveReq = new RetrieveReq();
            retrieveReq.setQuestions(List.of(content.substring(0, Math.min(content.length(), 200)) + "...")); // 截取部分内容作为检索问题
            RetrieveFilters filters = new RetrieveFilters();
            filters.setWorkId(novelId.toString());
            retrieveReq.setFilter(filters);
            retrieveReq.setTopK(3);
            
            Map<String, Object> results = milvusStoreClient.ragRetrieve(retrieveReq);
            return results.toString();
        } catch (Exception e) {
            return "无法获取Milvus参考信息";
        }
    }

    private String safeText(JsonNode root, String field, String def) {
        JsonNode n = root.get(field);
        return (n == null || n.isNull()) ? def : n.asText(def);
    }

    private String nodeToJson(JsonNode node, String def) {
        if (node == null || node.isNull()) return def;
        try {
            return objectMapper.writeValueAsString(node);
        } catch (Exception e) { 
            return def;
        }
    }

    private String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}