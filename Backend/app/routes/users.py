from fastapi import APIRouter, Depends, HTTPException
from app.database import get_db
from app.models import User, Team, TeamMember, Booking, BookingStatus, CancelRecord
from app.services.init_service import verify_password, hash_password
from app.services.wechat_service import wechat_code_to_session, wechat_ios_token
from app.routes.auth import get_current_admin, create_token, get_current_user
from sqlalchemy.orm import Session
from datetime import datetime, timezone, timedelta
from pydantic import BaseModel

router = APIRouter()


def now_cn():
    return datetime.now(timezone(timedelta(hours=8)))


def _get_user_team_info(user_id: int, db: Session) -> dict:
    """获取用户团队信息，返回 team_id/team_name/team_role 或 None"""
    membership = db.query(TeamMember).filter_by(user_id=user_id).first()
    if membership:
        team = db.query(Team).filter_by(id=membership.team_id).first()
        if team and team.is_active:
            return {
                "team_id": team.id,
                "team_name": team.name,
                "team_role": membership.role.value
            }
    return {"team_id": None, "team_name": None, "team_role": None}


# ─── 账号密码登录 ───

class PasswordLoginRequest(BaseModel):
    username: str
    password: str


class PasswordRegisterRequest(BaseModel):
    username: str
    password: str
    nickname: str = ""
    phone: str = ""


# App 端通用返回包装（匹配 Android 端 ApiResponse<LoginResponse>：{ code, message, data }）
def _ok(data) -> dict:
    return {"code": 0, "message": "ok", "data": data}


@router.post("/login/password")
async def password_login(req: PasswordLoginRequest, db: Session = Depends(get_db)):
    """账号密码登录"""
    user = db.query(User).filter_by(username=req.username).first()
    if not user or not user.password_hash:
        raise HTTPException(status_code=401, detail="账号或密码错误")
    if not verify_password(req.password, user.password_hash):
        raise HTTPException(status_code=401, detail="账号或密码错误")

    team_info = _get_user_team_info(user.id, db)
    token = create_token({"sub": str(user.id), "role": "user"})

    return _ok({
        "token": token,
        "user": {
            "id": user.id,
            "nickname": user.nickname,
            "phone": user.phone,
            "avatar_url": user.avatar_url,
            **team_info
        }
    })


@router.post("/register")
async def register(req: PasswordRegisterRequest, db: Session = Depends(get_db)):
    """用户注册"""
    existing = db.query(User).filter_by(username=req.username).first()
    if existing:
        raise HTTPException(status_code=400, detail="账号已存在")

    user = User(
        username=req.username,
        password_hash=hash_password(req.password),
        nickname=req.nickname or req.username,
        phone=req.phone,
        source="password",
        openid=f"password_{req.username}"
    )
    db.add(user)
    db.commit()
    db.refresh(user)

    token = create_token({"sub": str(user.id), "role": "user"})

    return _ok({
        "token": token,
        "user": {
            "id": user.id,
            "nickname": user.nickname,
            "phone": user.phone,
            "avatar_url": user.avatar_url,
            "team_id": None,
            "team_name": None,
            "team_role": None
        }
    })


# ─── 微信登录（保留兼容） ───

@router.post("/login/miniprogram")
async def mini_program_login(code: str, nickname: str = "", phone: str = "", db: Session = Depends(get_db)):
    """微信小程序登录"""
    session = await wechat_code_to_session(code, "miniprogram")
    if "errcode" in session and session["errcode"] != 0:
        raise HTTPException(status_code=400, detail=f"微信登录失败: {session.get('errmsg', '')}")

    openid = session["openid"]
    unionid = session.get("unionid")

    user = db.query(User).filter_by(openid=openid).first()
    if not user:
        user = User(
            openid=openid, unionid=unionid,
            nickname=nickname or "微信用户",
            phone=phone, source="miniprogram"
        )
        db.add(user)
        db.commit()
        db.refresh(user)
    else:
        if nickname:
            user.nickname = nickname
        if phone:
            user.phone = phone
        user.unionid = unionid or user.unionid
        db.commit()

    return {
        "user_id": user.id,
        "nickname": user.nickname,
        "phone": user.phone,
        "openid": user.openid
    }


