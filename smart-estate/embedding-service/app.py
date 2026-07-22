"""
Local multilingual embedding service for the Smart Estate assistant. Runs offline,
no external API. The Java backend (EmbeddingClient) calls POST /embed; when this
service is down, retrieval falls back to TF-IDF.

Model: intfloat/multilingual-e5-small, 384-dim, English and Chinese, ~470 MB.
e5 expects an input-type prefix ("query:" / "passage:"), applied here so the Java
side stays model-agnostic.
"""
from contextlib import asynccontextmanager
from typing import List, Literal

from fastapi import FastAPI
from pydantic import BaseModel
from sentence_transformers import SentenceTransformer

MODEL_NAME = "intfloat/multilingual-e5-small"
DIM = 384

_state = {}


@asynccontextmanager
async def lifespan(_: FastAPI):
    # Load once at startup; first run downloads the weights, later runs are cached.
    _state["model"] = SentenceTransformer(MODEL_NAME)
    yield
    _state.clear()


app = FastAPI(title="Smart Estate embedding service", lifespan=lifespan)


class EmbedRequest(BaseModel):
    texts: List[str]
    # e5 distinguishes the query side from the document side; the Java client says which.
    input_type: Literal["query", "passage"] = "passage"


class EmbedResponse(BaseModel):
    model: str
    dim: int
    vectors: List[List[float]]


@app.get("/health")
def health():
    return {"status": "ok" if "model" in _state else "loading", "model": MODEL_NAME, "dim": DIM}


@app.post("/embed", response_model=EmbedResponse)
def embed(req: EmbedRequest):
    prefix = "query: " if req.input_type == "query" else "passage: "
    inputs = [prefix + (t or "") for t in req.texts]
    # normalize so a plain dot product equals cosine similarity (matches pgvector's <=>).
    vecs = _state["model"].encode(
        inputs, normalize_embeddings=True, convert_to_numpy=True
    )
    return {"model": MODEL_NAME, "dim": DIM, "vectors": vecs.tolist()}
