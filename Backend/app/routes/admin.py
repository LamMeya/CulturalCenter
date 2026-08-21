from fastapi import APIRouter, Depends, HTTPException, Form
from app.database import SessionLocal, get_db
from app.models import AdminUser, AdminRole, User, Team, TeamMember
from app.routes.auth import get_current_admin, require_role
from app.services.init_service import hash_password
from sqlalchemy.orm import Session
from sqlalchemy import func

router = APIRouter()


@router.get("/users")
async def list_admins(
    admin: AdminUser = Depends(require_role(AdminRole.SUPER)),
    db: Session = Depends(get_db)
):
    users = db.query(AdminUser).order_by(AdminUser.created_at.desc()).all()
    return [{
        "id": u.id,
        "username": u.username,
        "name": u.name,
        "role": u.role.value,
        "is_active": u.is_active,
        "created_at": u.created_at.isoformat() if u.created_at else None
    } for u in users]


@router.post("/users")
async def create_admin(
    username: str = Form(...),
    password: str = Form(...),
    name: str = Form(...),
    role: str = Form("operator"),
    admin: AdminUser = Depends(require_role(AdminRole.SUPER)),
    db: Session = Depends(get_db)
):
    if role not in ["admin", "operator"]:
        raise HTTPException(status_code=400, detail="角色只能为 admin 或 operator")
    existing = db.query(AdminUser).filter_by(username=username).first()
    if existing:
        raise HTTPException(status_code=400, detail="账号已存在")
    role_enum = AdminRole.ADMIN if role == "admin" else AdminRole.OPERATOR
    new_admin = AdminUser(
        username=username,
        password_hash=hash_password(password),
        name=name,
        role=role_enum
    )
    db.add(new_admin)
    db.commit()
    return {"message": "创建成功", "id": new_admin.id}


@router.put("/users/{user_id}/deactivate")
async def deactivate_admin(
    user_id: int,
    admin: AdminUser = Depends(require_role(AdminRole.SUPER)),
    db: Session = Depends(get_db)
):
    target = db.query(AdminUser).filter_by(id=user_id).first()
    if not target:
        raise HTTPException(status_code=404, detail="管理员不存在")
    if target.id == admin.id:
        raise HTTPException(status_code=400, detail="不能注销自己")
    if target.role == AdminRole.SUPER:
        raise HTTPException(status_code=400, detail="不能注销超级管理员")
    target.is_active = False
    db.commit()
    return {"message": "已注销"}


# ─── App 用户管理 ───

@router.get("/app-users")
async def list_app_users(
    search: str = "",
    admin=Depends(get_current_admin),
    db: Session = Depends(get_db)
):
    """管理后台：查看 App 注册用户列表（含团队信息）"""
    q = db.query(User)
    if search:
        q = q.filter(
            (User.username.ilike(f"%{search}%")) |
            (User.nickname.ilike(f"%{search}%")) |
            (User.phone.ilike(f"%{search}%"))
        )
    users = q.order_by(User.created_at.desc()).all()

    result = []
    for u in users:
        # 获取团队信息
        membership = db.query(TeamMember).filter_by(user_id=u.id).first()
        team_name = None
        team_role = None
        joined_at = None
        if membership:
            team = db.query(Team).filter_by(id=membership.team_id).first()
            if team:
                team_name = team.name
                team_role = membership.role.value
                joined_at = membership.joined_at.isoformat() if membership.joined_at else None

        result.append({
            "id": u.id,
            "username": u.username,
            "nickname": u.nickname,
            "phone": u.phone,
            "source": u.source,
            "created_at": u.created_at.isoformat() if u.created_at else None,
            "team_name": team_name,
            "team_role": team_role,
            "joined_at": joined_at
        })

    return result


@router.get("/privilege-info")
async def get_privilege_info(admin: AdminUser = Depends(get_current_admin)):
    limits = {AdminRole.SUPER: 4, AdminRole.ADMIN: 2, AdminRole.OPERATOR: 1}
    return {
        "role": admin.role.value,
        "privilege_limit": limits.get(admin.role, 0),
        "privilege_used": 0,
        "privilege_remaining": limits.get(admin.role, 0)
    }