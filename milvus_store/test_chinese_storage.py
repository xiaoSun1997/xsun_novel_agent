import sys
sys.path.append('.')

from app.config import settings
from app.embedder import Embedder
from app.milvus_store import build_store

# 创建存储实例
store = build_store()

# 测试中文文本
test_text = ["这是测试中文文本", "小说内容应该正常显示", "人工智能技术"]

print("测试文本:")
for text in test_text:
    print(f"- {text} (长度: {len(text)})")

# 获取嵌入向量
embedder = Embedder(provider="st", dim=settings.vector_dim, st_model_name=settings.st_model_name)
embeddings = embedder.embed(test_text)

print(f"\n嵌入向量维度: {len(embeddings[0])}")

# 插入到Milvus
try:
    result = store.insert_chunks(
        work_id="test_work",
        work_title="测试作品",
        doc_type="story",
        tags="测试标签",
        chapter="第一章",
        chunks=test_text,
        embeddings=embeddings
    )
    print(f"插入成功，插入数量: {result}")
except Exception as e:
    print(f"插入失败: {e}")

# 从Milvus检索
try:
    search_embeddings = embedder.embed(["测试中文"])
    results = store.search(
        query_vectors=search_embeddings,
        topk=3,
        work_id="test_work"
    )
    
    print("\n检索结果:")
    for i, result_list in enumerate(results):
        print(f"查询 {i+1} 的结果:")
        for item in result_list:
            original_text = item["text"]
            print(f"  - {original_text}")
            print(f"    原始字符串 repr: {repr(original_text)}")
except Exception as e:
    print(f"检索失败: {e}")