# 珠海艺术中心 · 场地预约系统

广东珠海斗门区文化中心场地预约平台，面向文艺团队提供场地预约、抽签分配、团队管理等功能。覆盖 **管理后台（Web）**、**用户端（Flutter App）**、**用户端（微信小程序）**、**用户端（iOS 原生 App）** 多端。

---

## 项目结构

```
珠海艺术中心/
├── Backend/                    # Python 后台服务（FastAPI + SQLAlchemy）
├── Flutter/                    # Flutter 跨平台 App（Android / iOS / Web / Desktop）
├── Miniapp/                    # 微信小程序
├── iOS/                        # iOS 原生 App（SwiftUI）
├── Android/                    # Android 原生 App（Kotlin，脚手架）
├── doumen-culture-admin/       # 管理后台页面设计稿
├── doumen-culture-app/         # 用户端页面设计稿
├── 项目文档/                    # 产品/技术/UI 设计文档
├── .gitignore
└── README.md
```

---

## 一、后台服务 `Backend/`

### 技术栈

| 组件 | 技术 |
|------|------|
| 框架 | FastAPI 0.104+ |
| ORM | SQLAlchemy 2.0 |
| 数据库 | SQLite（开发）/ 可切换 PostgreSQL |
| 认证 | JWT（python-jose） |
| 密码 | bcrypt（passlib） |
| 模板引擎 | Jinja2（管理后台页面） |
| 任务调度 | APScheduler |

### 快速启动

```bash
cd Backend

# 创建虚拟环境（推荐）
python -m venv venv
source venv/bin/activate   # macOS/Linux
# venv\Scripts\activate    # Windows

# 安装依赖
pip install -r requirements.txt

# 配置环境变量
cp .env.example .env
# 编辑 .env 填入微信 AppID / Secret 等

# 启动服务
python -m uvicorn app.main:app --host 0.0.0.0 --port 8000 --reload
# 访问 http://localhost:8000
```

### 默认登录凭证

系统首次启动时会自动初始化以下账号：

| 端 | 登录方式 | 账号 | 密码 |
|------|---------|------|------|
| 管理后台 | 账号密码 | `superadmin` | `SuperAdmin@2026` |
| App / 小程序 | 账号密码 | `test` | `123456` |
| 小程序 | 微信登录 | 通过微信授权自动创建 | - |

> 管理后台地址：`http://localhost:8000/admin/login`

### 环境变量 `.env`

| 变量 | 说明 | 示例 |
|------|------|------|
| `DATABASE_URL` | 数据库连接 | `sqlite:///./doumen.db` |
| `SECRET_KEY` | JWT 签名密钥 | 随机字符串 |
| `WECHAT_APPID` | 微信小程序 AppID | `wxXXXXXXXX` |
| `WECHAT_SECRET` | 微信小程序 Secret | |
| `WECHAT_IOS_APPID` | iOS 微信开放平台 AppID | |
| `WECHAT_IOS_SECRET` | iOS 微信开放平台 Secret | |
| `ADMIN_INIT_USERNAME` | 初始超管账号 | `superadmin` |
| `ADMIN_INIT_PASSWORD` | 初始超管密码 | `SuperAdmin@2026` |

### API 路由

| 路径 | 方法 | 说明 |
|------|------|------|
| `/api/health` | GET | 健康检查 |
| `/api/auth/login` | POST | 管理员登录 |
| `/api/auth/me` | GET | 当前管理员信息 |
| `/api/users/login/password` | POST | 用户账号密码登录 |
| `/api/users/register` | POST | 用户注册 |
| `/api/users/{id}/profile` | GET | 用户资料 |
| `/api/users/{id}/bookings` | GET | 用户预约列表 |
| `/api/users/{id}/bookings/{bid}/cancel` | POST | 用户取消预约 |
| `/api/venues` | GET | 场地列表 |
| `/api/venues/{id}` | GET | 场地详情 |
| `/api/teams` | GET / POST | 团队列表 / 创建 |
| `/api/teams/{id}` | GET | 团队详情 |
| `/api/teams/{id}/join` | POST | 加入团队 |
| `/api/teams/{id}/leave` | POST | 退出团队 |
| `/api/bookings` | POST | 提交预约 |
| `/api/notifications/published` | GET | 已发布通知 |
| `/api/admin/*` | CRUD | 管理员管理 |

