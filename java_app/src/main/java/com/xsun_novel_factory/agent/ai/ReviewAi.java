package com.xsun_novel_factory.agent.ai;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * AI Service：章节质量审查
 *
 * 输出要求：必须是 JSON（便于直接写入 chapter_review 表）
 * decision: APPROVED/REJECTED
 * score_json: { coherence:0-1, style:0-1, consistency:0-1, ... }
 * issues_json: [{type,severity,quote,suggestion}, ...]
 * evidence_json: {chroma_hits:[...], notes:"..."}
 */
public interface ReviewAi {

    @SystemMessage("""
            你是小说质量审查器，负责发现以下问题：
            - 连贯性（叙事是否断裂、逻辑跳跃）
            - 风格一致性（是否偏离参考风格）
            - 世界观/人物一致性（人物性格、关系、设定是否自相矛盾）
            - 与前文的衔接性（是否与前文内容、人物关系、世界观设定一致）
            - 逻辑一致性（是否与知识图谱中的信息一致）
            
            你必须只输出 JSON，不要输出任何额外文本。
            JSON 格式如下：
            {
              "decision": "APPROVED|REJECTED",
              "score_json": {
                "coherence": 0.0-1.0,
                "style_consistency": 0.0-1.0,
                "character_consistency": 0.0-1.0,
                "worldview_consistency": 0.0-1.0,
                "continuity": 0.0-1.0,
                "overall": 0.0-1.0
              },
              "issues_json": [
                {
                  "type": "COHERENCE|STYLE|CHARACTER|WORLDVIEW|CONTINUITY|LOGIC",
                  "severity": "HIGH|MEDIUM|LOW",
                  "quote": "具体的问题文本片段",
                  "suggestion": "修改建议"
                }
              ],
              "evidence_json": {
                "chroma_hits": [],
                "graph_rag_findings": [],
                "notes": "审查备注信息"
              }
            }
            """)
    @UserMessage("""
            【作品ID】{{novelId}}
            【章节草稿内容】
            {{draftContent}}
            【前文章节细纲总结(可以为空)】
            {{beforeSummary}}
            【细纲到当前章节之间的内容为（可能为空）】
            {{beforeChapterInfo}}
            【人物关系图谱信息】
            {{characterRelations}}
            【Milvus参考信息】
            {{referenceInfo}}
            现在输出 JSON：
            """)
    String reviewAsJson(@V("novelId") Long novelId,
                        @V("draftContent") String draftContent,
                        @V("beforeSummary") String beforeSummary,
                        @V("beforeChapterInfo") String beforeChapterInfo,
                        @V("characterRelations") String characterRelations,
                        @V("referenceInfo") String referenceInfo);
}