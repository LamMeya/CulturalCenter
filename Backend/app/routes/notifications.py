from fastapi import APIRouter, Depends, HTTPException, Form
from app.database import get_db
from app.models import Notification
from app.routes.auth import get_current_admin
from sqlalchemy.orm import Session
from datetime import datetime, timezone, timedelta

router = APIRouter()


def now_cn():
    return datetime.now(timezone(timedelta(hours=8)))


@router.get("")
async def list_notifications(db: Session = Depends(get_db)):
    notifs = db.query(Notification).order_by(Notification.created_at.desc()).all()
    return [{
        "id": n.id, "title": n.title, "content": n.content,
        "notif_type": n.notif_type, "is_active": n.is_active,
        "created_at": n.created_at.isoformat() if n.created_at else None
    } for n in notifs]


@router.get("/published")
async def get_published_notification(db: Session = Depends(get_db)):
    notif = db.query(Notification).filter_by(is_active=True).order_by(
        Notification.created_at.desc()
    ).first()
    if not notif:
        return {"content": None, "notif_type": None}
    return {
        "id": notif.id, "title": notif.title,
        "content": notif.content, "notif_type": notif.notif_type
    }


@router.post("")
async def create_notification(
    title: str = Form(""),
    content: str = Form(...),
    notif_type: str = Form("normal"),
    admin=Depends(get_current_admin),
    db: Session = Depends(get_db)
):
    notif = Notification(
        title=title, content=content, notif_type=notif_type,
        is_active=True, created_by=admin.id
    )
    db.add(notif)
    db.commit()
    return {"message": "通知已创建", "id": notif.id}


@router.put("/{notif_id}/activate")
async def activate_notification(
    notif_id: int,
    admin=Depends(get_current_admin),
    db: Session = Depends(get_db)
):
    notif = db.query(Notification).filter_by(id=notif_id).first()
    if not notif:
        raise HTTPException(status_code=404, detail="通知不存在")
    notif.is_active = True
    db.commit()
    return {"message": "通知已发布"}


@router.put("/{notif_id}/deactivate")
async def deactivate_notification(
    notif_id: int,
    admin=Depends(get_current_admin),
    db: Session = Depends(get_db)
):
    notif = db.query(Notification).filter_by(id=notif_id).first()
    if not notif:
        raise HTTPException(status_code=404, detail="通知不存在")
    notif.is_active = False
    db.commit()
    return {"message": "通知已下架"}


@router.delete("/{notif_id}")
async def delete_notification(
    notif_id: int,
    admin=Depends(get_current_admin),
    db: Session = Depends(get_db)
):
    notif = db.query(Notification).filter_by(id=notif_id).first()
    if not notif:
        raise HTTPException(status_code=404, detail="通知不存在")
    db.delete(notif)
    db.commit()
    return {"message": "通知已删除"}