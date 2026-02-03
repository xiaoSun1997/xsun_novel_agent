import os

from dotenv import load_dotenv
from pydantic.v1 import BaseSettings

load_dotenv()

class Settings(BaseSettings):
    # Neo4j
    neo4j_uri: str = os.getenv("NEO4J_URI", "bolt://xx.xxx.xxx.xxx:7687")
    neo4j_user: str = os.getenv("NEO4J_USER", "neo4j")
    neo4j_password: str = os.getenv("NEO4J_PASSWORD", "neo4j_password")

    # LLM
    llm_model: str = os.getenv("LLM_MODEL", "qwen-plus")
    llm_api_key: str = os.getenv("LLM_API_KEY", "sk-your-api-key")
    llm_base_url: str = os.getenv("LLM_BASE_URL", "https://dashscope.aliyuncs.com/compatible-mode/v1")
    temperature: str = os.getenv("TEMPERATURE", 0.7)