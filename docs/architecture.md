# 珠海艺术中心 · 系统架构

三层架构：多端客户端 → FastAPI 后端 → 数据库。

## 1. 系统架构

```mermaid
flowchart TB
  subgraph Clients["客户端层 · Clients"]
    direction LR
    Flutter["Flutter App<br/>iOS · Android"]
    Mini["微信小程序<br/>WeChat Mini"]
    Native["Android Native<br/>Kotlin · Compose"]
    Admin["Admin Web<br/>管理后台"]
  end

  Clients -->|"HTTPS + JWT"| Backend

  subgraph Backend["FastAPI Backend<br/>8 route modules · UnifiedResponseMiddleware"]
    direction LR
    AuthMod["auth · admin"]
    VenueMod["venues · time-slots"]
    BookingMod["bookings · draw"]
    TeamMod["teams · members"]
    UserMod["users · profile"]
    NotifMod["notifications"]
  end

  Backend -->|"SQL"| DB[("Database<br/>SQLite / MySQL · SQLAlchemy ORM")]
```

## 2. 用户预约创建 · 调用链路

从登录到预约创建的完整请求路径。

```mermaid
sequenceDiagram
  participant App as Flutter App
  participant API as FastAPI
  participant DB as Database

  App->>API: POST /users/login/password
  API-->>App: JWT token

  App->>API: GET /venues
  API-->>App: 场馆列表

  App->>API: POST /bookings { venue_id, team_id, time_slot_ids }
  activate API
  Note over API: 校验用户团队归属<br/>检查时段占用情况
  API->>DB: INSERT Booking
  DB-->>API: booking_ids
  deactivate API
  API-->>App: { booking_ids: [5] }
```

## 3. 核心数据模型 · 实体关系

Booking 为中心实体，关联用户、团队、场馆时段。

```mermaid
erDiagram
  User ||--o{ Booking : "user_id"
  Team ||--o{ Booking : "team_id"
  TimeSlot ||--o{ Booking : "time_slot_id"
  Venue ||--o{ TimeSlot : "venue_id"
  User }o--o{ Team : "TeamMember (N:N)"

  User {
    int id PK
    string username
    string nickname
    string phone
    string openid
    string avatar_url
  }

  Team {
    int id PK
    string name
    int leader_id FK
    int max_members
  }

  Venue {
    int id PK
    string name
    string location
    int capacity
    bool is_active
  }

  TimeSlot {
    int id PK
    int venue_id FK
    datetime start
    datetime end
    int capacity
    int booked
  }

  Booking {
    int id PK
    int user_id FK
    int team_id FK
    int time_slot_id FK
    string status
  }
```

## 4. 后端路由模块

| 模块 | 路由前缀 | 职责 |
|------|----------|------|
| auth | `/api/auth` | 管理员登录、JWT 解析、角色权限校验 |
| admin | `/api/admin` | 管理员账号管理 |
| venues | `/api/venues` | 场馆列表、详情、可预约时段 |
| bookings | `/api/bookings` | 用户创建预约、管理员列表、自动抽签 |
| teams | `/api/teams` | 团队创建、详情、加入、退出 |
| users | `/api/users` | 用户注册、登录、资料、个人预约列表 |
| notifications | `/api/notifications` | 通知发布、已发布列表 |
| system | `/api/system` | 系统配置 |

## 5. Flutter 层级结构

```mermaid
flowchart LR
  subgraph UI["Screens"]
    Home["HomeScreen"]
    VenueDetail["VenueDetailScreen"]
    Bookings["BookingsScreen"]
    Team["TeamScreen"]
    Profile["ProfileScreen"]
    Login["LoginScreen"]
    Register["RegisterScreen"]
  end

  subgraph State["Providers"]
    AuthP["AuthProvider"]
    VenueP["VenueProvider"]
    BookingsP["BookingsProvider"]
    TeamP["TeamProvider"]
    ProfileP["ProfileProvider"]
  end

  subgraph Service["Service"]
    ApiS["ApiService<br/>Dio HTTP"]
  end

  UI --> State
  State --> ApiS
  ApiS -->|"HTTPS + JWT"| Backend["FastAPI Backend"]
```
