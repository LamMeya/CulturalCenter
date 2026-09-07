from fastapi import APIRouter, Depends, HTTPException, status, Form
from fastapi.security import HTTPBearer, HTTPAuthorizationCredentials
from jose import JWTError, jwt
from datetime import datetime, timedelta, timezone
from app.config import settings
from app.models import AdminUser, AdminRole

router = APIRouter()
security = HTTPBearer()


def create_token(data: dict) -> str:
    to_encode = data.copy()
    expire = datetime.now(timezone.utc) + timedelta(minutes=settings.ACCESS_TOKEN_EXPIRE_MINUTES)
    to_encode.update({"exp": expire})
    return jwt.encode(to_encode, settings.SECRET_KEY, algorithm="HS256")


def get_current_admin(credentials: HTTPAuthorizationCredentials = Depends(security)):
    from app.database import SessionLocal
    try:
        payload = jwt.decode(credentials.credentials, settings.SECRET_KEY, algorithms=["HS256"])
        admin_id = payload.get("sub")
        if not admin_id:
            raise HTTPException(status_code=401)
    except JWTError:
        raise HTTPException(status_code=401, detail="无效的令牌")

    db = SessionLocal()
    try:
        admin = db.query(AdminUser).filter_by(id=int(admin_id), is_active=True).first()
        if not admin:
            raise HTTPException(status_code=401, detail="管理员不存在")
        return admin
    finally:
        db.close()


def require_role(*roles: AdminRole):
    def checker(admin: AdminUser = Depends(get_current_admin)):
        if admin.role not in roles:
            raise HTTPException(status_code=403, detail="权限不足")
        return admin
    return checker


def get_current_user(credentials: HTTPAuthorizationCredentials = Depends(security)):
    """App 用户端：从 Bearer token 解析当前用户"""
    from app.database import SessionLocal
    from app.models import User
    try:
        payload = jwt.decode(credentials.credentials, settings.SECRET_KEY, algorithms=["HS256"])
        user_id = payload.get("sub")
        if not user_id:
            raise HTTPException(status_code=401, detail="无效的令牌")
    except JWTError:
        raise HTTPException(status_code=401, detail="无效的令牌")

    db = SessionLocal()
    try:
        user = db.query(User).filter_by(id=int(user_id)).first()
        if not user:
            raise HTTPException(status_code=401, detail="用户不存在")
        return user
    finally:
        db.close()


@router.post("/login")
async def admin_login(username: str = Form(...), password: str = Form(...)):
    """管理员登录 - 接收 x-www-form-urlencoded 表单数据"""
    from app.database import SessionLocal
    from app.services.init_service import verify_password

    db = SessionLocal()
    try:
        admin = db.query(AdminUser).filter_by(username=username, is_active=True).first()
        if not admin or not verify_password(password, admin.password_hash):
            raise HTTPException(status_code=401, detail="账号或密码错误")

        token = create_token({"sub": str(admin.id), "role": admin.role.value})
        return {
            "token": token,
            "admin": {
                "id": admin.id,
                "name": admin.name,
                "role": admin.role.value
            }
        }
    finally:
        db.close()


@router.get("/me")
async def get_me(admin: AdminUser = Depends(get_current_admin)):
    return {
        "id": admin.id,
        "name": admin.name,
        "role": admin.role.value,
        "username": admin.username
    }