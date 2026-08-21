from sqlalchemy import (
    Column, Integer, String, Text, DateTime, Boolean, ForeignKey, Enum, Time, Date
)
from sqlalchemy.orm import relationship
from datetime import datetime, timezone, timedelta
import enum
from app.database import Base


def now_cn():
    return datetime.now(timezone(timedelta(hours=8)))


class AdminRole(str, enum.Enum):
    SUPER = "super_admin"
    ADMIN = "admin"
    OPERATOR = "operator"


class BookingStatus(str, enum.Enum):
    PENDING = "pending"       # 待抽签
    WON = "won"               # 已中签
    LOST = "lost"             # 未中签
    CANCELLED = "cancelled"   # 已取消


class TeamRole(str, enum.Enum):
    LEADER = "leader"
    VICE_LEADER = "vice_leader"
    MEMBER = "member"


class AdminUser(Base):
    __tablename__ = "admin_users"

    id = Column(Integer, primary_key=True, autoincrement=True)
    username = Column(String(50), unique=True, nullable=False, index=True)
    password_hash = Column(String(200), nullable=False)
    name = Column(String(50), nullable=False)
    role = Column(Enum(AdminRole), nullable=False, default=AdminRole.OPERATOR)
    is_active = Column(Boolean, default=True)
    created_at = Column(DateTime, default=now_cn)
    updated_at = Column(DateTime, default=now_cn, onupdate=now_cn)


class User(Base):
    """微信小程序 / iOS App 用户"""
    __tablename__ = "users"

    id = Column(Integer, primary_key=True, autoincrement=True)
    username = Column(String(50), unique=True, nullable=True, index=True)
    password_hash = Column(String(200), nullable=True)
    openid = Column(String(100), unique=True, nullable=True, index=True)
    unionid = Column(String(100), unique=True, nullable=True)
    nickname = Column(String(100), nullable=False)
    phone = Column(String(20), nullable=True)
    avatar_url = Column(String(500), nullable=True)
    source = Column(String(20), default="miniprogram")  # miniprogram / ios / password
    created_at = Column(DateTime, default=now_cn)
    updated_at = Column(DateTime, default=now_cn, onupdate=now_cn)

    memberships = relationship("TeamMember", back_populates="user")
    bookings = relationship("Booking", back_populates="user")


class Team(Base):
    """团队"""
    __tablename__ = "teams"

    id = Column(Integer, primary_key=True, autoincrement=True)
    name = Column(String(100), unique=True, nullable=False)
    intro = Column(Text, nullable=True)
    max_members = Column(Integer, default=80)
    is_active = Column(Boolean, default=True)
    created_at = Column(DateTime, default=now_cn)

    members = relationship("TeamMember", back_populates="team", cascade="all, delete-orphan")
    bookings = relationship("Booking", back_populates="team")


class TeamMember(Base):
    """团队成员"""
    __tablename__ = "team_members"

    id = Column(Integer, primary_key=True, autoincrement=True)
    user_id = Column(Integer, ForeignKey("users.id"), nullable=False)
    team_id = Column(Integer, ForeignKey("teams.id"), nullable=False)
    role = Column(Enum(TeamRole), nullable=False, default=TeamRole.MEMBER)
    joined_at = Column(DateTime, default=now_cn)

    user = relationship("User", back_populates="memberships")
    team = relationship("Team", back_populates="members")


class Venue(Base):
    """场地"""
    __tablename__ = "venues"

    id = Column(Integer, primary_key=True, autoincrement=True)
    name = Column(String(100), nullable=False)
    image_url = Column(String(500), nullable=True)
    description = Column(Text, nullable=True)
    capacity = Column(String(50), nullable=True)
    area = Column(String(50), nullable=True)
    facilities = Column(Text, nullable=True)  # JSON string
    address = Column(String(200), nullable=True)
    is_active = Column(Boolean, default=True)
    created_at = Column(DateTime, default=now_cn)
    updated_at = Column(DateTime, default=now_cn, onupdate=now_cn)

    time_slots = relationship("TimeSlot", back_populates="venue")
    bookings = relationship("Booking", back_populates="venue")


