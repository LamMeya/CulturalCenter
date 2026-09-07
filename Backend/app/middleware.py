"""
统一响应格式 Middleware：
- 对 /api/* 下所有正常（2xx）JSON 返回自动包成 { code:0, message:"ok", data }
- 匹配 Android 端 ApiResponse<T>：{ code, message, data }
- HTTPException（4xx / 5xx）不包，FastAPI 默认返回 { "detail": ... }
  —— 客户端 safeApiCall 的 parseErrorMessage 已经处理 {"detail": ...} 格式
- /api/health（健康检查原始格式）、/admin/*（管理后台 HTML）、/static/*、
  /docs、/redoc、/openapi.json（OpenAPI）均不包装
"""
import json
import time
from starlette.middleware.base import BaseHTTPMiddleware, RequestResponseEndpoint
from starlette.requests import Request
from starlette.responses import Response, JSONResponse


# 不包装的路径前缀 / 精确路径（如果命中，原样返回）
_SKIP_PREFIXES = (
    "/admin",
    "/static",
    "/docs",
    "/redoc",
    "/openapi.json",
    "/favicon.ico",
)
_SKIP_EXACT = (
    "/api/health",
)

# 只对 /api 前缀做处理
_API_PREFIX = "/api/"


def _should_wrap(path: str, content_type: str | None) -> bool:
    if path in _SKIP_EXACT:
        return False
    for p in _SKIP_PREFIXES:
        if path.startswith(p):
            return False
    if not path.startswith(_API_PREFIX) and path != "/api":
        return False
    if not content_type:
        return False
    return "application/json" in content_type.lower()


async def _read_body(response: Response) -> bytes:
    """安全读取 response.body_iterator（兼容 Starlette 0.x 不同版本）"""
    chunks: list[bytes] = []
    # FastAPI 的 JSONResponse 有时会在 Response.body 缓存原始 bytes；没有就读迭代器
    cached = getattr(response, "body", None)
    if isinstance(cached, (bytes, bytearray)) and cached:
        return bytes(cached)
    try:
        async for chunk in response.body_iterator:
            if isinstance(chunk, str):
                chunk = chunk.encode("utf-8")
            chunks.append(chunk)
    except Exception:
        # 如果异步迭代读失败，尝试 getattr 兜底
        cached = getattr(response, "body", None)
        if isinstance(cached, (bytes, bytearray)):
            return bytes(cached)
        return b""
    return b"".join(chunks)


class UnifiedResponseMiddleware(BaseHTTPMiddleware):
    async def dispatch(self, request: Request, call_next: RequestResponseEndpoint) -> Response:
        start = time.perf_counter()
        response = await call_next(request)

        path = request.url.path
        ct = response.headers.get("content-type", "")

        # 非 2xx / 不符合条件 → 原样
        if response.status_code >= 300 or not _should_wrap(path, ct):
            return response

        # 读 body
        body_bytes = await _read_body(response)
        if not body_bytes:
            return response

        try:
            payload = json.loads(body_bytes.decode("utf-8"))
        except (ValueError, UnicodeDecodeError):
            # 不是合法 JSON，不包装
            return response

        # 已经包过（比如手动 _ok）→ 不重复包裹，但**仍用读出来的 body_bytes 重建 JSONResponse**
        # （因为 body_iterator 已经被 _read_body 消费完了，原 response 再发会是空 body / IncompleteRead）
        already_wrapped = (
            isinstance(payload, dict)
            and "code" in payload
            and "message" in payload
            and "data" in payload
        )
        if already_wrapped:
            headers = {k: v for k, v in response.headers.items() if k.lower() != "content-length"}
            headers["X-Response-Time-Ms"] = f"{int((time.perf_counter() - start) * 1000)}"
            return JSONResponse(
                content=payload,
                status_code=response.status_code,
                headers=headers,
                media_type="application/json",
            )

        wrapped = {"code": 0, "message": "ok", "data": payload}
        headers = {k: v for k, v in response.headers.items() if k.lower() != "content-length"}
        headers["X-Response-Time-Ms"] = f"{int((time.perf_counter() - start) * 1000)}"
        return JSONResponse(
            content=wrapped,
            status_code=response.status_code,
            headers=headers,
            media_type="application/json",
        )
