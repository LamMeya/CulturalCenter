from fastapi import APIRouter, Depends, HTTPException, UploadFile, File, Form
import json
from app.database import get_db
from app.models import Venue, TimeSlot, Booking
from app.routes.auth import get_current_admin
from sqlalchemy.orm import Session
from datetime import date as date_type
import os

router = APIRouter()
UPLOAD_DIR = "app/static/uploads/venues"


def _parse_facilities(facilities):
    if not facilities:
        return []
    if isinstance(facilities, list):
        return facilities
    if isinstance(facilities, str):
        try:
            parsed = json.loads(facilities)
            if isinstance(parsed, list):
                return parsed
        except (json.JSONDecodeError, TypeError):
            pass
        return [f.strip() for f in facilities.split(',') if f.strip()]
    return []



@router.get("")
async def list_venues(db: Session = Depends(get_db)):
    venues = db.query(Venue).filter_by(is_active=True).order_by(Venue.created_at.desc()).all()
    return [{
        "id": v.id, "name": v.name, "image_url": v.image_url,
        "description": v.description, "capacity": v.capacity,
        "area": v.area, "facilities": _parse_facilities(v.facilities),
        "address": v.address, "is_active": v.is_active
    } for v in venues]


@router.get("/{venue_id}")
async def get_venue(venue_id: int, db: Session = Depends(get_db)):
    venue = db.query(Venue).filter_by(id=venue_id).first()
    if not venue:
        raise HTTPException(status_code=404, detail="场地不存在")
    return {
        "id": venue.id, "name": venue.name, "image_url": venue.image_url,
        "description": venue.description, "capacity": venue.capacity,
        "area": venue.area, "facilities": venue.facilities,
        "address": venue.address, "is_active": venue.is_active, "facilities": _parse_facilities(venue.facilities)
    }


@router.get("/{venue_id}/time-slots")
async def get_venue_time_slots(venue_id: int, date: str = "", db: Session = Depends(get_db)):
    """用户端：获取指定场馆指定日期的时间段（含预约状态）
    is_booked 仅标记已被中签（WON）的时段，PENDING 状态允许其他团队继续预约竞争"""
    q = db.query(TimeSlot).filter(TimeSlot.venue_id == venue_id)
    if date:
        try:
            target_date = date_type.fromisoformat(date)
            q = q.filter(TimeSlot.available_date == target_date)
        except ValueError:
            raise HTTPException(status_code=400, detail="日期格式错误，应为 YYYY-MM-DD")
    slots = q.order_by(TimeSlot.start_time).all()

    # 查询每个时间段是否已被中签（won 状态）
    booked_slot_ids = set()
    if slots:
        slot_ids = [s.id for s in slots]
        booked = db.query(Booking.time_slot_id).filter(
            Booking.time_slot_id.in_(slot_ids),
            Booking.status == "won"
        ).all()
        booked_slot_ids = {b[0] for b in booked}

    return [{
        "id": s.id, "venue_id": s.venue_id,
        "available_date": str(s.available_date),
        "start_time": str(s.start_time), "end_time": str(s.end_time),
        "period": s.period, "is_open": s.is_open,
        "is_booked": s.id in booked_slot_ids
    } for s in slots]


@router.post("")
async def create_venue(
    name: str = Form(...),
    description: str = Form(""),
    capacity: str = Form(""),
    area: str = Form(""),
    facilities: str = Form(""),
    address: str = Form(""),
    image_url: str = Form(""),
    admin=Depends(get_current_admin),
    db: Session = Depends(get_db)
):
    venue = Venue(
        name=name, description=description, capacity=capacity,
        area=area, facilities=facilities, address=address,
        image_url=image_url
    )
    db.add(venue)
    db.commit()
    return {"message": "场地已创建", "id": venue.id}


@router.put("/{venue_id}")
async def update_venue(
    venue_id: int,
    name: str = Form(None),
    description: str = Form(None),
    capacity: str = Form(None),
    area: str = Form(None),
    facilities: str = Form(None),
    address: str = Form(None),
    image_url: str = Form(None),
    admin=Depends(get_current_admin),
    db: Session = Depends(get_db)
):
    venue = db.query(Venue).filter_by(id=venue_id).first()
    if not venue:
        raise HTTPException(status_code=404, detail="场地不存在")
    for field, val in [("name", name), ("description", description), ("capacity", capacity), ("area", area), ("facilities", facilities), ("address", address), ("image_url", image_url)]:
        if val is not None:
            setattr(venue, field, val)
    db.commit()
    return {"message": "场地已更新"}


@router.put("/{venue_id}/activate")
async def activate_venue(venue_id: int, admin=Depends(get_current_admin), db: Session = Depends(get_db)):
    venue = db.query(Venue).filter_by(id=venue_id).first()
    if not venue:
        raise HTTPException(status_code=404, detail="场地不存在")
    venue.is_active = True
    db.commit()
    return {"message": "场地已启用"}


@router.put("/{venue_id}/deactivate")
async def deactivate_venue(venue_id: int, admin=Depends(get_current_admin), db: Session = Depends(get_db)):
    venue = db.query(Venue).filter_by(id=venue_id).first()
    if not venue:
        raise HTTPException(status_code=404, detail="场地不存在")
    venue.is_active = False
    db.commit()
    return {"message": "场地已停用"}


@router.delete("/{venue_id}")
async def delete_venue(venue_id: int, admin=Depends(get_current_admin), db: Session = Depends(get_db)):
    venue = db.query(Venue).filter_by(id=venue_id).first()
    if not venue:
        raise HTTPException(status_code=404, detail="场地不存在")
    venue.is_active = False
    db.commit()
    return {"message": "场地已删除"}


@router.post("/{venue_id}/upload-image")
async def upload_venue_image(
    venue_id: int, file: UploadFile = File(...),
    admin=Depends(get_current_admin), db: Session = Depends(get_db)
):
    venue = db.query(Venue).filter_by(id=venue_id).first()
    if not venue:
        raise HTTPException(status_code=404, detail="场地不存在")
    os.makedirs(UPLOAD_DIR, exist_ok=True)
    ext = file.filename.split(".")[-1] if "." in (file.filename or "") else "jpg"
    filename = f"venue_{venue_id}.{ext}"
    filepath = os.path.join(UPLOAD_DIR, filename)
    with open(filepath, "wb") as f:
        f.write(await file.read())
    venue.image_url = f"/static/uploads/venues/{filename}"
    db.commit()
    return {"image_url": venue.image_url}