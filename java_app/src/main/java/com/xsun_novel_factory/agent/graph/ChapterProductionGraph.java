package com.xsun_novel_factory.agent.graph;

import cn.hutool.json.JSONUtil;
import com.xsun_novel_factory.agent.ChiefCoordinator;
import com.xsun_novel_factory.agent.ContentGenerationAgent;
import com.xsun_novel_factory.agent.QualityReviewAgent;
import com.xsun_novel_factory.agent.ai.SummaryAi;
import com.xsun_novel_factory.agent.state.ChapterFlowState;
import com.xsun_novel_factory.feign.GraphRagClient;
import com.xsun_novel_factory.feign.MilvusStoreClient;
import com.xsun_novel_factory.model.dto.DraftContent;
import com.xsun_novel_factory.model.dto.NovelDto;
import com.xsun_novel_factory.model.dto.ReviewResult;
import com.xsun_novel_factory.model.dto.graph.CheckLogicReq;
import com.xsun_novel_factory.model.dto.graph.ExtractGraphReq;
import com.xsun_novel_factory.model.dto.graph.GraphResponse;
import com.xsun_novel_factory.model.dto.graph.QueryGraphReq;
import com.xsun_novel_factory.model.dto.milvus.RetrieveReq;
import com.xsun_novel_factory.model.dto.milvus.RetrieveFilters;
import com.xsun_novel_factory.model.entity.Chapter;
import com.xsun_novel_factory.model.entity.ChapterDraft;
import com.xsun_novel_factory.model.entity.ChapterReview;
import com.xsun_novel_factory.model.entity.Novel;
import com.xsun_novel_factory.model.entity.Summary;
import com.xsun_novel_factory.model.enums.ReviewDecision;
import com.xsun_novel_factory.service.ChapterDraftService;
import com.xsun_novel_factory.service.ChapterService;
import com.xsun_novel_factory.service.NovelService;
import com.xsun_novel_factory.service.SummaryService;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.bsc.langgraph4j.*;
import org.bsc.langgraph4j.action.AsyncEdgeAction;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.bsc.langgraph4j.GraphDefinition.END;

/**
 * @program: ChapterProductionGraph.java
 * @description: 使用 LangGraph 实现的小说创作全流程
 * @author: sunmouren
 * @create: 2026-01-06
 **/
@Component
@RequiredArgsConstructor
@Slf4j
public class ChapterProductionGraph {

    private final ChiefCoordinator chiefCoordinator;
    private final ContentGenerationAgent contentGenerationAgent;
    private final QualityReviewAgent qualityReviewAgent;
    private final ChapterDraftService chapterDraftService;
    private final MilvusStoreClient milvusStoreClient;
    private final GraphRagClient graphRagClient;
    private final ChapterService chapterService;
    private final NovelService novelService;
    private final SummaryService summaryService;
    private final ChatModel chatModel;

