from fastapi import APIRouter, Depends, HTTPException, Form
from app.database import get_db
from app.models import SystemConfig, TimeSlot, Venue, Booking, BookingStatus
from app.routes.auth import get_current_admin
from sqlalchemy.orm import Session
from datetime import date, time, timedelta
import logging

logger = logging.getLogger(__name__)

router = APIRouter()


@router.get("/time-slots")
async def get_time_slots(venue_id: int = None, db: Session = Depends(get_db)):
    q = db.query(TimeSlot)
    if venue_id:
        q = q.filter(TimeSlot.venue_id == venue_id)
    slots = q.order_by(TimeSlot.available_date, TimeSlot.start_time).all()
    return [{
        "id": s.id, "venue_id": s.venue_id,
        "venue": {"id": s.venue.id, "name": s.venue.name} if s.venue else None,
        "available_date": str(s.available_date),
        "start_time": str(s.start_time), "end_time": str(s.end_time),
        "duration_minutes": s.duration_minutes,
        "period": s.period, "is_open": s.is_open
    } for s in slots]


@router.delete("/time-slots/{slot_id}")
async def delete_time_slot(slot_id: int, admin=Depends(get_current_admin), db: Session = Depends(get_db)):
    """删除单个时间段"""
    slot = db.query(TimeSlot).filter_by(id=slot_id).first()
    if not slot:
        raise HTTPException(status_code=404, detail="时间段不存在")

    booked = db.query(Booking).filter(
        Booking.time_slot_id == slot_id,
        Booking.status.in_([BookingStatus.PENDING, BookingStatus.WON])
    ).first()
    if booked:
        raise HTTPException(status_code=400, detail="该时间段存在有效预约，无法删除")

    db.delete(slot)
    db.commit()
    return {"message": "时间段已删除", "id": slot_id}


@router.post("/time-slots/generate")
async def generate_time_slots(
    venue_id: int = Form(...),
    start_date: str = Form(...),
    end_date: str = Form(...),
    start_time: str = Form(...),
    end_time: str = Form(...),
    duration_minutes: int = Form(90),
    admin=Depends(get_current_admin),
    db: Session = Depends(get_db)
):
    """生成指定日期范围的时间段"""
    from datetime import datetime as dt

    start_d = dt.strptime(start_date, "%Y-%m-%d").date()
    end_d = dt.strptime(end_date, "%Y-%m-%d").date()
    start_t = dt.strptime(start_time, "%H:%M").time()
    end_t = dt.strptime(end_time, "%H:%M").time()

    venue = db.query(Venue).filter_by(id=venue_id, is_active=True).first()
    if not venue:
        raise HTTPException(status_code=404, detail="场地不存在")

    logger.info(f"[generate] 参数: venue={venue_id}, {start_date}~{end_date}, {start_time}-{end_time}, 间隔{duration_minutes}分钟")
    created = 0
    skipped_monday = 0
    skipped_existing = 0
    current = start_d
    while current <= end_d:
        if current.weekday() == 0:  # 周一除外
            logger.info(f"[generate] 跳过周一: {current}")
            skipped_monday += 1
            current += timedelta(days=1)
            continue

        existing = db.query(TimeSlot).filter_by(
            venue_id=venue_id, available_date=current
        ).first()
        if existing:
            logger.info(f"[generate] 跳过已有数据: {current}")
            skipped_existing += 1
            current += timedelta(days=1)
            continue

        slot_start = start_t
        while True:
            h = slot_start.hour + slot_start.minute / 60
            slot_end_h = h + duration_minutes / 60
            slot_end = time(int(slot_end_h), int((slot_end_h % 1) * 60))
            if slot_end > end_t:
                break

            period = "morning" if slot_start.hour < 12 else "afternoon"
            slot = TimeSlot(
                venue_id=venue_id, available_date=current,
                start_time=slot_start, end_time=slot_end,
                duration_minutes=duration_minutes, period=period
            )
            db.add(slot)
            created += 1

            slot_start = slot_end

        current += timedelta(days=1)

    db.commit()
    extra = ""
    if created == 0:
        if skipped_monday > 0:
            extra = f" (请选择周二至周日的日期范围)"
        elif skipped_existing > 0:
            extra = f" (已有{skipped_existing}天存在时间段，无需重复生成)"
    return {"message": f"已生成 {created} 个时间段{extra}", "count": created, "skipped_monday": skipped_monday, "skipped_existing": skipped_existing}


@router.get("/config")
async def get_configs(db: Session = Depends(get_db)):
    configs = db.query(SystemConfig).all()
    return {c.key: c.value for c in configs}


@router.put("/config")
async def set_config(
    key: str = Form(...),
    value: str = Form(...),
    admin=Depends(get_current_admin),
    db: Session = Depends(get_db)
):
    cfg = db.query(SystemConfig).filter_by(key=key).first()
    if cfg:
        cfg.value = value
    else:
        cfg = SystemConfig(key=key, value=value)
        db.add(cfg)
    db.commit()
    return {"message": "配置已更新"}