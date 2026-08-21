from fastapi import APIRouter, Depends, HTTPException, Form
from pydantic import BaseModel
from app.database import get_db
from app.models import Team, TeamMember, TeamRole, User, Booking, BookingStatus, CancelRecord
from sqlalchemy.orm import Session
from datetime import datetime, timezone, timedelta
from typing import Optional

router = APIRouter()


def now_cn():
    return datetime.now(timezone(timedelta(hours=8)))


# ─── Pydantic Models ───

class CreateTeamBody(BaseModel):
    name: str
    intro: str = ""
    leader_nickname: str = ""
    user_id: Optional[int] = None


class JoinTeamBody(BaseModel):
    team_id: int
    user_id: Optional[int] = None


class LeaveTeamBody(BaseModel):
    user_id: int


# ─── Helpers ───

def _team_to_dict(team: Team) -> dict:
    members = [{
        "id": m.id, "user_id": m.user_id,
        "nickname": m.user.nickname if m.user else "",
        "avatar_url": m.user.avatar_url if m.user else None,
        "role": m.role.value,
        "joined_at": m.joined_at.isoformat() if m.joined_at else None
    } for m in team.members]
    leader = next((m for m in team.members if m.role == TeamRole.LEADER), None)
    return {
        "id": team.id, "name": team.name, "intro": team.intro,
        "member_count": len(members), "members": members,
        "max_members": team.max_members,
        "leader_id": leader.user_id if leader else None,
        "leader_name": leader.user.nickname if (leader and leader.user) else "",
        "created_at": team.created_at.isoformat() if team.created_at else None
    }


# ─── Routes ───

@router.get("")
async def list_teams(search: str = "", db: Session = Depends(get_db)):
    q = db.query(Team).filter(Team.is_active == True)
    if search:
        q = q.join(Team.members).join(TeamMember.user).filter(
            (Team.name.ilike(f"%{search}%")) |
            (User.nickname.ilike(f"%{search}%"))
        ).distinct()
    teams = q.all()
    return [_team_to_dict(t) for t in teams]


@router.post("")
async def create_team(body: CreateTeamBody, db: Session = Depends(get_db)):
    existing = db.query(Team).filter_by(name=body.name).first()
    if existing:
        raise HTTPException(status_code=400, detail="团队名称已存在")
    team = Team(name=body.name, intro=body.intro)
    db.add(team)
    db.flush()
    if body.user_id:
        member = TeamMember(user_id=body.user_id, team_id=team.id, role=TeamRole.LEADER)
        db.add(member)
    db.commit()
    db.refresh(team)
    return _team_to_dict(team)


@router.get("/{team_id}")
async def get_team(team_id: int, db: Session = Depends(get_db)):
    team = db.query(Team).filter_by(id=team_id).first()
    if not team:
        raise HTTPException(status_code=404, detail="团队不存在")
    return _team_to_dict(team)


@router.post("/{team_id}/join")
async def join_team(team_id: int, body: JoinTeamBody, db: Session = Depends(get_db)):
    uid = body.user_id
    if not uid:
        raise HTTPException(status_code=400, detail="缺少 user_id")
    team = db.query(Team).filter_by(id=team_id, is_active=True).first()
    if not team:
        raise HTTPException(status_code=404, detail="团队不存在")
    if len(team.members) >= team.max_members:
        raise HTTPException(status_code=400, detail="团队已满员")
    existing = db.query(TeamMember).filter_by(user_id=uid, team_id=team_id).first()
    if existing:
        raise HTTPException(status_code=400, detail="已加入该团队")
    member = TeamMember(user_id=uid, team_id=team_id, role=TeamRole.MEMBER)
    db.add(member)
    db.commit()
    db.refresh(team)
    return _team_to_dict(team)


@router.post("/{team_id}/leave")
async def leave_team(team_id: int, body: LeaveTeamBody, db: Session = Depends(get_db)):
    member = db.query(TeamMember).filter_by(user_id=body.user_id, team_id=team_id).first()
    if not member:
        raise HTTPException(status_code=404, detail="未加入该团队")
    if member.role == TeamRole.LEADER:
        raise HTTPException(status_code=400, detail="团长不能直接退出，请先转移团长")
    db.delete(member)
    db.commit()
    return {"message": "已退出团队"}


@router.post("/{team_id}/set-vice-leaders")
async def set_vice_leaders(
    team_id: int, user_ids: list[int], leader_id: int,
    db: Session = Depends(get_db)
):
    leader = db.query(TeamMember).filter_by(
        user_id=leader_id, team_id=team_id, role=TeamRole.LEADER
    ).first()
    if not leader:
        raise HTTPException(status_code=403, detail="仅团长可设置副团长")
    if len(user_ids) > 4:
        raise HTTPException(status_code=400, detail="副团长最多4人")
    db.query(TeamMember).filter_by(team_id=team_id, role=TeamRole.VICE_LEADER).update(
        {TeamMember.role: TeamRole.MEMBER}
    )
    for uid in user_ids:
        m = db.query(TeamMember).filter_by(user_id=uid, team_id=team_id).first()
        if m and m.role != TeamRole.LEADER:
            m.role = TeamRole.VICE_LEADER
    db.commit()
    return {"message": "副团长已设置"}


@router.post("/{team_id}/transfer-leader")
async def transfer_leader(
    team_id: int, from_user_id: int, to_user_id: int,
    db: Session = Depends(get_db)
):
    from_member = db.query(TeamMember).filter_by(
        user_id=from_user_id, team_id=team_id, role=TeamRole.LEADER
    ).first()
    if not from_member:
        raise HTTPException(status_code=403, detail="仅团长可转移")
    to_member = db.query(TeamMember).filter_by(user_id=to_user_id, team_id=team_id).first()
    if not to_member:
        raise HTTPException(status_code=404, detail="目标用户不在团队中")
    from_member.role = TeamRole.MEMBER
    to_member.role = TeamRole.LEADER
    db.commit()
    return {"message": "团长已转移"}


@router.delete("/{team_id}")
async def dissolve_team(team_id: int, leader_id: int, db: Session = Depends(get_db)):
    leader = db.query(TeamMember).filter_by(
        user_id=leader_id, team_id=team_id, role=TeamRole.LEADER
    ).first()
    if not leader:
        raise HTTPException(status_code=403, detail="仅团长可解散团队")
    team = db.query(Team).filter_by(id=team_id).first()
    team.is_active = False
    db.commit()
    return {"message": "团队已解散"}


@router.get("/{team_id}/booking-stats")
async def team_booking_stats(team_id: int, db: Session = Depends(get_db)):
    two_weeks_ago = now_cn() - timedelta(days=14)
    thirty_days_ago = now_cn() - timedelta(days=30)
    success_count = db.query(Booking).filter(
        Booking.team_id == team_id,
        Booking.status == BookingStatus.WON,
        Booking.created_at >= two_weeks_ago
    ).count()
    cancel_count = db.query(CancelRecord).filter(
        CancelRecord.team_id == team_id,
        CancelRecord.cancelled_at >= thirty_days_ago
    ).count()
    return {
        "success_2weeks": success_count,
        "max_success_2weeks": 2,
        "cancel_30days": cancel_count,
        "max_cancel_30days": 2
    }