    public ChapterFlowState createNovel(Integer novelId) throws GraphStateException {
        StateGraph<ChapterFlowState> graphBuilder = new StateGraph<>(ChapterFlowState.SCHEMA, ChapterFlowState::new);

        // 创建小说完整流程
        graphBuilder
                // 初始化小说
                .addNode("init", initNode(novelId))
                // 检索参考资料
                .addNode("retrieveRefs", retrieveReferencesNode())
                // 生成大纲
                .addNode("generateOutline", generateOutlineNode())
                // 确认大纲
                .addNode("confirmOutline", confirmOutlineNode())
                // 开始写作循环 - 生成章节
                .addNode("generateChapter", generateChapterNode())
                // GraphRAG 一致性检查
                .addNode("checkConsistency", checkConsistencyNode())
                // 质量审查
                .addNode("review", reviewNode())
                // 用户确认
                .addNode("confirmChapter", confirmChapterNode())
                // 发布章节
                .addNode("publish", publishNode())
                // 更新知识图谱
                .addNode("updateGraph", updateGraphNode())
                // 检查是否为卷末/书末
                .addNode("checkCompletion", checkCompletionNode())
                // 生成章节总结 - 已有GRAPHRAG 去掉？
//                .addNode("summarize", summarizeNode())
                // 检查是否需要阶段性总结
                .addNode("checkMilestone", checkMilestoneNode());

        // 定义流程
        graphBuilder
                .addEdge(GraphDefinition.START, "init")
                .addEdge("init", "retrieveRefs")
                .addEdge("retrieveRefs", "generateOutline")
                .addEdge("generateOutline", "confirmOutline")
                // 大纲确认后开始章节写作循环
                .addConditionalEdges("confirmOutline",
                        AsyncEdgeAction.edge_async(state -> {
                            boolean outlineConfirmed = state.<Boolean>value("outlineConfirmed").orElse(false);
                            return outlineConfirmed ? "generateChapter" : "generateOutline"; // 如果未确认，重新生成大纲
                        }),
                        Map.of("generateChapter", "generateChapter", "generateOutline", "generateOutline"))
                .addEdge("generateChapter", "checkConsistency")
                .addConditionalEdges("checkConsistency",
                        AsyncEdgeAction.edge_async(state -> {
                            boolean passed = state.<Boolean>value("consistencyPassed").orElse(false);
                            return passed ? "review" : "generateChapter"; // 如果一致性检查失败，重新生成章节
                        }),
                        Map.of("review", "review", "generateChapter", "generateChapter"))
//                .addEdge("review", "confirmChapter")
                .addConditionalEdges("review",
                        AsyncEdgeAction.edge_async(state -> {
                            String decision = state.<String>value("decision").orElse("APPROVED");
                            return "APPROVED".equals(decision) ? "confirmChapter" : "generateChapter"; // 如果审查失败 重新生成
                        }),
                        Map.of("confirmChapter", "confirmChapter", "generateChapter", "generateChapter"))
                .addConditionalEdges("confirmChapter",
                        AsyncEdgeAction.edge_async(state -> {
                            boolean chapterConfirmed = state.<Boolean>value("chapterConfirmed").orElse(false);
                            return chapterConfirmed ? "publish" : "generateChapter"; // 如果用户不确认，重新生成章节
                        }),
                        Map.of("publish", "publish", "generateChapter", "generateChapter"))
                .addEdge("publish", "updateGraph")
                .addEdge("updateGraph", "checkCompletion")
                .addConditionalEdges("checkCompletion",
                        AsyncEdgeAction.edge_async(state -> {
                            String completionStatus = state.<String>value("completionStatus").orElse("CONTINUE");
                            if ("BOOK_END".equals(completionStatus)) {
                                return END; // 全书结束
                            } else {
                                return "checkMilestone"; // 继续写作
                            }
                        }),
                        Map.of( END, END, "checkMilestone", "checkMilestone"))
                .addConditionalEdges("checkMilestone",
                        AsyncEdgeAction.edge_async(state -> {
                            return "retrieveRefs"; // 继续写作下一章
                        }),
                        Map.of(
                                "retrieveRefs", "retrieveRefs"
                        ));

        // 关键修改：使用 CompileConfig 设置更大的最大迭代次数
        CompileConfig compileConfig = CompileConfig.builder()
                .recursionLimit(1000)  // 设置最大迭代次数为 10000，可根据需要调整
                .build();

        CompiledGraph<ChapterFlowState> compile = graphBuilder.compile(compileConfig);
        Map<String, Object> initialInput = Map.of(
                "novelId", novelId,
                "chapterNo", 1
        );
        ChapterFlowState last = null;

        // 设置最大迭代次数以避免无限循环，支持更大的迭代次数
        int maxIterations = 1000; // 支持多达1000次迭代
        int iterationCount = 0;
        
        for (var step : compile.stream(initialInput)) {
            last = (ChapterFlowState) step.state();
            iterationCount++;
            
            if (iterationCount >= maxIterations) {
                log.warn("达到最大迭代次数限制: {}，当前章节号: {}", maxIterations, last.chapterNo());
                break;
            }
            
            // 添加日志来跟踪进度
            if (iterationCount % 100 == 0) {
                log.info("已完成 {} 次迭代，当前章节号: {}", iterationCount, last.chapterNo());
            }
        }
        
        log.info("总共执行了 {} 次迭代，完成小说创作", iterationCount);
        return last;
    }

