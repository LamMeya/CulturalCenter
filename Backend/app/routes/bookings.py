from fastapi import APIRouter, Depends, HTTPException, Form
from pydantic import BaseModel
from app.database import get_db
from app.models import (
    Booking, BookingStatus, TimeSlot, Venue, Team, User, TeamMember, TeamRole,
    DrawRecord, CancelRecord, AdminUser, AdminRole
)
from app.routes.auth import get_current_admin, require_role, get_current_user
from sqlalchemy.orm import Session
from sqlalchemy import func
from datetime import datetime, timezone, timedelta
import random

router = APIRouter()


def now_cn():
    return datetime.now(timezone(timedelta(hours=8)))


# ─── 用户端：创建预约 ───

class CreateBookingBody(BaseModel):
    venue_id: int
    team_id: int
    time_slot_ids: list[int]


@router.post("")
async def create_booking(body: CreateBookingBody, db: Session = Depends(get_db), user: User = Depends(get_current_user)):
    """用户端：提交预约申请（每个时间段一条 booking，允许不同团队竞争同一时间段）"""
    if not body.time_slot_ids:
        raise HTTPException(status_code=400, detail="请选择至少一个时间段")
    if len(body.time_slot_ids) > 4:
        raise HTTPException(status_code=400, detail="最多选择4个时间段")

    # 验证场地
    venue = db.query(Venue).filter_by(id=body.venue_id, is_active=True).first()
    if not venue:
        raise HTTPException(status_code=404, detail="场地不存在")

    # 验证团队
    team = db.query(Team).filter_by(id=body.team_id, is_active=True).first()
    if not team:
        raise HTTPException(status_code=404, detail="团队不存在")

    # 验证用户是否属于该团队
    membership = db.query(TeamMember).filter_by(user_id=user.id, team_id=body.team_id).first()
    if not membership:
        raise HTTPException(status_code=400, detail="您不属于该团队")

    # 检查2周内成功预约次数
    two_weeks_ago = now_cn() - timedelta(days=14)
    recent_wins = db.query(Booking).filter(
        Booking.team_id == body.team_id,
        Booking.status == BookingStatus.WON,
        Booking.created_at >= two_weeks_ago
    ).count()
    if recent_wins >= 4:
        raise HTTPException(status_code=400, detail="2周内成功预约已达上限（4次）")

    created = []
    for slot_id in body.time_slot_ids:
        slot = db.query(TimeSlot).filter_by(id=slot_id, venue_id=body.venue_id).first()
        if not slot:
            raise HTTPException(status_code=404, detail=f"时间段 {slot_id} 不存在")

        # 检查是否已有中签记录（WON 状态不可再预约）
        won = db.query(Booking).filter(
            Booking.time_slot_id == slot_id,
            Booking.status == BookingStatus.WON
        ).first()
        if won:
            raise HTTPException(status_code=400, detail=f"时间段 {slot_id} 已被中签占用")

        # 检查同一团队是否已预约该时间段
        existing_team = db.query(Booking).filter(
            Booking.time_slot_id == slot_id,
            Booking.team_id == body.team_id,
            Booking.status.in_([BookingStatus.PENDING, BookingStatus.WON])
        ).first()
        if existing_team:
            raise HTTPException(status_code=400, detail=f"您的团队已预约时间段 {slot_id}")

        booking = Booking(
            user_id=user.id,
            team_id=body.team_id,
            venue_id=body.venue_id,
            time_slot_id=slot_id,
            status=BookingStatus.PENDING
        )
        db.add(booking)
        created.append(booking)

    db.commit()
    return {
        "message": f"已提交 {len(created)} 个预约",
        "booking_ids": [b.id for b in created]
    }


# ─── 管理员端 ───

@router.get("")
async def list_bookings(
    venue_id: int = None, status: str = None, date: str = None,
    team_name: str = None,
    admin=Depends(get_current_admin), db: Session = Depends(get_db)
):
    q = db.query(Booking)
    if venue_id:
        q = q.filter(Booking.venue_id == venue_id)
    if status:
        q = q.filter(Booking.status == status)
    if date:
        q = q.filter(Booking.time_slot.has(available_date=date))
    if team_name:
        q = q.join(Booking.team).filter(Team.name.ilike(f"%{team_name}%"))

    bookings = q.order_by(Booking.created_at.desc()).all()
    return [{
        "id": b.id, "user_id": b.user_id, "team_id": b.team_id,
        "venue_id": b.venue_id, "time_slot_id": b.time_slot_id,
        "status": b.status.value if b.status else None,
        "is_privilege_draw": b.is_privilege_draw,
        "cancel_reason": b.cancel_reason,
        "created_at": b.created_at.isoformat() if b.created_at else None,
        "venue": {"name": b.venue.name} if b.venue else None,
        "team": {"name": b.team.name} if b.team else None,
        "user": {"nickname": b.user.nickname} if b.user else None,
        "time_slot": {
            "available_date": str(b.time_slot.available_date) if b.time_slot else None,
            "start_time": str(b.time_slot.start_time) if b.time_slot else None,
            "end_time": str(b.time_slot.end_time) if b.time_slot else None
        } if b.time_slot else None
    } for b in bookings]


