"""管理后台 HTML 页面路由"""
import logging
from fastapi import APIRouter, Request
from fastapi.responses import HTMLResponse
from fastapi.templating import Jinja2Templates
from pathlib import Path

# 创建本模块专用的 logger
logger = logging.getLogger("admin_ui")
logger.setLevel(logging.DEBUG)

router = APIRouter()

_templates_dir = str(Path(__file__).resolve().parent.parent / "templates")
logger.info(f"[admin_ui] 模板目录: {_templates_dir}")
templates = Jinja2Templates(directory=_templates_dir)


@router.get("/login", response_class=HTMLResponse)
async def admin_login_page(request: Request):
    logger.info(f"[admin_ui] GET /login from {request.client.host}")
    return templates.TemplateResponse("admin/login.html", {"request": request})


@router.get("/dashboard", response_class=HTMLResponse)
async def admin_dashboard_page(request: Request):
    logger.info(f"[admin_ui] GET /dashboard")
    return templates.TemplateResponse("admin/dashboard.html", {"request": request})


@router.get("/venues", response_class=HTMLResponse)
async def admin_venues_page(request: Request):
    return templates.TemplateResponse("admin/venues.html", {"request": request})


@router.get("/bookings", response_class=HTMLResponse)
async def admin_bookings_page(request: Request):
    return templates.TemplateResponse("admin/bookings.html", {"request": request})


@router.get("/time-slots", response_class=HTMLResponse)
async def admin_time_slots_page(request: Request):
    return templates.TemplateResponse("admin/time_slots.html", {"request": request})


@router.get("/notifications", response_class=HTMLResponse)
async def admin_notifications_page(request: Request):
    return templates.TemplateResponse("admin/notifications.html", {"request": request})


@router.get("/users", response_class=HTMLResponse)
async def admin_users_page(request: Request):
    return templates.TemplateResponse("admin/admin_users.html", {"request": request})


@router.get("/app-users", response_class=HTMLResponse)
async def admin_app_users_page(request: Request):
    return templates.TemplateResponse("admin/app_users.html", {"request": request})


@router.get("/", response_class=HTMLResponse)
async def admin_redirect(request: Request):
    return templates.TemplateResponse("admin/login.html", {"request": request})