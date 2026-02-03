import os

from pydantic import BaseModel


class Settings(BaseModel):
    //此处使用ziliz 云平台
    milvus_uri: str = os.getenv("MILVUS_URI", "https://xxxxxxxali-cn-hangzhou.cloud.zilliz.com.cn")
    milvus_token: str = os.getenv("MILVUS_TOKEN", "xxxx")
    milvus_collection: str = os.getenv("MILVUS_COLLECTION", "novel_chunks")
    vector_dim: int = int(os.getenv("VECTOR_DIM", 768))

    embedding_provider: str = os.getenv("EMBEDDING_PROVIDER", "hash")
    st_model_name: str = os.getenv("ST_MODEL_NAME", "shibing624/text2vec-base-chinese")

    chunk_size_chars: int = int(os.getenv("CHUNK_SIZE_CHARS", 800))
    chunk_overlap_chars: int = int(os.getenv("CHUNK_OVERLAP_CHARS", 120))

    embed_batch_size: int = int(os.getenv("EMBED_BATCH_SIZE", 64))
    insert_batch_size: int = int(os.getenv("INSERT_BATCH_SIZE", 64))


settings = Settings()