class TimeSlot(Base):
    """可预约时间段"""
    __tablename__ = "time_slots"

    id = Column(Integer, primary_key=True, autoincrement=True)
    venue_id = Column(Integer, ForeignKey("venues.id"), nullable=False)
    available_date = Column(Date, nullable=False)
    start_time = Column(Time, nullable=False)
    end_time = Column(Time, nullable=False)
    duration_minutes = Column(Integer, nullable=False)
    period = Column(String(10), nullable=False)  # morning / afternoon
    is_open = Column(Boolean, default=True)
    created_at = Column(DateTime, default=now_cn)

    venue = relationship("Venue", back_populates="time_slots")


class Booking(Base):
    """预约记录"""
    __tablename__ = "bookings"

    id = Column(Integer, primary_key=True, autoincrement=True)
    user_id = Column(Integer, ForeignKey("users.id"), nullable=False)
    team_id = Column(Integer, ForeignKey("teams.id"), nullable=False)
    venue_id = Column(Integer, ForeignKey("venues.id"), nullable=False)
    time_slot_id = Column(Integer, ForeignKey("time_slots.id"), nullable=False)
    status = Column(Enum(BookingStatus), nullable=False, default=BookingStatus.PENDING)
    is_privilege_draw = Column(Boolean, default=False)  # 是否特权抽签
    cancel_reason = Column(String(200), nullable=True)
    cancelled_at = Column(DateTime, nullable=True)
    created_at = Column(DateTime, default=now_cn)
    updated_at = Column(DateTime, default=now_cn, onupdate=now_cn)

    user = relationship("User", back_populates="bookings")
    team = relationship("Team", back_populates="bookings")
    venue = relationship("Venue", back_populates="bookings")
    time_slot = relationship("TimeSlot")


class Notification(Base):
    """首页通知"""
    __tablename__ = "notifications"

    id = Column(Integer, primary_key=True, autoincrement=True)
    title = Column(String(200), nullable=True, default='')
    content = Column(Text, nullable=False)
    notif_type = Column(String(20), default="normal")  # normal / cancel / system
    is_active = Column(Boolean, default=False)
    created_by = Column(Integer, ForeignKey("admin_users.id"), nullable=True)
    created_at = Column(DateTime, default=now_cn)
    published_at = Column(DateTime, nullable=True)


class DrawRecord(Base):
    """抽签记录"""
    __tablename__ = "draw_records"

    id = Column(Integer, primary_key=True, autoincrement=True)
    admin_id = Column(Integer, ForeignKey("admin_users.id"), nullable=True)
    draw_type = Column(String(20), nullable=False)  # auto / privilege
    target_booking_id = Column(Integer, ForeignKey("bookings.id"), nullable=True)
    result = Column(String(20), nullable=False)  # won / lost
    created_at = Column(DateTime, default=now_cn)


class CancelRecord(Base):
    """取消记录（用于频次统计）"""
    __tablename__ = "cancel_records"

    id = Column(Integer, primary_key=True, autoincrement=True)
    user_id = Column(Integer, ForeignKey("users.id"), nullable=False)
    team_id = Column(Integer, ForeignKey("teams.id"), nullable=False)
    booking_id = Column(Integer, ForeignKey("bookings.id"), nullable=True)
    cancelled_at = Column(DateTime, default=now_cn)


class SystemConfig(Base):
    """系统配置"""
    __tablename__ = "system_configs"

    id = Column(Integer, primary_key=True, autoincrement=True)
    key = Column(String(50), unique=True, nullable=False)
    value = Column(Text, nullable=True)
    updated_at = Column(DateTime, default=now_cn, onupdate=now_cn)