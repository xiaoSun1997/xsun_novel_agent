from pydantic import BaseModel, Field
from typing import List, Optional, Dict, Any

# ... 保留原有的模型 ...

class ExtractGraphReq(BaseModel):
    novelId: str = Field(..., description="小说ID")
    chapter: int = Field(..., description="章节号")
    content: str = Field(..., description="章节正文内容")

class QueryGraphReq(BaseModel):
    novelId: str = Field(..., description="小说ID")
    queryType: str = Field(..., description="查询类型: character/location/organization/timeline")
    entityName: Optional[str] = Field(None, description="实体名称（可选，不填返回全部）")
    chapter: Optional[int] = Field(None, description="限制章节范围（可选）")

class CheckLogicReq(BaseModel):
    novelId: str = Field(..., description="小说ID")
    chapter: int = Field(..., description="当前章节号")
    content: str = Field(..., description="待检查的正文内容")
