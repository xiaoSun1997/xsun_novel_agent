from typing import Optional, List, Dict, Any

from pydantic import BaseModel, Field


class RetrieveFilters(BaseModel):
    """
    Retrieve filters
    """
    workId: Optional[str] = None
    workTitle: Optional[str] = None
    docTypes: Optional[List[str]] = None
    tags: Optional[List[str]] = None
    chapter: Optional[str] = None


class RetrieveReq(BaseModel):
    questions: List[str] = Field(main_length=1)
    filter: RetrieveFilters = RetrieveFilters()
    topK: int = Field(default=8, ge=1, le=50)


class Answer(BaseModel):
    text: str
    score: float
    meta: Dict[str, Any]