@router.post("/auto-draw")
async def auto_draw(
    admin=Depends(get_current_admin),
    db: Session = Depends(get_db)
):
    """自动抽签：同一时间段多个 PENDING 预约中，随机选一个 WON，其余 LOST"""
    pending = db.query(Booking).filter_by(status=BookingStatus.PENDING).all()
    if not pending:
        return {"message": "没有待抽签的预约", "processed": 0}

    # 按 time_slot_id 分组
    slots = {}
    for b in pending:
        slots.setdefault(b.time_slot_id, []).append(b)

    results = []
    for slot_id, bookings in slots.items():
        winner = random.choice(bookings)
        for booking in bookings:
            won = (booking.id == winner.id)
            booking.status = BookingStatus.WON if won else BookingStatus.LOST
            draw = DrawRecord(
                admin_id=admin.id,
                draw_type="auto",
                target_booking_id=booking.id,
                result="won" if won else "lost"
            )
            db.add(draw)
            results.append({"booking_id": booking.id, "result": "won" if won else "lost"})

    db.commit()
    return {"message": "自动抽签完成", "processed": len(results), "results": results}


@router.post("/{booking_id}/draw")
async def draw_single(
    booking_id: int,
    admin=Depends(get_current_admin),
    db: Session = Depends(get_db)
):
    """单个预约抽签 —— 同一时间段的所有 PENDING 一起处理，只选一个赢家"""
    booking = db.query(Booking).filter_by(id=booking_id, status=BookingStatus.PENDING).first()
    if not booking:
        raise HTTPException(status_code=404, detail="预约不存在或已处理")

    # 同一时间段的所有 PENDING 预约一起处理
    same_slot = db.query(Booking).filter(
        Booking.time_slot_id == booking.time_slot_id,
        Booking.status == BookingStatus.PENDING
    ).all()
    winner = random.choice(same_slot)

    for b in same_slot:
        won = (b.id == winner.id)
        b.status = BookingStatus.WON if won else BookingStatus.LOST
        draw = DrawRecord(
            admin_id=admin.id, draw_type="auto",
            target_booking_id=b.id, result="won" if won else "lost"
        )
        db.add(draw)

    db.commit()
    return {
        "message": "抽签完成",
        "booking_id": booking.id,
        "result": "won" if winner.id == booking_id else "lost"
    }


@router.post("/privilege-draw/{booking_id}")
async def privilege_draw(
    booking_id: int,
    admin=Depends(get_current_admin),
    db: Session = Depends(get_db)
):
    booking = db.query(Booking).filter_by(id=booking_id, status=BookingStatus.PENDING).first()
    if not booking:
        raise HTTPException(status_code=404, detail="预约不存在或已处理")

    limits = {AdminRole.SUPER: 4, AdminRole.ADMIN: 2, AdminRole.OPERATOR: 1}
    limit = limits.get(admin.role, 0)

    used = db.query(DrawRecord).filter(
        DrawRecord.admin_id == admin.id,
        DrawRecord.draw_type == "privilege",
        DrawRecord.created_at >= now_cn() - timedelta(days=14)
    ).count()

    if used >= limit:
        raise HTTPException(status_code=400, detail=f"特权次数已用完（{used}/{limit}）")

    # 特权抽签：该预约直接获胜，同一时间段的其他 PENDING 全部标记为 LOST
    same_slot = db.query(Booking).filter(
        Booking.time_slot_id == booking.time_slot_id,
        Booking.status == BookingStatus.PENDING,
        Booking.id != booking.id
    ).all()

    booking.status = BookingStatus.WON
    booking.is_privilege_draw = True
    draw = DrawRecord(
        admin_id=admin.id, draw_type="privilege",
        target_booking_id=booking.id, result="won"
    )
    db.add(draw)

    for other in same_slot:
        other.status = BookingStatus.LOST
        draw_lost = DrawRecord(
            admin_id=admin.id, draw_type="privilege",
            target_booking_id=other.id, result="lost"
        )
        db.add(draw_lost)

    db.commit()
    return {"message": "特权抽签成功", "booking_id": booking.id, "privilege_used": used + 1}


