import os
from pathlib import Path

# 手动解析 .env，不依赖 python-dotenv 的查找逻辑和 override 行为
_env_path = Path(__file__).resolve().parent.parent / ".env"
if _env_path.exists():
    with open(_env_path, encoding="utf-8") as f:
        for line in f:
            line = line.strip()
            if line and not line.startswith("#") and "=" in line:
                key, _, value = line.partition("=")
                key = key.strip()
                value = value.strip()
                # 强制覆盖，确保 .env 始终生效
                os.environ[key] = value
    print(f"[config] 已加载 .env: {_env_path}")
else:
    print(f"[config] 未找到 .env: {_env_path}，使用默认值")

# 二次确认：如果 DATABASE_URL 还是 mysql，强制回退到 SQLite
_db_url = os.environ.get("DATABASE_URL", "")
if "mysql" in _db_url.lower():
    print("[config] 检测到 MySQL 配置，本地开发强制使用 SQLite")
    os.environ["DATABASE_URL"] = "sqlite:///./doumen.db"


class Settings:
    DATABASE_URL: str = os.environ.get("DATABASE_URL", "sqlite:///./doumen.db")
    SECRET_KEY: str = os.environ.get("SECRET_KEY", "dev-secret-change-me")
    WECHAT_APPID: str = os.environ.get("WECHAT_APPID", "wx4056aef6d3021576")
    WECHAT_SECRET: str = os.environ.get("WECHAT_SECRET", "1245775ae924d67a1d8d024f1a32b523")
    WECHAT_IOS_APPID: str = os.environ.get("WECHAT_IOS_APPID", "MengYanLin.MiniApp")
    WECHAT_IOS_SECRET: str = os.environ.get("WECHAT_IOS_SECRET", "")
    ADMIN_INIT_USERNAME: str = os.environ.get("ADMIN_INIT_USERNAME", "superadmin")
    ADMIN_INIT_PASSWORD: str = os.environ.get("ADMIN_INIT_PASSWORD", "SuperAdmin@2026")
    ENVIRONMENT: str = os.environ.get("ENVIRONMENT", "development")
    ACCESS_TOKEN_EXPIRE_MINUTES: int = 60 * 24 * 7  # 7 days

    def __init__(self):
        print(f"[config] 最终 DATABASE_URL = {self.DATABASE_URL}")


settings = Settings()