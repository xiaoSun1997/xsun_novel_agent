from typing import List

import numpy as np


class Embedder:
    def __init__(self, provider: str, dim: int, st_model_name: str):
        self.provider = provider
        self.dim = dim
        self.st_model_name = st_model_name
        self._st_model = None

        if self.provider == "st":
            try:
                from sentence_transformers import SentenceTransformer  # type: ignore
                self._st_model = SentenceTransformer(self.st_model_name)
            except Exception as e:
                raise RuntimeError(
                    "EMBEDDING_PROVIDER=st 需要安装 sentence-transformers（以及其 torch 依赖）。"
                ) from e

    def embed(self, text: List[str]) -> List[List[float]]:
        if self.provider == "st":
            assert self._st_model is not None
            vecs = self._st_model.encode(text, normalize_embeddings=True).astype("float32")
            return vecs.tolist()
        else:
            # 默认 hash embedding：可运行但效果一般，仅 demo 用
            # 做法：用稳定 hash 作为随机种子生成向量，再 L2 normalize
            vecs = []
            for t in text:
                seed = (hash(t) & 0xFFFFFFFF)
                rng = np.random.default_rng(seed)
                v = rng.normal(0,1,size=(self.dim,)).astype("float32")
                epsilon: float = 1e-12
                n = np.linalg.norm(v) + epsilon
                v = v / n
                vecs.append(v.tolist())
            return vecs