    // 初始化小说
    private AsyncNodeAction<ChapterFlowState> initNode(Integer novelId) {
        return AsyncNodeAction.node_async(state -> {
            log.info("1.init 开始初始化小说 ");
            Novel novel = chiefCoordinator.createNewNovel(novelId);
            final NovelDto novelDto = new NovelDto();
            novelDto.setTitle(novel.getTitle()).setDescription(novel.getDescription());
            List<NovelDto.Volume> list = JSONUtil.toList(novel.getVolumes(), NovelDto.Volume.class);
            novelDto.setVolumes(list);
            //查询最新章节
            Integer newChapterNo = chapterService.findLatestChapter(novelId);
            log.info("1.init 初始化小说完成,下一章章节号为：{}",newChapterNo);

            return Map.of("novelId", novel.getId(), "novel_info", novelDto,"chapterNo",newChapterNo);
        });
    }

    // 检索参考资料
    private AsyncNodeAction<ChapterFlowState> retrieveReferencesNode() {
        return AsyncNodeAction.node_async(state -> {
            log.info("2.retrieveRefs 开始检索参考资料 ");
            // 使用 Milvus 检索相关写作理论和参考小说
            // TODO 让ai根据大纲等内容，最近的章节优先构思一下新的章节的大纲，和新的章节需要问的问题
            RetrieveReq retrieveReq = new RetrieveReq();
            retrieveReq.setQuestions(java.util.Arrays.asList("如何描写诡异恐怖的场景？", "如何设计恐怖故事故事结构", "如何进行人物塑造", "如何构建一个中式克苏鲁的世界观设定"));
            RetrieveFilters filters = new RetrieveFilters();
            filters.setWorkId("4");
            filters.setDocTypes(java.util.Arrays.asList("technique", "story", "world_setting"));
            retrieveReq.setFilter(filters);
            retrieveReq.setTopK(5);

            // 从 Milvus 服务获取检索结果
            Map<String, Object> results = milvusStoreClient.ragRetrieve(retrieveReq);
            log.info("milvus 搜索的结果为：{}", results);
            log.info("2.retrieveRefs 检索参考资料完成 ");
            return Map.of("referenceMaterials", results);
        });
    }

    // 生成大纲
    private AsyncNodeAction<ChapterFlowState> generateOutlineNode() {
        return AsyncNodeAction.node_async(state -> {
            log.info("3.generateOutline 开始生成大纲 ");
            // AI 生成大纲，结合检索到的参考资料
            String novelType = "玄幻"; // 这里应该从state或request中获取
            String referenceNovel = state.<String>value("referenceNovelId").orElse(null);
            String referenceNovelStyle = state.<String>value("referenceNovelStyle").orElse(null);

            // 实际调用AI服务生成大纲
            // CreateNovelAi createNovelAi = AiServices.builder(CreateNovelAi.class).chatModel(chatModel).build();
            // String outlineJson = createNovelAi.createNovelAndGetJson(novelType, referenceNovel, referenceNovelStyle);

            // 这里暂时返回示例大纲，实际应用中需要调用AI服务
            log.info("3.generateOutline 生成大纲完成 ");
            return Map.of("outlineGenerated", true, "outline", "小说大纲内容");
        });
    }

    // 确认大纲
    private AsyncNodeAction<ChapterFlowState> confirmOutlineNode() {
        return AsyncNodeAction.node_async(state -> {
            log.info("4.confirmOutline 开始确认大纲 ");
            // 这里应该等待用户确认，示意返回 true 表示确认
            // 实际应用中，这里可能需要异步等待用户操作
            boolean outlineConfirmed = true; // 实际应用中需要从数据库或缓存中获取用户确认状态
            log.info("4.confirmOutline 确认大纲完成 ");
            return Map.of("outlineConfirmed", outlineConfirmed);
        });
    }

