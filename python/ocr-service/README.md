# OCR Service

This service provides a local HTTP adapter for PaddleOCR and matches the Java-side OCR contract.

## API

- `GET /health`
- `POST /api/ocr/recognize`

Request body:

```json
{
  "filename": "resume.pdf",
  "fileBase64": "..."
}
```

Response body:

```json
{
  "engineName": "paddleocr",
  "rawText": "...",
  "pages": [
    { "pageNo": 1, "text": "..." }
  ]
}
```

## Start

```powershell
cd python/ocr-service
.\install.ps1
.\start.ps1
```

## Notes

- Default port: `9001`
- Java-side config should use:
  - `app.ocr.provider=paddle`
  - `app.ocr.base-url=http://localhost:9001`
