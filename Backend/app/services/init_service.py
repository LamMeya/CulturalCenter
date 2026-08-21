import bcrypt
from app.config import settings
from app.database import SessionLocal
from app.models import AdminUser, AdminRole, User


def hash_password(password: str) -> str:
    return bcrypt.hashpw(password.encode("utf-8"), bcrypt.gensalt()).decode("utf-8")


def verify_password(plain: str, hashed: str) -> bool:
    return bcrypt.checkpw(plain.encode("utf-8"), hashed.encode("utf-8"))


def init_super_admin():
    """初始化超级管理员"""
    db = SessionLocal()
    try:
        existing = db.query(AdminUser).filter_by(username=settings.ADMIN_INIT_USERNAME).first()
        if not existing:
            admin = AdminUser(
                username=settings.ADMIN_INIT_USERNAME,
                password_hash=hash_password(settings.ADMIN_INIT_PASSWORD),
                name="超级管理员",
                role=AdminRole.SUPER,
            )
            db.add(admin)
            db.commit()
            print(f"[INIT] 超级管理员已创建: {settings.ADMIN_INIT_USERNAME}")
    finally:
        db.close()


def init_test_user():
    """初始化测试用户（账号密码登录用）"""
    db = SessionLocal()
    try:
        existing = db.query(User).filter_by(username="test").first()
        if not existing:
            user = User(
                username="test",
                password_hash=hash_password("123456"),
                nickname="测试用户",
                phone="13800138000",
                source="password",
                openid="password_test"  # 旧DB openid 有 NOT NULL 约束
            )
            db.add(user)
            db.commit()
            print(f"[INIT] 测试用户已创建: test / 123456")
    finally:
        db.close()