> 所有 `/api/*` 接口响应统一包装为 `{ code: 0, message: "ok", data }` 格式。

### 数据模型

- **AdminUser**：管理员（super_admin / admin / operator 三级权限）
- **User**：用户（openid / unionid / 昵称 / 手机号）
- **Team**：团队（名称 / 简介 / 上限 80 人）
- **TeamMember**：团队成员（leader / vice_leader / member）
- **Venue**：场地（名称 / 图片 / 描述 / 容量 / 面积 / 设施）
- **TimeSlot**：时间段（日期 / 起止时间 / 上午/下午）
- **Booking**：预约（用户 / 团队 / 场地 / 时段 / 状态 / 是否特权）
- **Notification**：首页通知
- **DrawRecord**：抽签记录
- **CancelRecord**：取消记录
- **SystemConfig**：系统配置

### 业务规则

- 每团队每 2 周最多成功预约 2 次
- 每次预约最多选 4 个时间段
- 预约后进入待抽签状态，管理员执行自动抽签或特权抽签
- 超级管理员每日 4 次特权抽签，管理员 2 次，操作员 1 次
- 抽中后 30 天内取消不得超过 2 次，超限则禁止 2 周内再预约
- 周一闭馆，不开放预约

---

## 二、Flutter App `Flutter/`

跨平台移动应用，支持 Android、iOS、Web、桌面端。

### 技术栈

| 组件 | 技术 |
|------|------|
| 框架 | Flutter 3.x |
| 状态管理 | Provider |
| 网络请求 | Dio |
| 本地存储 | SharedPreferences |
| 国际化 | intl |
| 图片缓存 | cached_network_image |

### 快速启动

```bash
cd Flutter

# 获取依赖
flutter pub get

# 运行（指定设备）
flutter devices                    # 查看可用设备
flutter run -d <device_id>         # 运行到指定设备

# 常用设备 ID
# Android 模拟器：emulator-5554
# iOS 模拟器：通过 flutter devices 查看
```

### 项目结构

```
Flutter/lib/
├── main.dart                      # 应用入口
├── models/models.dart             # 数据模型
├── providers/                     # 状态管理
│   ├── auth_provider.dart         # 认证状态
│   ├── bookings_provider.dart     # 预约状态
│   ├── profile_provider.dart      # 个人资料
│   ├── team_provider.dart         # 团队状态
│   └── venue_provider.dart        # 场地状态
├── screens/                       # 页面
│   ├── login_screen.dart          # 登录
│   ├── register_screen.dart       # 注册
│   ├── home_screen.dart           # 首页
│   ├── main_tab_screen.dart       # 主 Tab
│   ├── venue_detail_screen.dart   # 场馆详情/预约
│   ├── team_screen.dart           # 团队
│   ├── bookings_screen.dart       # 预约历史
│   └── profile_screen.dart        # 个人中心
├── services/                      # 服务层
│   ├── api_service.dart           # API 封装
│   └── auth_service.dart          # 认证服务
├── utils/                         # 工具
│   ├── constants.dart             # 常量（含 baseUrl）
│   └── date_utils.dart            # 日期工具
└── widgets/                       # 通用组件
```

### API 地址配置

`lib/utils/constants.dart` 中根据平台自动选择 API 地址：

```dart
static final String baseUrl = kIsWeb
    ? 'http://localhost:8000/api'
    : (Platform.isAndroid
        ? 'http://10.0.2.2:8000/api'   // Android 模拟器
        : 'http://localhost:8000/api'); // iOS 模拟器 / Web
```

> Android 真机需将 `10.0.2.2` 改为电脑的局域网 IP（如 `192.168.x.x`），并确保后端以 `--host 0.0.0.0` 启动。

### iOS 注意事项

iOS 默认禁止 HTTP 请求，需在 `ios/Runner/Info.plist` 中配置 ATS 例外（已配置）：

