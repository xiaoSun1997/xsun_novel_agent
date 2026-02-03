import json
from typing import Dict, List, Any
from openai import OpenAI
import logging
from app.config import Settings

logger = logging.getLogger(__name__)


class LLMService:
    def __init__(self):
        self.settings = Settings()

        if self.settings.llm_model:  # 修正：这里假设都是用llm_model判断，可能需要根据实际情况调整
            self.client = OpenAI(
                api_key=self.settings.llm_api_key,
                base_url="https://dashscope.aliyuncs.com/compatible-mode/v1"
            )
            self.model = "qwen-plus"
        else:
            raise ValueError(f"Unsupported LLM provider: {self.settings.llm_model}")

    def extract_entities_and_relations(self, content: str, chapter: int) -> Dict[str, Any]:
        """从文本中提取实体和关系"""
        prompt = f"""
你是一个专业的小说内容分析助手。请从以下第{chapter}章的小说内容中提取人物、地点、组织、事件及其关系。

小说内容：
{content[:3000]}  # 限制长度避免token超限

请以JSON格式返回，包含：
1. characters: 人物列表，每个包含 name, aliases（别名列表）, description
2. locations: 地点列表，每个包含 name, type, description
3. organizations: 组织列表，每个包含 name, type, description
4. events: 事件列表，每个包含 name, description, timestamp（章节内时间）
5. relations: 关系列表，每个包含 source（源实体名）, target（目标实体名）, type（关系类型如KNOWS/LOVES/HATES/BELONGS_TO/LOCATED_IN/PARTICIPATED_IN）, properties（其他属性，如intensity强度）

注意：
- 只提取明确出现的实体和关系
- 关系类型使用英文大写
- description用中文简洁描述

返回格式示例：
{{
  "characters": [{{"name": "张三", "aliases": ["小三"], "description": "主角，修仙者"}}],
  "locations": [{{"name": "青云山", "type": "mountain", "description": "主角修炼地"}}],
  "organizations": [{{"name": "天剑宗", "type": "sect", "description": "主角所属门派"}}],
  "events": [{{"name": "突破筑基", "description": "主角成功筑基", "timestamp": "章节开始"}}],
  "relations": [{{"source": "张三", "target": "天剑宗", "type": "BELONGS_TO", "properties": {{"role": "弟子"}}}}]
}}
"""

        try:
            response = self.client.chat.completions.create(
                model=self.model,
                messages=[
                    {"role": "system", "content": "你是一个专业的小说内容分析助手，擅长提取实体和关系。"},
                    {"role": "user", "content": prompt}
                ],
                temperature=0.3,
                response_format={"type": "json_object"}
            )

            result = json.loads(response.choices[0].message.content)
            logger.info(f"Extracted {len(result.get('characters', []))} characters, "
                        f"{len(result.get('relations', []))} relations")
            return result

        except Exception as e:
            logger.error(f"LLM extraction failed: {e}")
            return {"characters": [], "locations": [], "organizations": [], "events": [], "relations": []}

    def check_logic_errors(self, content: str, chapter: int, graph_context: str) -> List[Dict[str, Any]]:
        """检查内容的逻辑错误"""
        prompt = f"""
你是一个专业的小说逻辑检查助手。请根据已有的知识图谱信息，检查第{chapter}章的新内容是否存在逻辑错误。

已有知识图谱信息：
{graph_context[:2000]}

新章节内容：
{content[:3000]}

请检查以下类型的逻辑错误：
1. 人物矛盾：人物在之前章节已死亡但又出现
2. 关系矛盾：人物关系前后矛盾（如之前是敌人现在突然是朋友但没有解释）
3. 地点矛盾：人物同时出现在不同地点
4. 能力矛盾：人物能力突然降低或消失
5. 时间矛盾：事件时间线混乱

以JSON格式返回错误列表，每个错误包含：
- type: 错误类型
- description: 错误描述
- severity: 严重程度（high/medium/low）
- suggestion: 修改建议

如果没有错误，返回空列表。

返回格式：
{{
  "errors": [
    {{"type": "character_contradiction", "description": "张三在第5章已死亡，但在本章又出现", "severity": "high", "suggestion": "检查是否为回忆场景或需要复活设定"}}
  ]
}}
"""

        try:
            response = self.client.chat.completions.create(
                model=self.model,
                messages=[
                    {"role": "system", "content": "你是一个专业的小说逻辑检查助手。"},
                    {"role": "user", "content": prompt}
                ],
                temperature=0.2,
                response_format={"type": "json_object"}
            )

            result = json.loads(response.choices[0].message.content)
            return result.get("errors", [])

        except Exception as e:
            logger.error(f"LLM logic check failed: {e}")
            return []


llm_service = LLMService()