    // 生成章节
    private AsyncNodeAction<ChapterFlowState> generateChapterNode() {
        return AsyncNodeAction.node_async(state -> {
            log.info("5.generateChapter 开始生成章节 ");
            // 获取当前章节号
            int currentChapterNo = state.<Integer>value("chapterNo").orElse(1);
            if (currentChapterNo != 1) {
                // 查询上章人物关系图谱，用于写作参考
                QueryGraphReq queryGraphReq = new QueryGraphReq();
                queryGraphReq.setNovelId(state.novelId().toString());
                queryGraphReq.setQueryType("character");
                queryGraphReq.setChapter(currentChapterNo - 1); // 查询前一章的人物关系
                GraphResponse graphResponse = graphRagClient.queryGraph(queryGraphReq);
                log.info("5.1 截止目前人物关系知识图谱为:{}", JSONUtil.toJsonStr(graphResponse));
                // 查询上章人物关系图谱，用于写作参考
                QueryGraphReq locationGraphReq = new QueryGraphReq();
                locationGraphReq.setNovelId(state.novelId().toString());
                locationGraphReq.setQueryType("location");
                locationGraphReq.setChapter(currentChapterNo - 1); // 查询到前一章的时间关系
                GraphResponse locationGraphResponse = graphRagClient.queryGraph(locationGraphReq);
                log.info("5.2 截止目前地点知识图谱为:{}", JSONUtil.toJsonStr(locationGraphResponse));
             // 查询上章人物关系图谱，用于写作参考
                QueryGraphReq timeLineGraphReq = new QueryGraphReq();
                timeLineGraphReq.setNovelId(state.novelId().toString());
                timeLineGraphReq.setQueryType("timeline");
                timeLineGraphReq.setChapter(currentChapterNo - 1); // 查询到前一章的时间关系
                GraphResponse timeLineGraphResponse = graphRagClient.queryGraph(timeLineGraphReq);
                log.info("5.3 截止目前时间线知识图谱为:{}", JSONUtil.toJsonStr(timeLineGraphResponse));
                DraftContent draftContent = contentGenerationAgent.generateDraft(state.novelId(), currentChapterNo, JSONUtil.toJsonStr(graphResponse.getResults()), JSONUtil.toJsonStr(locationGraphResponse.getResults()), JSONUtil.toJsonStr(timeLineGraphResponse.getResults()));
                ChapterDraft draft = chapterDraftService.createDraft(state.novelId(), currentChapterNo, draftContent, state.createdBy());

                // 提交审查
                chapterDraftService.submitForReview(draft.getId());
                chapterDraftService.markReviewing(draft.getId());

                log.info("5.generateChapter 生成章节完成 ");
                return Map.of("draftId", draft.getId(), "chapterNo", currentChapterNo, "draftContent", draftContent.getContent());
                //TODO 获取查询到的知识图谱信息
            }else {
                DraftContent draftContent = contentGenerationAgent.generateDraft(state.novelId(), currentChapterNo, StringUtils.EMPTY, StringUtils.EMPTY, StringUtils.EMPTY);
                ChapterDraft draft = chapterDraftService.createDraft(state.novelId(), currentChapterNo, draftContent, state.createdBy());

                // 提交审查
                chapterDraftService.submitForReview(draft.getId());
                chapterDraftService.markReviewing(draft.getId());

                log.info("5.generateChapter 生成章节完成 ");
                return Map.of("draftId", draft.getId(), "chapterNo", currentChapterNo, "draftContent", draftContent.getContent());
            }
        });
    }

    // GraphRAG 一致性检查
    private AsyncNodeAction<ChapterFlowState> checkConsistencyNode() {
        return AsyncNodeAction.node_async(state -> {
            log.info("6.checkConsistency 开始一致性检查 ");
            // 使用 GraphRAG 检查新章节的逻辑一致性
            CheckLogicReq checkLogicReq = new CheckLogicReq();
            checkLogicReq.setNovelId(state.novelId().toString());
            checkLogicReq.setChapter(state.chapterNo());
            // 从状态中获取章节内容
            checkLogicReq.setContent(state.draftContent());

            String response1 = graphRagClient.checkLogic(checkLogicReq);
            log.info("response:{}",response1);
            GraphResponse response = JSONUtil.toBean(response1,GraphResponse.class);
            boolean passed = !response.getHasErrors();

            log.info("6.checkConsistency 一致性检查完成 ");
            return Map.of("consistencyPassed", passed);
        });
    }