@router.put("/{booking_id}/cancel")
async def cancel_booking(
    booking_id: int,
    reason: str = Form("管理员取消"),
    admin=Depends(get_current_admin),
    db: Session = Depends(get_db)
):
    booking = db.query(Booking).filter_by(id=booking_id).first()
    if not booking:
        raise HTTPException(status_code=404, detail="预约不存在")

    booking.status = BookingStatus.CANCELLED
    booking.cancel_reason = reason
    booking.cancelled_at = now_cn()

    cancel_record = CancelRecord(
        user_id=booking.user_id, team_id=booking.team_id,
        booking_id=booking.id
    )
    db.add(cancel_record)

    same_slot_pending = db.query(Booking).filter(
        Booking.venue_id == booking.venue_id,
        Booking.time_slot_id == booking.time_slot_id,
        Booking.status == BookingStatus.PENDING,
        Booking.id != booking.id
    ).all()

    redraw_winner = None
    if same_slot_pending:
        redraw_winner = random.choice(same_slot_pending)
        redraw_winner.status = BookingStatus.WON
        draw = DrawRecord(
            admin_id=admin.id, draw_type="auto",
            target_booking_id=redraw_winner.id, result="won"
        )
        db.add(draw)

    db.commit()
    return {
        "message": "预约已取消" + ("，已重新抽签" if redraw_winner else "，无待抽签预约可替代"),
        "redraw_booking_id": redraw_winner.id if redraw_winner else None
    }


@router.get("/stats")
async def booking_stats(admin=Depends(get_current_admin), db: Session = Depends(get_db)):
    total_venues = db.query(func.count(Venue.id)).filter(Venue.is_active == True).scalar()
    weekly_bookings = db.query(func.count(Booking.id)).filter(
        Booking.created_at >= now_cn() - timedelta(days=7)
    ).scalar()
    pending_count = db.query(func.count(Booking.id)).filter(
        Booking.status == BookingStatus.PENDING
    ).scalar()
    won_count = db.query(func.count(Booking.id)).filter(
        Booking.status == BookingStatus.WON
    ).scalar()

    return {
        "total_venues": total_venues or 0,
        "weekly_bookings": weekly_bookings or 0,
        "pending_draws": pending_count or 0,
        "won_bookings": won_count or 0
    }

# ─── 分组抽签（管理后台） ───

@router.get("/groups")
async def list_booking_groups(
    admin=Depends(get_current_admin),
    db: Session = Depends(get_db)
):
    """按 (场地, 时间段) 分组展示预约，每组显示所有竞争团队"""
    pending_bookings = db.query(Booking).filter(
        Booking.status.in_([BookingStatus.PENDING, BookingStatus.WON, BookingStatus.LOST])
    ).order_by(Booking.time_slot_id, Booking.created_at).all()

    # 按 (venue_id, time_slot_id) 分组
    groups = {}
    for b in pending_bookings:
        key = (b.venue_id, b.time_slot_id)
        if key not in groups:
            slot = b.time_slot
            groups[key] = {
                "venue_id": b.venue_id,
                "venue_name": b.venue.name if b.venue else "-",
                "time_slot_id": b.time_slot_id,
                "available_date": str(slot.available_date) if slot else None,
                "start_time": str(slot.start_time) if slot else None,
                "end_time": str(slot.end_time) if slot else None,
                "bookings": [],
                "pending_count": 0,
                "has_won": False,
            }
        group = groups[key]
        status = b.status.value if b.status else "unknown"
        if status == "pending":
            group["pending_count"] += 1
        if status == "won":
            group["has_won"] = True
        group["bookings"].append({
            "id": b.id,
            "team_id": b.team_id,
            "team_name": b.team.name if b.team else "-",
            "user_name": b.user.nickname if b.user else "-",
            "status": status,
            "created_at": b.created_at.isoformat() if b.created_at else None,
        })

    # 按日期+时间排序
    result = sorted(groups.values(), key=lambda g: (g["available_date"] or "", g["start_time"] or ""))
    return result


@router.post("/draw-by-slot")
async def draw_by_slot(
    venue_id: int = Form(...),
    time_slot_id: int = Form(...),
    admin=Depends(get_current_admin),
    db: Session = Depends(get_db)
):
    """对指定场地+时间段的所有 PENDING 预约进行抽签，随机选一个 WON，其余 LOST"""
    pending = db.query(Booking).filter(
        Booking.venue_id == venue_id,
        Booking.time_slot_id == time_slot_id,
        Booking.status == BookingStatus.PENDING
    ).all()

    if not pending:
        raise HTTPException(status_code=400, detail="该组没有待抽签的预约")

    winner = random.choice(pending)
    for b in pending:
        won = (b.id == winner.id)
        b.status = BookingStatus.WON if won else BookingStatus.LOST
        draw = DrawRecord(
            admin_id=admin.id, draw_type="auto",
            target_booking_id=b.id, result="won" if won else "lost"
        )
        db.add(draw)

    db.commit()
    return {
        "message": f"抽签完成，{winner.team.name if winner.team else '?'} 中签",
        "winner_booking_id": winner.id,
        "winner_team": winner.team.name if winner.team else "?",
        "processed": len(pending)
    }
