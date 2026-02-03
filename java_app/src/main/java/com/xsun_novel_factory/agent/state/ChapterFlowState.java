package com.xsun_novel_factory.agent.state;

import com.xsun_novel_factory.model.dto.NovelDto;
import org.bsc.langgraph4j.state.AgentState;
import org.bsc.langgraph4j.state.Channel;
import org.bsc.langgraph4j.state.Channels;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * @author sunlinglei
 */
public class ChapterFlowState extends AgentState {

    // 基础流程状态
    private static final Map<String, Channel<?>> BASE_SCHEMA = Map.of(
            "novelId", Channels.base(() -> 1),
            "novel_info", Channels.base(NovelDto::new),
            "chapterNo", Channels.base(() -> 1),
            "createdBy", Channels.base(() -> "")
    );

    // 流程中间产物
    private static final Map<String, Channel<?>> INTERMEDIATE_SCHEMA = Map.of(
            "draftId", Channels.base(() -> ""),
            "reviewId", Channels.base(() -> ""),
            "decision", Channels.base(() -> "")
    );

    // 发布结果（可为 null）
    private static final Map<String, Channel<?>> PUBLISH_SCHEMA = Map.of(
            "chapterId", Channels.base(() -> ""),
            "chapterVersion", Channels.base(() -> ""),
            "outboxEventId", Channels.base(() -> "")
    );

    // 小说创作流程状态
    private static final Map<String, Channel<?>> FLOW_STATE_SCHEMA = Map.of(
            "outlineConfirmed", Channels.base(() -> false),
            "consistencyPassed", Channels.base(() -> false),
            "chapterConfirmed", Channels.base(() -> false),
            "completionStatus", Channels.base(() -> "CONTINUE"),
            "needsMilestoneSummary", Channels.base(() -> false)
    );

    // 参考资料和大纲
    private static final Map<String, Channel<?>> REFERENCE_SCHEMA = Map.of(
            "referenceMaterials", Channels.base(() -> ""),
            "outlineGenerated", Channels.base(() -> false),
            "outline", Channels.base(() -> "")
    );

    // 章节内容和上下文
    private static final Map<String, Channel<?>> CONTENT_SCHEMA = Map.of(
            "draftContent", Channels.base(() -> ""),
            "chapterContent", Channels.base(() -> ""),
            "characterRelations", Channels.base(() -> ""),
            "previousSummary", Channels.base(() -> ""),
            "recentChapters", Channels.base(() -> new ArrayList<>())
    );

    // 知识图谱相关
    private static final Map<String, Channel<?>> GRAPH_SCHEMA = Map.of(
            "graphUpdated", Channels.base(() -> ""),
            "summaryGenerated", Channels.base(() -> "")
    );

    // 日志相关
    private static final Map<String, Channel<?>> LOG_SCHEMA = Map.of(
            "logs", Channels.appender(ArrayList::new)
    );

    public static final Map<String, Channel<?>> SCHEMA = new HashMap<String, Channel<?>>() {{
        putAll(BASE_SCHEMA);
        putAll(INTERMEDIATE_SCHEMA);
        putAll(PUBLISH_SCHEMA);
        putAll(FLOW_STATE_SCHEMA);
        putAll(REFERENCE_SCHEMA);
        putAll(CONTENT_SCHEMA);
        putAll(GRAPH_SCHEMA);
        putAll(LOG_SCHEMA);
    }};

    public ChapterFlowState(Map<String, Object> initData) {
        super(initData);
    }

    public Long novelId() {
        return this.<Long>value("novelId").orElse(null);
    }

    public Object novel_info() {
        return this.<Object>value("novel_info").orElse(null);
    }

    public Integer chapterNo() {
        return this.<Integer>value("chapterNo").orElse(1);
    }

    public String createdBy() {
        return this.<String>value("createdBy").orElse("system");
    }

    public Long draftId() {
        return this.<Long>value("draftId").orElse(null);
    }

    public Long reviewId() {
        return this.<Long>value("reviewId").orElse(null);
    }

    public String decision() {
        return this.<String>value("decision").orElse(null);
    }

    public Long chapterId() {
        return this.<Long>value("chapterId").orElse(null);
    }

    public Integer chapterVersion() {
        return this.<Integer>value("chapterVersion").orElse(null);
    }

    public Long outboxEventId() {
        return this.<Long>value("outboxEventId").orElse(null);
    }

    public Boolean outlineConfirmed() {
        return this.<Boolean>value("outlineConfirmed").orElse(false);
    }

    public Boolean consistencyPassed() {
        return this.<Boolean>value("consistencyPassed").orElse(false);
    }

    public Boolean chapterConfirmed() {
        return this.<Boolean>value("chapterConfirmed").orElse(false);
    }

    public String completionStatus() {
        return this.<String>value("completionStatus").orElse("CONTINUE");
    }

    public Boolean needsMilestoneSummary() {
        return this.<Boolean>value("needsMilestoneSummary").orElse(false);
    }

    public Object referenceMaterials() {
        return this.<Object>value("referenceMaterials").orElse(null);
    }

    public Boolean outlineGenerated() {
        return this.<Boolean>value("outlineGenerated").orElse(false);
    }

    public String outline() {
        return this.<String>value("outline").orElse(null);
    }

    public String draftContent() {
        return this.<String>value("draftContent").orElse(null);
    }

    public String chapterContent() {
        return this.<String>value("chapterContent").orElse(null);
    }

    public Object characterRelations() {
        return this.<Object>value("characterRelations").orElse(null);
    }

    public String previousSummary() {
        return this.<String>value("previousSummary").orElse(null);
    }

    public Object recentChapters() {
        return this.<Object>value("recentChapters").orElse(new ArrayList<>());
    }

    public Boolean graphUpdated() {
        return this.<Boolean>value("graphUpdated").orElse(false);
    }

    public Boolean summaryGenerated() {
        return this.<Boolean>value("summaryGenerated").orElse(false);
    }
}