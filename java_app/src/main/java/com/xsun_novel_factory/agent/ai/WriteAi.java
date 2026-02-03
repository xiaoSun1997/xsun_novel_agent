package com.xsun_novel_factory.agent.ai;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * AI Service：章节写作
 * <p>
 * - prompt规范化
 * - 未来想做多模型、多温度、多参数，只要在 builder 或方法参数里控制
 */
public interface WriteAi {

    @SystemMessage("""
            你是一个长篇小说写作引擎。
            目标：写出连贯、具象、节奏明确的章节正文。
            约束：
            - 必须严格遵循【章节目标/大纲】；
            - 保持【风格参考片段】的叙述节奏与语言质感，但不要抄袭原句；
            - 注意人物性格一致、事件因果一致；
            - 输出只包含“正文”，不要输出解释。
            """)
    @UserMessage("""
            【作品ID】{{novelId}}
            【章节序号】{{chapterNo}}
            
            【章节目标/大纲】
            {{outline}}
            
            【本卷细纲】
            {{detailLine}}
            
            【最近5章章节内容为（可能为空）】
            {{beforeChapterInfo}}
            
            【前文总结】
            【1.基于人物关系的知识图谱为(可能为空)】
            {{roleGraphResponse}}
            【2.基于地点关系的知识图谱为(可能为空)】
            {{locationGraphResponse}}
            【3.基于时间关系的知识图谱为(可能为空)】
            {{timeLineGraphResponse}}
            
            现在开始写本章节正文（建议 2000~3500 字，按你的内容自然收束）。
            """)
    String writeChapter(@V("novelId") Long novelId,
                        @V("chapterNo") Integer chapterNo,
                        @V("outline") String outline,
                        @V("detailLine") String detailLine,
                        @V("beforeChapterInfo") String beforeChapterInfo,
                        @V("roleGraphResponse") String roleGraphResponse,
                        @V("locationGraphResponse") String locationGraphResponse,
                        @V("timeLineGraphResponse") String timeLineGraphResponse);


    @SystemMessage("""
            # Role
            你是一位资深网文作者，擅长根据已有大纲和章节总结，创作连贯且引人入胜的小说章节。
            
            # Goal
            基于提供的小说大纲、历史总结和当前状态，生成指定章节的内容。
            
            # Constraints & Requirements
            1. **内容连贯性**：必须严格遵循已有剧情发展，保持人物性格、世界观的一致性。
            2. **伏笔处理**：必须处理历史总结中提到的所有未解伏笔，要么推进、要么保持悬念。
            3. **节奏控制**：章节需包含：
               - 至少一个情节推进
               - 人物关系或状态的变化
               - 适当的悬念或钩子（为下一章铺垫）
            4. **风格匹配**：保持与已有章节相同的叙事风格和语言特点。
            5. **章节结构**：每章约3000-5000字，包含：
               - 开场（承接上章）
               - 发展（情节推进）
               - 高潮（本章核心冲突）
               - 结尾（留下悬念）
            
            # Output Format
            请严格按照以下JSON格式输出：
            {
              "chapterNo": "章节编号",
              "chapterTitle": "章节标题",
              "content": "完整的章节内容（纯文本，包含对话、描写、心理活动等）",
              "summary": "本章节简要总结（用于后续总结生成）",
              "plotHooks": ["本章引入的新伏笔1", "本章引入的新伏笔2"],
              "characterStatusUpdates": {
                "角色名1": "状态变化描述",
                "角色名2": "状态变化描述"
              }
            }
            """)
    @UserMessage("""
            【小说基本信息】
            标题：{{title}}
            当前分卷：第{{currentVolume}}卷
            
            【历史剧情总结】
            {{previousSummary}}
            
            【当前故事状态（JSON）】
            {{currentStoryState}}
            
            【待处理伏笔】
            {{pendingPlotHooks}}
            
            【当前章节信息】
            需要生成：第{{targetChapterNo}}章
            章节目标：{{chapterGoal}}
            风格参考：{{styleReference}}
            
            请生成第{{targetChapterNo}}章内容，确保：
            1. 承接第{{previousChapterNo}}章的结尾
            2. 推进{{chapterGoal}}中描述的目标
            3. 处理至少一个待处理伏笔
            4. 为下一章留下合理悬念
            
            现在输出 JSON：
            {
              "chapterNo": "{{targetChapterNo}}",
              "chapterTitle": "你的章节标题",
              "content": "完整的章节内容",
              "summary": "本章节简要总结",
              "plotHooks": ["新伏笔1", "新伏笔2"],
              "characterStatusUpdates": {
                "角色名1": "状态变化",
                "角色名2": "状态变化"
              }
            }
            """)
    String generateChapter(
            @V("title") String title,
            @V("currentVolume") int currentVolume,
            @V("previousSummary") String previousSummary,
            @V("currentStoryState") String currentStoryState,
            @V("pendingPlotHooks") String pendingPlotHooks,
            @V("targetChapterNo") int targetChapterNo,
            @V("chapterGoal") String chapterGoal,
            @V("styleReference") String styleReference,
            @V("previousChapterNo") int previousChapterNo
    );
}
