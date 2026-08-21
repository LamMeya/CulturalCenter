# 斗门文化中心 · 场地预约系统

广东珠海斗门区文化中心场地预约平台，面向文艺团队提供场地预约、抽签分配、团队管理等功能。覆盖 **管理后台（Web）**、**用户端（微信小程序）**、**用户端（iOS App）** 三端。

---

## 项目结构

```
├── doumen-culture-backend/     # Python 后台（FastAPI + SQLAlchemy）
├── doumen-culture-miniapp/     # 微信小程序
├── doumen-culture-ios/         # iOS App（SwiftUI）
├── doumen-culture-admin/       # 管理后台页面设计稿（参考用）
├── doumen-culture-app/         # 用户端页面设计稿（参考用）
└── README.md                   # 本文件
```

---

## 一、后台服务 `doumen-culture-backend/`

### 技术栈

| 组件 | 技术 |
|------|------|
| 框架 | FastAPI 0.104 |
| ORM | SQLAlchemy 2.0 |
| 数据库 | SQLite（开发）/ 可切换 PostgreSQL |
| 认证 | JWT（python-jose） |
| 密码 | bcrypt（passlib） |
| 模板引擎 | Jinja2（管理后台页面） |

### 快速启动

```bash
cd doumen-culture-backend

# 安装依赖
pip install -r requirements.txt

# 配置环境变量
cp .env.example .env
# 编辑 .env 填入微信 AppID / Secret 等

# 启动服务
python -m app.main
# 访问 http://localhost:8000
```

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
| `/api/venues` | GET / POST | 场地列表 / 创建 |
| `/api/venues/{id}` | GET / PUT / DELETE | 场地详情 / 更新 / 删除 |
| `/api/venues/{id}/upload-image` | POST | 上传场地图片 |
| `/api/bookings` | GET | 预约列表（管理员） |
| `/api/bookings/auto-draw` | POST | 自动抽签 |
| `/api/bookings/privilege-draw/{id}` | POST | 特权抽签 |
| `/api/bookings/{id}/cancel` | POST | 管理员取消预约 |
| `/api/bookings/stats` | GET | 预约统计 |
| `/api/teams` | GET / POST | 团队列表 / 创建 |
| `/api/teams/{id}` | GET | 团队详情 |
| `/api/teams/{id}/join` | POST | 加入团队 |
| `/api/teams/{id}/leave` | POST | 退出团队 |
| `/api/teams/{id}/booking-stats` | GET | 团队预约统计 |
| `/api/users/login/miniprogram` | POST | 小程序登录 |
| `/api/users/login/ios` | POST | iOS 微信登录 |
| `/api/users/{id}/profile` | GET | 用户资料 |
| `/api/users/{id}/bookings` | GET | 用户预约列表 |
| `/api/users/{id}/bookings/{bid}/cancel` | POST | 用户取消预约 |
| `/api/system/config` | GET / PUT | 系统配置 |
| `/api/system/configs` | GET | 所有配置项 |
| `/api/admin/*` | CRUD | 管理员管理 |

### 数据模型

- **AdminUser**：管理员（super_admin / admin / operator 三级权限）
- **User**：微信用户（openid / unionid / 昵称 / 手机号）
- **Team**：团队（名称 / 简介 / 上限 80人）
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

## 二、微信小程序 `doumen-culture-miniapp/`

### 页面结构

| 页面 | 路径 | Tab | 说明 |
|------|------|-----|------|
| 首页 | `pages/index/index` | 是 | 通知横幅 + 场馆列表 |
| 团队 | `pages/team/team` | 是 | 创建/加入/管理团队 |
| 预约 | `pages/bookings/bookings` | 是 | 预约历史列表 |
| 我的 | `pages/profile/profile` | 是 | 个人中心 / 登录 |
| 场馆详情 | `pages/venue-detail/venue-detail` | 否 | 日期选择 + 时段选择 + 预约 |
| 文化中心 | `pages/culture-center/culture-center` | 否 | 文化中心介绍页 |

### 登录流程

1. 调用 `wx.login()` 获取 code
2. 调用 `wx.getUserProfile()` 获取昵称和头像
3. 将 code + nickname 发送到后端 `POST /api/users/login/miniprogram`
4. 后端通过 code 换取 openid，创建/更新用户
5. 手机号绑定通过 `button open-type="getPhoneNumber"` 获取加密数据，发送到后端解密

### 工具模块

`utils/api.js` 封装了 `wx.request`，提供 `get()` / `post()` / `put()` / `del()` 方法，自动携带 Token。