    // 质量审查
    private AsyncNodeAction<ChapterFlowState> reviewNode() {
        return AsyncNodeAction.node_async(state -> {
            log.info("7.review 开始质量审查 ");
            // 执行质量审查
                //ReviewResult reviewResult = qualityReviewAgent.reviewDraft(state.draftId());
            ReviewResult reviewResult = ReviewResult.builder()
                    .decision( ReviewDecision.APPROVED)
                    .scoreJson("{}")
                    .issuesJson("{}")
                    .evidenceJson("{}")
                    .reviewerVersion("reviewer-ai-json-v1")
                    .traceId(UUID.randomUUID().toString())
                    .build();
            ChapterReview review = chapterDraftService.recordReviewResult(state.draftId(), reviewResult);

            log.info("7.review 质量审查完成 ");
            return Map.of("reviewId", review.getId(), "decision", reviewResult.getDecision().name());
        });
    }

    // 用户确认章节
    private AsyncNodeAction<ChapterFlowState> confirmChapterNode() {
        return AsyncNodeAction.node_async(state -> {
            log.info("8.confirmChapter 开始确认章节 ");
            // 这里应该等待用户确认，示意返回 true 表示确认
            // 实际应用中，这里可能需要异步等待用户操作
            boolean chapterConfirmed = true; // 实际应用中需要从数据库或缓存中获取用户确认状态
            log.info("8.confirmChapter 确认章节完成 ");
            return Map.of("chapterConfirmed", chapterConfirmed);
        });
    }

    // 发布章节
    private AsyncNodeAction<ChapterFlowState> publishNode() {
        return AsyncNodeAction.node_async(state -> {
            log.info("9.publish 开始发布章节 ");
            // 发布章节
            int currentChapterNo = state.<Integer>value("chapterNo").orElse(1);
            log.info("当前发布章节号：{}，下一章章节号：{}",currentChapterNo,currentChapterNo+1);
            var publishResult = chapterDraftService.publishIfApproved(state.draftId());
            log.info("9.publish 发布章节完成 ");
            return Map.of("chapterId", publishResult.getChapterId(), "chapterVersion", publishResult.getChapterVersion(), "outboxEventId", publishResult.getOutboxEventId(),"chapterNo", currentChapterNo+1);
        });
    }

    // 更新知识图谱
    private AsyncNodeAction<ChapterFlowState> updateGraphNode() {
        return AsyncNodeAction.node_async(state -> {
            log.info("10.updateGraph 开始更新知识图谱 ");
            // 章节发布后，更新 GraphRAG 知识图谱

            // 从数据库获取发布的章节内容
            String chapterContent = state.draftContent(); // 从状态中获取刚生成的章节内容
            if (chapterContent == null || chapterContent.isEmpty()) {
                // 如果没有从状态中获取到内容，尝试从数据库获取
                if (state.draftId() != null) {
                    ChapterDraft draft = chapterDraftService.getById(state.draftId());
                    if (draft != null) {
                        chapterContent = draft.getDraftContent();
                    }
                }
            }

            if (chapterContent != null && !chapterContent.isEmpty()) {
                // 提取实体和关系并存入Neo4j
                ExtractGraphReq extractGraphReq = new ExtractGraphReq();
                extractGraphReq.setNovelId(state.novelId().toString());
                extractGraphReq.setChapter(state.chapterNo());
                extractGraphReq.setContent(chapterContent);

                // 执行知识图谱更新
                GraphResponse graphResponse = graphRagClient.extractGraph(extractGraphReq);
                log.info("知识图谱更新，结果为：{}",JSONUtil.toJsonStr(graphResponse));
                if (graphResponse != null && graphResponse.getSuccess()) {
                    log.info("10.updateGraph 更新知识图谱完成 ");
                    return Map.of("graphUpdated", true);
                } else {
                    log.info("10.updateGraph 更新知识图谱完成 ");
                    return Map.of("graphUpdated", false);
                }
            }

            log.info("10.updateGraph 更新知识图谱完成 ");
            return Map.of("graphUpdated", false);
        });
    }

