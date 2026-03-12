# Embedding Service

This service provides a local HTTP adapter for `BGE-M3` and matches the Java-side embedding contract.

## API

- `GET /health`
- `POST /api/embedding/embed`

Request body:

```json
{
  "texts": ["Java Spring Boot", "Elasticsearch retrieval"],
  "normalize": true
}
```

Response body:

```json
{
  "model": "BAAI/bge-m3",
  "dimension": 1024,
  "embeddings": [
    [0.1, 0.2],
    [0.3, 0.4]
  ]
}
```

## Start

```powershell
cd python/embedding-service
.\install.ps1
.\start.ps1
```

## Notes

- Default port: `9002`
- Java-side config should use:
  - `app.embedding.provider=bge-m3`
  - `app.embedding.base-url=http://localhost:9002`
  - `app.embedding.embed-path=/api/embedding/embed`