### 开发注意事项

1. 修改 `app.js` 中 `globalData.apiBase` 为实际 API 地址
2. 修改 `project.config.json` 中 `appid` 为实际小程序 AppID
3. Tab 图标需要准备 8 张图片放到 `images/` 目录（tab-home, tab-home-active, tab-team, tab-team-active, tab-bookings, tab-bookings-active, tab-profile, tab-profile-active）
4. 在微信公众平台配置服务器域名白名单

---

## 三、iOS App `doumen-culture-ios/`

### 技术栈

| 组件 | 技术 |
|------|------|
| 框架 | SwiftUI |
| 最低版本 | iOS 16.0 |
| 依赖管理 | CocoaPods / SPM |
| 微信 SDK | WechatOpenSDK（通过 Podfile） |

### 项目结构

```
doumen-culture-ios/
├── DoumenCultureApp.swift         # 主入口
├── Info.plist                     # 微信 URL Scheme 配置
├── Podfile                        # CocoaPods 依赖
├── Models/Models.swift            # 数据模型
├── Services/
│   ├── APIService.swift           # API 服务层
│   └── WeChatAuthService.swift    # 微信 OAuth 认证
├── ViewModels/AppState.swift      # 全局状态管理
└── Views/
    ├── LoginView.swift            # 登录页
    ├── MainTabView.swift          # 主 Tab 视图
    ├── HomeView.swift             # 首页
    ├── VenueDetailView.swift      # 场馆详情与预约
    ├── TeamView.swift             # 团队管理
    ├── BookingsView.swift         # 预约历史
    └── ProfileView.swift          # 个人中心
```

### 微信登录流程

1. 用户点击"微信登录"
2. `WeChatAuthService` 调用 `sendAuthRequest` 唤起微信
3. 微信返回 auth code
4. 将 code 发送到后端 `POST /api/users/login/ios`
5. 后端通过 code 换取 access_token 和 openid
6. 获取用户信息，返回 token 和用户数据

### 开发注意事项

1. 在微信开放平台注册 iOS 应用，获取 AppID 和 Universal Link
2. 修改 `Info.plist` 中的 `CFBundleURLSchemes` 为实际微信 AppID
3. 配置 `LSApplicationQueriesSchemes` 包含 `weixin` 和 `weixinULAPI`
4. 修改 `APIService.swift` 中 `baseURL` 为实际 API 地址
5. 开发阶段可使用 `WeChatAuthService.mockLogin()` 跳过微信 SDK 集成测试

---

## 四、设计系统

### 色彩体系

| 变量 | 色值 | 用途 |
|------|------|------|
| `--dm-primary` | `#3d8e7a` | 主色（按钮、选中态、导航栏） |
| `--dm-primary-dark` | `#2e6e5e` | 主色深色（按下态） |
| `--dm-bg` | `#faf8f6` | 背景色（贝壳白） |
| `--dm-card` | `#ffffff` | 卡片背景 |
| `--dm-border` | `#ebe3da` | 边框色 |
| `--dm-text` | `#211c18` | 正文色 |
| `--dm-muted` | `#8c7b6a` | 次要文字 |
| `--dm-accent` | `#e8a840` | 强调色（渔灯金） |

### 状态颜色

| 状态 | 文字色 | 背景色 |
|------|--------|--------|
| 待抽签 | `#e8a840` | `#fdf6e8` |
| 已中签 | `#4caf7d` | `#e8f5ee` |
| 未中签 | `#8c7b6a` | `#f5f0eb` |
| 已取消 | `#d4644a` | `#fdf0ed` |

### 圆角

- 卡片：16rpx（小程序）/ 12pt（iOS）
- 小元素：8rpx（小程序）/ 8pt（iOS）

---

## 五、部署建议

### 后台部署

```bash
# 使用 Docker
docker build -t doumen-culture-backend .
docker run -d -p 8000:8000 --env-file .env doumen-culture-backend

# 或使用 uvicorn + nginx 反向代理
uvicorn app.main:app --host 0.0.0.0 --port 8000 --workers 4
```

### 数据库

- 开发环境：SQLite
- 生产环境：建议切换 PostgreSQL，修改 `DATABASE_URL` 即可

### 文件存储

- 场地图片上传到 `app/static/uploads/venues/`
- 生产环境建议使用 OSS（阿里云/腾讯云对象存储）

---

## 六、接口人

项目开发过程中如有疑问，请参考各子项目的源码注释或联系项目负责人。