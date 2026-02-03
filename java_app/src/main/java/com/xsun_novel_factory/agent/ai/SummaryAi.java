package com.xsun_novel_factory.agent.ai;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

public interface SummaryAi {
    @SystemMessage("""
        # Role
        你是专业的网文编辑和故事分析师，擅长从连续章节中提取核心信息并生成结构化总结。
        
        # Goal
        基于增量章节内容和历史总结，生成用于后续章节创作的详细状态总结。
        
        # Constraints & Requirements
        1. **信息密度**：总结必须包含所有关键信息，但避免冗余。
        2. **结构清晰**：按以下模块组织信息：
           - 核心情节推进（时间线顺序）
           - 人物状态更新（所有人物的变化）
           - 伏笔跟踪（新出现+历史伏笔状态）
           - 连贯性提醒（容易被忽略的细节）
           - 故事状态快照（结构化数据）
        3. **实用导向**：总结需便于AI理解并用于生成后续章节。
        4. **准确性**：必须准确反映增量章节的所有重要内容。
        
        # Output Format
        请严格按照以下JSON格式输出：
        {
          "summaryRange": "起始章-结束章",
          "narrativeSummary": "叙事性总结（3-5段）",
          "corePlotPoints": [
            {
              "chapter": "章节号",
              "event": "事件描述",
              "significance": "重要性（高/中/低）"
            }
          ],
          "characterStates": [
            {
              "name": "角色名",
              "currentStatus": "当前状态（情绪/健康/位置等）",
              "goals": "当前目标",
              "relationshipsChanged": ["与XX的关系变为YY"]
            }
          ],
          "plotHooksTracking": {
            "newHooks": [
              {
                "hook": "伏笔描述",
                "introducedIn": "引入章节",
                "currentStatus": "未触发/已揭示/进行中"
              }
            ],
            "updatedHooks": [
              {
                "hook": "历史伏笔",
                "previousStatus": "之前状态",
                "currentStatus": "当前状态",
                "progress": "进展描述"
              }
            ]
          },
          "continuityNotes": [
            "需要注意的细节1",
            "需要注意的细节2"
          ],
          "storyStateSnapshot": {
            "currentArc": "当前故事弧",
            "activeConflicts": [
              {
                "type": "冲突类型",
                "description": "冲突描述",
                "partiesInvolved": ["参与方"],
                "intensity": "强度（1-10）"
              }
            ],
            "timelineAnchor": "时间锚点（如：事件发生后第X天）",
            "keyLocations": ["当前重要地点"],
            "narrativeTone": "当前叙事语气"
          }
        }
        """)
    @UserMessage("""
        【基础信息】
        小说标题：{{title}}
        当前分卷：第{{currentVolume}}卷
        
        【历史总结】
        {{lastSummary}}
        总结截止章节：第{{lastChapterEnd}}章
        
        【增量章节内容】
        从第{{startChapter}}章到第{{endChapter}}章：
        {{incrementalContent}}
        
        【额外要求】
        {{specialInstructions}}
        
        请结合历史总结生成从第1章到第{{endChapter}}章的总结。
        重点关注：
        1. 第{{lastChapterEnd}}章之后的新发展
        2. 所有人物状态的变化
        3. 新增和更新的伏笔
        4. 为下一章创作提供必要的上下文
        
        现在输出 JSON：
        {
          "summaryRange": "{{lastChapterEnd}}-{{endChapter}}",
          "narrativeSummary": "你的叙事总结",
          "corePlotPoints": [...],
          "characterStates": [...],
          "plotHooksTracking": {...},
          "continuityNotes": [...],
          "storyStateSnapshot": {...}
        }
        """)
    String generateChapterSummary(
            @V("title") String title,
            @V("currentVolume") int currentVolume,
            @V("lastSummary") String lastSummary,
            @V("lastChapterEnd") int lastChapterEnd,
            @V("startChapter") int startChapter,
            @V("endChapter") int endChapter,
            @V("incrementalContent") String incrementalContent,
            @V("specialInstructions") String specialInstructions
    );
}
