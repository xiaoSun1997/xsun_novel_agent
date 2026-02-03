#!/usr/bin/env milvus_store
"""
测试 Milvus 连接和配置
"""

import os
import sys

# 添加项目根目录到 Python 路径
sys.path.append(os.path.dirname(os.path.abspath(__file__)))

def test_milvus_store():
    """测试 MilvusStore 初始化"""
    try:
        print("正在导入 MilvusStore...")
        from app.milvus_store import MilvusStore
        print("✓ 成功导入 MilvusStore")
        
        print("正在导入配置...")
        from app.config import settings
        print(f"✓ 成功导入配置，MILVUS_URI: {settings.milvus_uri}")
        
        print("正在尝试初始化 MilvusStore...")
        store = MilvusStore(
            uri=settings.milvus_uri,
            token=settings.milvus_token,
            collection=settings.milvus_collection,
            dim=settings.vector_dim
        )
        print("✓ 成功初始化 MilvusStore")


        print("正在尝试确保集合存在...")
        store.ensure_collection()
        print("✓ 成功确保集合存在")
        
        print("\n所有测试通过！Milvus 应该可以正常工作。")
        return True
        
    except Exception as e:
        print(f"✗ 测试失败: {str(e)}")
        import traceback
        traceback.print_exc()
        return False

if __name__ == "__main__":
    test_milvus_store()