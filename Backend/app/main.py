from pathlib import Path
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from fastapi.staticfiles import StaticFiles
from app.database import engine, Base
from app.routes import auth, admin, venues, bookings, teams, users, notifications, system
from app.routes import admin_ui
from app.services.init_service import init_super_admin, init_test_user

app = FastAPI(title="斗门文化中心场地预约系统", version="1.0.0")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# 用绝对路径，避免 uvicorn --reload 子进程 cwd 变化导致找不到
_static_dir = str(Path(__file__).resolve().parent / "static")
app.mount("/static", StaticFiles(directory=_static_dir), name="static")

# REST API routes
app.include_router(auth.router, prefix="/api/auth", tags=["认证"])
app.include_router(admin.router, prefix="/api/admin", tags=["管理员"])
app.include_router(venues.router, prefix="/api/venues", tags=["场地"])
app.include_router(bookings.router, prefix="/api/bookings", tags=["预约"])
app.include_router(teams.router, prefix="/api/teams", tags=["团队"])
app.include_router(users.router, prefix="/api/users", tags=["用户"])
app.include_router(notifications.router, prefix="/api/notifications", tags=["通知"])
app.include_router(system.router, prefix="/api/system", tags=["系统"])

# 管理后台 HTML 页面
app.include_router(admin_ui.router, prefix="/admin", tags=["管理后台"])


@app.on_event("startup")
async def startup():
    Base.metadata.create_all(bind=engine)
    init_super_admin()
    init_test_user()


@app.get("/api/health")
async def health():
    return {"status": "ok", "service": "doumen-culture"}


if __name__ == "__main__":
    import uvicorn
    uvicorn.run("app.main:app", host="0.0.0.0", port=8000, reload=True)