from __future__ import annotations

import base64
import json
import os
import tempfile
from contextlib import suppress
from pathlib import Path
from typing import Any

from fastapi import FastAPI, HTTPException, Request
from pydantic import BaseModel

SERVICE_ROOT = Path(__file__).resolve().parent
os.environ.setdefault("PADDLE_PDX_CACHE_HOME", str(SERVICE_ROOT / ".cache" / "paddlex"))
os.environ.setdefault("PADDLE_PDX_DISABLE_MODEL_SOURCE_CHECK", "True")

try:
    from paddleocr import PaddleOCR
except ImportError:  # pragma: no cover
    PaddleOCR = None


class RecognizeRequest(BaseModel):
    filename: str
    fileBase64: str


class PageResponse(BaseModel):
    pageNo: int
    text: str


class RecognizeResponse(BaseModel):
    engineName: str
    rawText: str
    pages: list[PageResponse]


app = FastAPI(title="recruit-agent-paddle-ocr")
_ocr: PaddleOCR | None = None


def get_ocr() -> PaddleOCR:
    global _ocr
    if PaddleOCR is None:
        raise RuntimeError("paddleocr is not installed")
    if _ocr is None:
        _ocr = PaddleOCR(
            use_doc_orientation_classify=False,
            use_doc_unwarping=False,
            use_textline_orientation=False,
        )
    return _ocr


@app.on_event("startup")
def warm_up_ocr() -> None:
    get_ocr()


def extract_page_text(page_result: Any) -> str:
    if hasattr(page_result, "json"):
        data = page_result.json
    elif isinstance(page_result, dict):
        data = page_result
    else:
        data = getattr(page_result, "res", None)

    if callable(data):
        data = data()

    if not isinstance(data, dict):
        return ""

    res = data.get("res", data)
    rec_texts = res.get("rec_texts")
    if isinstance(rec_texts, list):
        return "\n".join(str(text) for text in rec_texts if text)

    rec_text = res.get("rec_text")
    if isinstance(rec_text, str):
        return rec_text

    return ""


@app.get("/health")
def health() -> dict[str, str]:
    return {"status": "ok"}


async def read_recognize_payload(request: Request) -> dict[str, Any]:
    content_type = (request.headers.get("content-type") or "").lower()

    if "application/json" in content_type:
        try:
            payload = await request.json()
            if isinstance(payload, dict):
                return payload
        except Exception:  # noqa: BLE001
            pass

    if "application/x-www-form-urlencoded" in content_type or "multipart/form-data" in content_type:
        try:
            form = await request.form()
            payload = dict(form)
            if payload:
                return payload
        except Exception:  # noqa: BLE001
            pass

    raw_body = await request.body()
    if not raw_body:
        raise HTTPException(
            status_code=422,
            detail={
                "code": "EMPTY_BODY",
                "message": "request body is empty",
                "contentType": content_type,
            },
        )

    try:
        payload = json.loads(raw_body.decode("utf-8"))
    except Exception as exception:  # noqa: BLE001
        raise HTTPException(
            status_code=400,
            detail={
                "code": "INVALID_JSON",
                "message": "request body is not valid json",
                "contentType": content_type,
                "bodyPreview": raw_body[:128].decode("utf-8", errors="replace"),
            },
        ) from exception

    if not isinstance(payload, dict):
        raise HTTPException(status_code=422, detail="recognize payload must be a json object")

    return payload


@app.post("/api/ocr/recognize", response_model=RecognizeResponse)
async def recognize(request: Request) -> RecognizeResponse:
    payload = await read_recognize_payload(request)

    try:
        parsed = RecognizeRequest.model_validate(payload)
    except Exception as exception:  # noqa: BLE001
        raise HTTPException(status_code=422, detail=f"invalid recognize payload: {exception}") from exception

    try:
        file_bytes = base64.b64decode(parsed.fileBase64)
    except Exception as exception:  # noqa: BLE001
        raise HTTPException(status_code=400, detail="invalid base64 payload") from exception

    suffix = Path(parsed.filename).suffix or ".bin"
    temp_path = None
    try:
        with tempfile.NamedTemporaryFile(delete=False, suffix=suffix) as temp_file:
            temp_file.write(file_bytes)
            temp_path = Path(temp_file.name)

        ocr = get_ocr()
        prediction = ocr.predict(str(temp_path))

        pages: list[PageResponse] = []
        for index, page_result in enumerate(prediction, start=1):
            pages.append(PageResponse(pageNo=index, text=extract_page_text(page_result)))

        raw_text = "\n\n".join(page.text for page in pages if page.text)
        return RecognizeResponse(engineName="paddleocr", rawText=raw_text, pages=pages)
    except HTTPException:
        raise
    except Exception as exception:  # noqa: BLE001
        raise HTTPException(status_code=500, detail=f"ocr failed: {exception}") from exception
    finally:
        if temp_path is not None:
            with suppress(OSError):
                os.remove(temp_path)