@router.post("/login/ios")
async def ios_wechat_login(code: str, db: Session = Depends(get_db)):
    """iOS App 微信SDK登录（保留兼容）"""
    result = await wechat_ios_token(code)
    if "errcode" in result:
        raise HTTPException(status_code=400, detail=f"微信登录失败: {result.get('errmsg', '')}")

    openid = result["openid"]
    unionid = result.get("unionid")
    nickname = result.get("nickname", "iOS用户")
    avatar = result.get("headimgurl", "")

    user = db.query(User).filter_by(openid=openid).first()
    if not user:
        user = User(
            openid=openid, unionid=unionid,
            nickname=nickname, avatar_url=avatar,
            source="ios"
        )
        db.add(user)
        db.commit()
        db.refresh(user)

    return {
        "user_id": user.id,
        "nickname": user.nickname,
        "avatar_url": user.avatar_url,
        "openid": user.openid
    }


# ─── 用户信息 ───

@router.get("/{user_id}/profile")
async def get_user_profile(user_id: int, db: Session = Depends(get_db)):
    user = db.query(User).filter_by(id=user_id).first()
    if not user:
        raise HTTPException(status_code=404, detail="用户不存在")

    team_info = _get_user_team_info(user_id, db)

    return {
        "id": user.id,
        "nickname": user.nickname,
        "phone": user.phone,
        "avatar_url": user.avatar_url,
        "team_id": team_info["team_id"],
        "team_name": team_info["team_name"],
        "team_role": team_info["team_role"]
    }


@router.get("/{user_id}/bookings")
async def get_user_bookings(user_id: int, db: Session = Depends(get_db), user: User = Depends(get_current_user)):
    # 只能查自己的预约
    if user.id != user_id:
        raise HTTPException(status_code=403, detail="无权查看他人预约")
    bookings = db.query(Booking).filter_by(user_id=user_id).order_by(
        Booking.created_at.desc()
    ).all()

    return [{
        "id": b.id, "venue_name": b.venue.name if b.venue else "",
        "team_name": b.team.name if b.team else "",
        "time_slot": {
            "date": str(b.time_slot.available_date) if b.time_slot else None,
            "start": str(b.time_slot.start_time) if b.time_slot else None,
            "end": str(b.time_slot.end_time) if b.time_slot else None
        } if b.time_slot else None,
        "status": b.status.value if b.status else None,
        "cancel_reason": b.cancel_reason,
        "created_at": b.created_at.isoformat() if b.created_at else None
    } for b in bookings]


@router.post("/{user_id}/bookings/{booking_id}/cancel")
async def user_cancel_booking(user_id: int, booking_id: int, db: Session = Depends(get_db), user: User = Depends(get_current_user)):
    """用户取消预约（需检查频次限制）"""
    if user.id != user_id:
        raise HTTPException(status_code=403, detail="无权操作")
    booking = db.query(Booking).filter_by(id=booking_id, user_id=user_id).first()
    if not booking:
        raise HTTPException(status_code=404, detail="预约不存在")

    if booking.status == BookingStatus.CANCELLED:
        raise HTTPException(status_code=400, detail="已取消")

    if booking.status in (BookingStatus.WON, BookingStatus.LOST):
        cancel_count = db.query(CancelRecord).filter(
            CancelRecord.user_id == user_id,
            CancelRecord.cancelled_at >= now_cn() - timedelta(days=30)
        ).count()
        if cancel_count >= 2:
            raise HTTPException(status_code=400, detail="30天内取消已达上限，禁止2周内预约")

    booking.status = BookingStatus.CANCELLED
    booking.cancelled_at = now_cn()
    cancel_record = CancelRecord(
        user_id=user_id, team_id=booking.team_id, booking_id=booking.id
    )
    db.add(cancel_record)
    db.commit()
    return {"message": "预约已取消"}