from __future__ import annotations

import os
from pathlib import Path

from fastapi import FastAPI, HTTPException
from pydantic import BaseModel, Field

SERVICE_ROOT = Path(__file__).resolve().parent
os.environ.setdefault("HF_HOME", str(SERVICE_ROOT / ".cache" / "huggingface"))
os.environ.setdefault("TRANSFORMERS_CACHE", str(SERVICE_ROOT / ".cache" / "transformers"))
os.environ.setdefault("SENTENCE_TRANSFORMERS_HOME", str(SERVICE_ROOT / ".cache" / "sentence-transformers"))
os.environ.setdefault("HF_HUB_DISABLE_SYMLINKS_WARNING", "1")

try:
    from FlagEmbedding import FlagReranker
except ImportError:  # pragma: no cover
    FlagReranker = None


class RerankRequest(BaseModel):
    query: str = ""
    documents: list[str] = Field(default_factory=list)


class RerankResponse(BaseModel):
    model: str
    scores: list[float]


app = FastAPI(title="recruit-agent-bge-reranker")
_model = None


def get_model():
    global _model
    if FlagReranker is None:
        raise RuntimeError("FlagEmbedding is not installed")
    if _model is None:
        _model = FlagReranker("BAAI/bge-reranker-v2-m3", use_fp16=False)
    return _model

@app.get("/health")
def health() -> dict[str, str]:
    return {"status": "ok"}


@app.post("/api/rerank", response_model=RerankResponse)
def rerank(request: RerankRequest) -> RerankResponse:
    if not request.documents:
        return RerankResponse(model="BAAI/bge-reranker-v2-m3", scores=[])

    try:
        model = get_model()
        pairs = [[request.query or "", document or ""] for document in request.documents]
        scores = model.compute_score(pairs, normalize=True)
        if not isinstance(scores, list):
            scores = list(scores)
        scores = [float(score) for score in scores]
        return RerankResponse(model="BAAI/bge-reranker-v2-m3", scores=scores)
    except HTTPException:
        raise
    except Exception as exception:  # noqa: BLE001
        raise HTTPException(status_code=500, detail=f"rerank failed: {exception}") from exception