```xml
<key>NSAppTransportSecurity</key>
<dict>
    <key>NSAllowsArbitraryLoads</key>
    <true/>
</dict>
```

---

## 三、微信小程序 `Miniapp/`

### 页面结构

| 页面 | 路径 | Tab | 说明 |
|------|------|-----|------|
| 首页 | `pages/index/index` | 是 | 通知横幅 + 场馆列表 |
| 团队 | `pages/team/team` | 是 | 创建/加入/管理团队 |
| 预约 | `pages/bookings/bookings` | 是 | 预约历史列表 |
| 我的 | `pages/profile/profile` | 是 | 个人中心 / 登录 |
| 场馆详情 | `pages/venue-detail/venue-detail` | 否 | 日期选择 + 时段选择 + 预约 |
| 文化中心 | `pages/culture-center/culture-center` | 否 | 文化中心介绍页 |

### 开发注意事项

1. 修改 `app.js` 中 `globalData.apiBase` 为实际 API 地址
2. 修改 `project.config.json` 中 `appid` 为实际小程序 AppID
3. 在微信公众平台配置服务器域名白名单

---

## 四、iOS 原生 App `iOS/`

### 技术栈

| 组件 | 技术 |
|------|------|
| 框架 | SwiftUI |
| 最低版本 | iOS 16.0 |
| 依赖管理 | CocoaPods / SPM |

### 项目结构

```
iOS/MiniApp/
├── DoumenCultureApp.swift         # 主入口
├── Models/Models.swift            # 数据模型
├── Services/
│   ├── APIService.swift           # API 服务层
│   └── WeChatAuthService.swift    # 微信 OAuth 认证
├── ViewModels/AppState.swift      # 全局状态管理
└── Views/                         # 页面视图
```

---

## 五、设计系统

### 色彩体系

| 变量 | 色值 | 用途 |
|------|------|------|
| `primary` | `#3d8e7a` | 主色（按钮、选中态、导航栏） |
| `primaryDark` | `#2e6e5e` | 主色深色（按下态） |
| `background` | `#faf8f6` | 背景色（贝壳白） |
| `card` | `#ffffff` | 卡片背景 |
| `border` | `#ebe3da` | 边框色 |
| `text` | `#211c18` | 正文色 |
| `muted` | `#8c7b6a` | 次要文字 |
| `accent` | `#e8a840` | 强调色（渔灯金） |

### 状态颜色

| 状态 | 文字色 | 背景色 |
|------|--------|--------|
| 待抽签 | `#e8a840` | `#fdf6e8` |
| 已中签 | `#4caf7d` | `#e8f5ee` |
| 未中签 | `#8c7b6a` | `#f5f0eb` |
| 已取消 | `#d4644a` | `#fdf0ed` |

---

## 六、部署建议

### 后台部署

```bash
# 使用 uvicorn
uvicorn app.main:app --host 0.0.0.0 --port 8000 --workers 4

# 或使用 Docker
docker build -t zhuhai-art-backend .
docker run -d -p 8000:8000 --env-file .env zhuhai-art-backend
```

### 数据库

- 开发环境：SQLite
- 生产环境：建议切换 PostgreSQL，修改 `DATABASE_URL` 即可

---

## 七、开发环境要求

| 工具 | 版本要求 |
|------|---------|
| Python | 3.10+ |
| Flutter | 3.19+ |
| Dart | 3.3+ |
| Node.js | 16+（小程序工具） |
| Xcode | 15+（iOS 开发） |
| Android Studio | Hedgehog+（Android 开发） |

---

## 八、项目文档

可视化架构图见 [`docs/architecture.md`](docs/architecture.md)，包含系统架构、预约调用链路、数据模型 ER 图、后端路由模块、 Flutter 层级结构（GitHub/Gitee 原生渲染 Mermaid）。

详细文档见 `项目文档/` 目录：

- `需求文档.md` — 产品需求
- `产品文档.md` — 产品设计
- `技术说明文档.md` — 技术架构
- `UI_UX设计文档.md` — UI/UX 设计规范
- `逻辑流程图.md` — 业务流程图
- `部署操作手册.md` — 部署运维手册
