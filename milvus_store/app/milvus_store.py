from typing import Optional, List, Dict

from pymilvus import MilvusClient, DataType

from app.config import settings


class MilvusStore:
    def __init__(self, uri: str, collection: str, dim: int, token: str):
        print("正在尝试初始化 MilvusStore...")
        print("✓ 创建 MilvusClient,uri:",uri)
        self.uri = uri
        print("✓ 创建 MilvusClient,collection:",collection)
        self.collection = collection
        print("✓ 创建 MilvusClient,dim:",dim)
        print("✓ 创建 MilvusClient,token:",token)
        self.dim = dim

        self.client = MilvusClient(
            uri=uri,
            token=token,
        )

    def ensure_collection(self):
        if self.client.has_collection(self.collection):
            return

        schema = self.client.create_schema(
            auto_id=True,
            enable_dynamic_field=False,
        )
        schema.add_field(
            field_name="id",
            datatype=DataType.INT64,
            is_primary=True,
            auto_id=True
        )
        schema.add_field(
            field_name="work_id",
            datatype=DataType.VARCHAR,
            max_length=64
        )
        schema.add_field(
            field_name="work_title",
            datatype=DataType.VARCHAR,
            max_length=256
        )
        schema.add_field(field_name="doc_type", datatype=DataType.VARCHAR, max_length=32)
        schema.add_field(field_name="chunk_id", datatype=DataType.INT64)
        schema.add_field(field_name="chapter", datatype=DataType.VARCHAR, max_length=64)
        schema.add_field(field_name="tags", datatype=DataType.VARCHAR, max_length=512)
        schema.add_field(field_name="text", datatype=DataType.VARCHAR, max_length=65535)
        schema.add_field(field_name="embedding", datatype=DataType.FLOAT_VECTOR, dim=self.dim)

        index_params = self.client.prepare_index_params()

        # HNSW：通用
        index_params.add_index(
            field_name="embedding",
            index_type="HNSW",
            metric_type="L2",
            params={
                "M": 16,
                "efConstruction": 200
            }
        )

        self.client.create_collection(
            collection_name=self.collection,
            schema=schema,
            index_params=index_params
        )

    def _build_filter_expr(self,
                           work_id: Optional[str],
                           work_title: Optional[str],
                           doc_type: Optional[List[str]],
                           tags: Optional[List[str]],
                           chapter: Optional[str]) -> str:
        exprs = []
        if work_id:
            exprs.append(f"work_id == '{work_id}'")
        if work_title:
            exprs.append(f"work_title == '{work_title}'")
        if doc_type:
            types = ",".join([f"'{t}'" for t in doc_type])
            exprs.append(f"doc_type in [{types}]")
        if chapter:
            exprs.append(f"chapter == '{chapter}'")

        if tags:
            for tag in tags:
                exprs.append(f"tags LIKE '%{tag}%'")

        return " and ".join(exprs)

    def insert_chunks(self,
                      work_id: str,
                      work_title: str,
                      doc_type: str,
                      tags: str,
                      chapter: str,
                      chunks: List[str],
                      embeddings: List[List[float]],
                      chunk_id_start: int = 0) -> int:
        assert len(chunks) == len(embeddings)
        data = []

        for i, (text, emb) in enumerate(zip(chunks, embeddings)):
            data.append({
                "work_id": work_id,
                "work_title": work_title,
                "doc_type": doc_type,
                "tags": tags,
                "chapter": chapter,
                "chunk_id": chunk_id_start + i,
                "text": text,
                "embedding": emb
            })
        self.client.insert(collection_name=self.collection, data=data)
        return len(data)

    def search(self,
               query_vectors: List[List[float]],
               topk: int,
               work_id: Optional[str] = None,
               work_title: Optional[str] = None,
               doc_type: Optional[List[str]] = None,
               tags: Optional[List[str]] = None,
               chapter: Optional[str] = None) -> List[List[Dict[str, any]]]:
        expr = self._build_filter_expr(work_id, work_title, doc_type, tags, chapter)

        res = self.client.search(
            collection_name=self.collection,
            data=query_vectors,
            limit=topk,
            output_fields=["text", "work_title", "work_id", "doc_type", "chunk_id", "chapter", "tags"],
            filter=expr if expr else None,
            search_params={
                "metric_type": "L2",
                "params": {
                    "M": 16,
                    "efConstruction": 200
                }
            },
        )

        out: List[List[Dict[str, any]]] = []

        for hits in res:
            items = []
            for hit in hits:
                entity = hit.get("entity", {})
                items.append({
                    "text": entity.get("text", ""),
                    "score": float(hit.get("distance", 0.0)),
                    "meta": {
                        "workTitle": entity.get("work_title"),
                        "workId": entity.get("work_id"),
                        "docType": entity.get("doc_type"),
                        "chunkId": entity.get("chunk_id"),
                        "chapter": entity.get("chapter"),
                        "tags": entity.get("tags"),
                    }
                })
            out.append(items)
        return out


def build_store() -> MilvusStore:
    store = MilvusStore(uri=settings.milvus_uri,
                        token=settings.milvus_token,
                        collection=settings.milvus_collection,
                        dim=settings.vector_dim)
    store.ensure_collection()
    return store