    // 检查是否为卷末/书末
    private AsyncNodeAction<ChapterFlowState> checkCompletionNode() {
        return AsyncNodeAction.node_async(state -> {
            log.info("11.checkCompletion 开始检查是否为卷末/书末 ");
            // 检查当前章节是否为卷末或书末
            String completionStatus = "CONTINUE"; // 默认继续
            int currentChapter = state.chapterNo();

            // 实际逻辑判断是否为卷末或书末
            // 示例：如果当前章节是预设的卷末章节号，则返回 VOLUME_END
            if (currentChapter % 20 == 0) { // 假设每20章为一卷
                completionStatus = "VOLUME_END";
            } else if (currentChapter >= 200) { // 假设200章为全书完结
                completionStatus = "BOOK_END";
            }

            log.info("11.checkCompletion 检查是否为卷末/书末完成 ");
            return Map.of("completionStatus", completionStatus);
        });
    }

    // 生成章节总结
    private AsyncNodeAction<ChapterFlowState> summarizeNode() {
        return AsyncNodeAction.node_async(state -> {
            log.info("12.summarize 开始生成章节总结 ");
            // 生成章节总结，特别是卷末时生成该卷的总结
            // 获取需要总结的章节范围
            int currentChapter = state.chapterNo();
            Long novelId = state.novelId();

            // 获取小说信息
            Novel novel = novelService.getById(novelId);
            if (novel == null) {
                log.info("12.summarize 生成章节总结完成 ");
                return Map.of("summaryGenerated", false);
            }

            // 获取需要总结的章节内容
            int startChapter = 1; // 从第一章开始，或从上次总结之后开始

            // 查询上一次总结的结束章节
            String latestSummary = summaryService.getLatestSummary(novelId);
            if (latestSummary != null) {
                // 如果已有总结，可以从上次总结的章节号开始计算
                // 这里简化处理，直接总结到当前章节
                startChapter = currentChapter; // 或者可以根据已有逻辑计算起始章节
            }

            // 获取需要总结的章节内容
            List<Chapter> chaptersToSummarize = chapterService.getChaptersInRange(novelId, startChapter, currentChapter);
            if (chaptersToSummarize.isEmpty()) {
                log.info("12.summarize 生成章节总结完成 ");
                return Map.of("summaryGenerated", false);
            }

            // 将章节内容合并
            String chaptersContent = chaptersToSummarize.stream()
                    .sorted((c1, c2) -> Integer.compare(c1.getChapterNo(), c2.getChapterNo()))
                    .map(chapter -> "第" + chapter.getChapterNo() + "章:\n" + chapter.getContent())
                    .collect(Collectors.joining("\n\n"));

            // 调用AI服务生成总结
            SummaryAi summaryAi = AiServices.builder(SummaryAi.class).chatModel(chatModel).build();
            try {
                String summary = summaryAi.generateChapterSummary(
                        novel.getTitle(),
                        1, // 当前分卷号
                        latestSummary != null ? latestSummary : "", // 历史总结
                        startChapter - 1, // 上次总结结束章节
                        startChapter, // 起始章节
                        currentChapter, // 结束章节
                        chaptersContent, // 增量内容
                        "生成简洁的章节总结，突出核心情节点和人物状态变化"
                );

                // 存储总结到数据库
                Summary summaryEntity = summaryService.generateAndSaveSummary(novelId, startChapter, currentChapter, summary);

                log.info("12.summarize 生成章节总结完成 ");
                return Map.of("summaryGenerated", true);
            } catch (Exception e) {
                // 如果AI生成失败，仍然标记为已生成但记录错误
                log.error("Failed to generate summary for novel {} chapter {}", novelId, currentChapter, e);
                log.info("12.summarize 生成章节总结完成 ");
                return Map.of("summaryGenerated", false);
            }
        });
    }

    // 检查是否需要阶段性总结（如每10章）
    private AsyncNodeAction<ChapterFlowState> checkMilestoneNode() {
        return AsyncNodeAction.node_async(state -> {
            log.info("13.checkMilestone 开始检查是否需要阶段性总结 ");
            // 检查是否达到阶段性总结的要求（如每10章）
            boolean needsMilestoneSummary = (state.chapterNo() % 10 == 0);

            log.info("13.checkMilestone 检查是否需要阶段性总结完成 ");
            return Map.of("needsMilestoneSummary", needsMilestoneSummary);
        });
    }
}