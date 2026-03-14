from __future__ import annotations

import os
from pathlib import Path
from typing import Any

from fastapi import FastAPI, HTTPException
from pydantic import BaseModel, Field

SERVICE_ROOT = Path(__file__).resolve().parent
os.environ.setdefault("HF_HOME", str(SERVICE_ROOT / ".cache" / "huggingface"))
os.environ.setdefault("TRANSFORMERS_CACHE", str(SERVICE_ROOT / ".cache" / "transformers"))
os.environ.setdefault("SENTENCE_TRANSFORMERS_HOME", str(SERVICE_ROOT / ".cache" / "sentence-transformers"))

try:
    from FlagEmbedding import BGEM3FlagModel
except ImportError:  # pragma: no cover
    BGEM3FlagModel = None


class EmbeddingRequest(BaseModel):
    texts: list[str] = Field(default_factory=list)
    normalize: bool = True


class EmbeddingResponse(BaseModel):
    model: str
    dimension: int
    embeddings: list[list[float]]


app = FastAPI(title="recruit-agent-bge-m3-embedding")
_model: Any = None


def get_model() -> Any:
    global _model
    if BGEM3FlagModel is None:
        raise RuntimeError("FlagEmbedding is not installed")
    if _model is None:
        use_fp16 = False
        _model = BGEM3FlagModel("BAAI/bge-m3", use_fp16=use_fp16)
    return _model


@app.on_event("startup")
def warm_up_model() -> None:
    get_model()


@app.get("/health")
def health() -> dict[str, str]:
    return {"status": "ok"}


@app.post("/api/embedding/embed", response_model=EmbeddingResponse)
def embed(request: EmbeddingRequest) -> EmbeddingResponse:
    if not request.texts:
        return EmbeddingResponse(model="BAAI/bge-m3", dimension=0, embeddings=[])

    texts = [text if text is not None else "" for text in request.texts]
    try:
        model = get_model()
        result = model.encode(
            texts,
            batch_size=min(len(texts), 8),
            max_length=2048,
            return_dense=True,
            return_sparse=False,
            return_colbert_vecs=False,
        )
        dense_vecs = result.get("dense_vecs")
        if dense_vecs is None:
            raise RuntimeError("dense vectors are missing from model output")

        embeddings = dense_vecs.tolist() if hasattr(dense_vecs, "tolist") else dense_vecs
        if request.normalize:
            embeddings = [normalize_vector(vector) for vector in embeddings]

        dimension = len(embeddings[0]) if embeddings else 0
        return EmbeddingResponse(model="BAAI/bge-m3", dimension=dimension, embeddings=embeddings)
    except HTTPException:
        raise
    except Exception as exception:  # noqa: BLE001
        raise HTTPException(status_code=500, detail=f"embedding failed: {exception}") from exception


def normalize_vector(vector: list[float]) -> list[float]:
    if not vector:
        return vector
    squared_sum = sum(value * value for value in vector)
    if squared_sum <= 0:
        return vector
    norm = squared_sum ** 0.5
    return [value / norm for value in vector]
