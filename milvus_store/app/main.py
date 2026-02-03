# 上传 检索
from typing import Dict, Any, List

from fastapi import FastAPI, UploadFile, Form, File, HTTPException

from app.chunker import chunk_text_stream
from app.config import settings
from app.embedder import Embedder
from app.milvus_store import build_store
from app.models import RetrieveReq

app = FastAPI(title="xsun novel factory agent")

store = build_store()
embedder = Embedder(provider="st", dim=settings.vector_dim, st_model_name=settings.st_model_name)

@app.get("/health")
def health() -> Dict[str, Any]:
    return {
        "ok": True,
        "milvus_uri": settings.milvus_uri,
        "collection": settings.milvus_collection,
        "dim": settings.vector_dim,
        "embedding_provider": settings.embedding_provider
    }

@app.post("/works/upload")
async def upload_work(
    file: UploadFile = File(...),
    workId: str = Form(...),
    workTitle: str = Form(...),
    docType: str = Form(...),      # story/technique/notes/world_setting
    tags: str = Form(""),
    chapter: str = Form("")
) -> Dict[str, Any]:
    if docType not in {"story", "technique", "notes", "world_setting"}:
        raise HTTPException(status_code=400, detail="docType must be one of story/technique/notes/world_setting")

    # 流式读取 UploadFile：逐行 decode
    # 注意：UploadFile.file 是 SpooledTemporaryFile，可用 read/iter
    # 这里用分块读取再 splitlines，尽量避免一次读入内存
    def iter_lines():
        import codecs
        decoder = codecs.getincrementaldecoder("utf-8")("ignore")
        while True:
            chunk = file.file.read(1024 * 1024)  # 1MB
            if not chunk:
                break
            text = decoder.decode(chunk)
            for line in text.splitlines():
                yield line
        # flush
        tail = decoder.decode(b"", final=True)
        if tail:
            for line in tail.splitlines():
                yield line

    chunk_iter = chunk_text_stream(
        iter_lines(),
        chunk_size_chars=settings.chunk_size_chars,
        overlap_chars=settings.chunk_overlap_chars
    )

    chunks_buffer: List[str] = []
    total_inserted = 0
    chunk_id_cursor = 0

    EMB_BATCH = settings.embed_batch_size

    for c in chunk_iter:
        if not c.strip():
            continue
        chunks_buffer.append(c)

        if len(chunks_buffer) >= EMB_BATCH:
            embs = embedder.embed(chunks_buffer)
            inserted = store.insert_chunks(
                work_id=workId,
                work_title=workTitle,
                doc_type=docType,
                tags=tags,
                chapter=chapter,
                chunks=chunks_buffer,
                embeddings=embs,
                chunk_id_start=chunk_id_cursor
            )
            total_inserted += inserted
            chunk_id_cursor += inserted
            chunks_buffer = []

    # flush remain
    if chunks_buffer:
        embs = embedder.embed(chunks_buffer)
        inserted = store.insert_chunks(
            work_id=workId,
            work_title=workTitle,
            doc_type=docType,
            tags=tags,
            chapter=chapter,
            chunks=chunks_buffer,
            embeddings=embs,
            chunk_id_start=chunk_id_cursor
        )
        total_inserted += inserted

    return {"workId": workId, "workTitle": workTitle, "docType": docType, "chunksInserted": total_inserted}

@app.post("/rag/retrieve")
def rag_retrieve(req: RetrieveReq) -> Dict[str, Any]:
    # 1) embed questions
    q_vecs = embedder.embed(req.questions)

    # 2) search with filter
    f = req.filter
    results = store.search(
        query_vectors=q_vecs,
        topk=req.topK,
        work_id=f.workId,
        work_title=f.workTitle,
        doc_type=f.docTypes,
        tags=f.tags,
        chapter=f.chapter
    )

    # 3) Map<String, List<Answer>>
    out: Dict[str, Any] = {}
    for q, ans in zip(req.questions, results):
        out[q] = ans
    return out