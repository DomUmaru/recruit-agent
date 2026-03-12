# Rerank Service

This service provides a local HTTP adapter for `bge-reranker-v2-m3` and matches the Java-side rerank contract.

## API

- `GET /health`
- `POST /api/rerank`

Request body:

```json
{
  "query": "Java 搜索工程师",
  "documents": [
    "候选人 A 简介",
    "候选人 B 简介"
  ]
}
```

Response body:

```json
{
  "model": "BAAI/bge-reranker-v2-m3",
  "scores": [0.91, 0.52]
}
```

## Start

```powershell
cd python/rerank-service
.\install.ps1
.\start.ps1
```

## Notes

- Default port: `9003`
- Java-side config should use:
  - `app.rerank.provider=bge-reranker-v2-m3`
  - `app.rerank.base-url=http://localhost:9003`
  - `app.rerank.rerank-path=/api/rerank`
