from typing import Dict, Any
from fastapi import FastAPI, HTTPException
from app.models import ExtractGraphReq, QueryGraphReq, CheckLogicReq
from app.neo4j_service import neo4j_service
from app.llm_service import llm_service
import logging

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

app = FastAPI(title="xsun novel factory agent")

# 初始化Neo4j索引
neo4j_service.create_indexes()


@app.on_event("shutdown")
def shutdown_event():
    neo4j_service.close()


# ... 保留原有的接口 ...

# ========== 新增知识图谱接口 ==========

@app.post("/graph/extract")
async def extract_graph(req: ExtractGraphReq) -> Dict[str, Any]:
    """
    接口1: 从章节内容中提取实体和关系并存入Neo4j
    """
    try:
        # 1. 使用LLM提取实体和关系
        logger.info(f"Extracting entities from novel {req.novelId}, chapter {req.chapter}")
        extraction_result = llm_service.extract_entities_and_relations(req.content, req.chapter)

        # 2. 存入Neo4j
        neo4j_service.add_entities_and_relations(
            novel_id=req.novelId,
            chapter=req.chapter,
            entities={
                "characters": extraction_result.get("characters", []),
                "locations": extraction_result.get("locations", []),
                "organizations": extraction_result.get("organizations", []),
                "events": extraction_result.get("events", [])
            },
            relations=extraction_result.get("relations", [])
        )

        return {
            "success": True,
            "novelId": req.novelId,
            "chapter": req.chapter,
            "extracted": {
                "characters_count": len(extraction_result.get("characters", [])),
                "locations_count": len(extraction_result.get("locations", [])),
                "organizations_count": len(extraction_result.get("organizations", [])),
                "events_count": len(extraction_result.get("events", [])),
                "relations_count": len(extraction_result.get("relations", []))
            },
            "details": extraction_result
        }

    except Exception as e:
        logger.error(f"Graph extraction failed: {e}")
        raise HTTPException(status_code=500, detail=str(e))


@app.post("/graph/query")
async def query_graph(req: QueryGraphReq) -> Dict[str, Any]:
    """
    接口2: 查询知识图谱信息
    支持查询类型：
    - character: 查询人物及其关系
    - location: 查询地点信息
    - organization: 查询组织信息
    - timeline: 查询章节时间线（按章节顺序展示事件）
    """
    try:
        if req.queryType == "character":
            results = neo4j_service.query_character_info(
                novel_id=req.novelId,
                character_name=req.entityName,
                chapter=req.chapter
            )
            return {
                "success": True,
                "queryType": "character",
                "results": results
            }

        elif req.queryType == "location":
            results = neo4j_service.query_location_info(
                novel_id=req.novelId,
                location_name=req.entityName
            )
            return {
                "success": True,
                "queryType": "location",
                "results": results
            }

        elif req.queryType == "organization":
            # 类似实现
            return {"success": True, "queryType": "organization", "results": []}

        elif req.queryType == "timeline":
            # 查询事件时间线
            with neo4j_service.driver.session() as session:
                result = session.run("""
                    MATCH (e:Event {novelId: $novelId})
                    WHERE $chapter IS NULL OR e.chapter <= $chapter
                    RETURN e.chapter as chapter, e.name as event, e.description as description
                    ORDER BY e.chapter
                """, novelId=req.novelId, chapter=req.chapter)

                timeline = [dict(record) for record in result]
                return {
                    "success": True,
                    "queryType": "timeline",
                    "results": timeline
                }

        else:
            raise HTTPException(status_code=400, detail=f"Unsupported queryType: {req.queryType}")

    except Exception as e:
        logger.error(f"Graph query failed: {e}")
        raise HTTPException(status_code=500, detail=str(e))


@app.post("/graph/check-logic")
async def check_logic(req: CheckLogicReq) -> Dict[str, Any]:
    """
    接口3: 检查新章节内容是否存在逻辑错误
    """
    try:
        # 1. 获取相关的知识图谱上下文
        characters = neo4j_service.query_character_info(
            novel_id=req.novelId,
            chapter=req.chapter - 1  # 获取之前章节的信息
        )

        graph_context = f"已知人物信息：\n"
        for char in characters[:10]:  # 限制数量
            graph_context += f"- {char['name']}: {char.get('description', '')}\n"
            for rel in char.get('relations', [])[:5]:
                graph_context += f"  └─ {rel['type']} {rel['target']} (自第{rel['since_chapter']}章)\n"

        # 2. 使用LLM检查逻辑错误
        errors = llm_service.check_logic_errors(req.content, req.chapter, graph_context)

        # 3. 基于图谱的矛盾检测（补充）
        # 这里可以添加更多基于规则的检测逻辑

        return {
            "success": True,
            "novelId": req.novelId,
            "chapter": req.chapter,
            "hasErrors": len(errors) > 0,
            "errorsCount": len(errors),
            "errors": errors
        }

    except Exception as e:
        logger.error(f"Logic check failed: {e}")
        raise HTTPException(status_code=500, detail=str(e))

# curl -X POST "http://localhost:8000/graph/extract" \
#   -H "Content-Type: application/json" \
#   -d '{
#     "novelId": "novel_001",
#     "chapter": 1,
#     "content": "张三是天剑宗的弟子，他与李四是好友。两人一起在青云山修炼..."
#   }'
