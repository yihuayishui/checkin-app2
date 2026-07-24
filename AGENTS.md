# 打卡搭档 — Checkin Partner

情侣/搭档之间的双向打卡激励 App。一方打卡后，实时同步给另一方。

## 项目结构

```
D:\project\checkin-app2\
├── server/              ← Node.js + Express 后端
│   ├── package.json
│   └── src/
│       ├── index.js         # 入口（HTTP + Socket.IO）
│       ├── app.js           # Express 应用
│       ├── config.js        # 配置（端口 3001）
│       ├── api/v1/          # REST 路由
│       ├── middleware/      # JWT 认证 + 错误处理
│       ├── store/           # better-sqlite3 数据库
│       │   ├── db-init.js
│       │   └── tables/      # 每张表一个模块
│       ├── ws/              # Socket.IO 事件处理
│       └── utils/           # FCM 推送等工具
├── android/             ← Kotlin + Jetpack Compose 前端
│   ├── build.gradle.kts
│   ├── settings.gradle.kts
│   └── app/src/main/java/com/checkin/partner/
│       ├── CheckinApp.kt / MainActivity.kt
│       ├── data/           # Room Entity/DAO/DB/Repository
│       ├── network/        # Retrofit + WebSocket + FCM + DTO
│       └── ui/             # Theme / NavGraph / Auth / Home / Task / Reward / Profile / Checkin
│       ├── viewmodel/      # AppViewModel（全局状态管理）
│       └── utils/          # 工具（ReminderWorker）
└── AGENTS.md
```

## 运行命令

### 后端（服务器）
```bash
# 服务器 IP: 198.46.252.178（root / [REDACTED]）
# 后端路径: /var/www/checkin-server-v2/server/
cd /var/www/checkin-server-v2/server
npm start            # node src/index.js
pm2 list             # 查看进程
pm2 restart my-backend-v2
pm2 logs my-backend-v2
```

### 前端（Windows 本地）
用 Android Studio 打开 `D:\project\checkin-app2\android`，Sync Gradle → Run。

## 技术栈

| 层级 | 技术 |
|------|------|
| Android 前端 | Kotlin + Jetpack Compose |
| 本地数据库 | Room (SQLite), 版本号 5（fallbackToDestructiveMigration） |
| HTTP | Retrofit + OkHttp |
| 实时通信 | OkHttp WebSocket（Socket.IO 协议简化版） |
| 状态管理 | ViewModel + StateFlow / SharedFlow / mutableStateListOf |
| 后端 | Node.js + Express (端口 3001) |
| 后端数据库 | better-sqlite3 (checkin-v2.db) |
| 实时推送 | Socket.IO |
| 离线推送 | FCM HTTP v1（国内不通，备用本地通知） |

## 通知系统（当前完整方案）

| 方式 | App 前台 | App 后台/被杀 |
|------|---------|-------------|
| WebSocket 实时推送 → `showLocalNotification()` | ✅ | ❌ 断连 |
| 冷启动/切回首页时 `refreshNotifications(showAlert=true)` 拉取未读 → 弹系统栏 | ✅ | ✅ 打开 App 时补弹 |
| WorkManager 定时打卡提醒 | ✅ | ✅ |
| FCM 推送 | ❌ 国内不通 | ❌ 国内不通 |

### 通知去重与已读
- `shownNotificationIds` 持久化到 SharedPreferences（存为逗号分隔字符串），已弹过的通知不重复弹
- 用户**点击**系统通知 → 跳转对应页面 + 标记已读
- 用户**滑出**系统通知 → 标记已读
- App 内通知中心可手动标已读/清空 |

## 架构要点

### 数据流
```
Android Room ←→ Retrofit/WS → Node.js Express → better-sqlite3
```

- 服务端是权威数据源，Room 只做缓存
- 增量同步用 `?since=` 时间戳（UTC ISO 8601）
- WebSocket 透传 JSON，不落库（仅转发）

### 数据库表（12 张）
`user`, `pair`, `task`, `checkin_record`, `checkin_comment`, `reward`, `point_transaction`, `makeup_card`, `notification`, `achievement`, `user_config`, `guide_status`

### API 前缀
`/api/v1/` + 功能路由

### Room
- 实体类用 `@ColumnInfo(name = "xxx")` 映射数据库列名（下划线格式）
- DAO 中 SQL 查询**必须用数据库列名**（如 `user_id`、`created_at`），不用 Kotlin 属性名
- 版本变更时用 `fallbackToDestructiveMigration()`，需手动递增 `version`

## 关键约定

### 命名
- SQL 列名：蛇形（`user_id`, `created_at`）
- Kotlin 属性：驼峰（`userId`, `createdAt`）
- DTO 字段：@SerializedName("列名") 对应后端 JSON

### Retrofit 类型约束
- `Map<String, Any?>` **不能用**，Retrofit 不支持类型变量/通配符
- 用 `Map<String, @JvmSuppressWildcards Any>` 替代
- 或直接用具体类型 `Map<String, String>`

### 底部导航
主页面（首页/任务/奖励/我的）有固定底部导航栏，子页面（设置/创建/详情）没有。

### 搭档确认逻辑
涉及对方数据变更的操作需对方确认：
- 删除搭档创建的任务 → 发请求 → 对方同意才删
- 编辑搭档创建的任务 → 发请求 → 对方同意才生效
- 删除奖励 → 发请求 → 对方同意才删
- 兑换奖励 → 申请 → 对方同意 → 扣积分 → 兑现 → 双方确认

### 通知
- FATAL 通知显示为系统通知栏消息
- 其他通知推送到通知中心，App 打开时自动拉取
- 定时打卡提醒通过 WorkManager 调度，无需后端参与

## 后端部署

| 服务 | 端口 | PM2 名称 | 数据库 |
|------|------|----------|--------|
| 旧后端 | 3000 | my-backend | checkin.db (sql.js) |
| 新后端 | 3001 | my-backend-v2 | checkin-v2.db (better-sqlite3) |

- 旧后端（3000）不动，新后端（3001）并行运行
- ufw 防火墙已开放 3001 端口
- FCM 服务帐号文件: `service-account.json`

## Android 开发注意事项

- Compose BOM: `2023.10.01`（兼容 Kotlin 1.9.22 / Compose compiler 1.5.10）
- `Card(onClick = ...)` 不支持 Material3 低版本，用 `Card(Modifier.clickable { })` 替代
- `HorizontalDivider` 不可用，用 `Box(Modifier.fillMaxWidth().height(1.dp).background(...))` 替代
- `Tab` 的 `tabIndicatorOffset` 在 BOM 2023.10.01 中不可用，用文字颜色/加粗区分选中态
- 阿里云 / 华为云 / 中科大 Gradle 镜像源可用，官方源需代理
- 国内 FCM 推送不通，备用本地通知方案可用
- 通知冷启动拉取在 `AppViewModel.init` 中执行 `refreshNotifications(showAlert=true)`
- 首页 `LaunchedEffect` 中同时调 `refreshDashboard()` + `refreshNotifications(showAlert=true)`
- 下拉刷新使用 Material `pullRefresh`（需添加 `androidx.compose.material:material` 依赖）
- `PullRefreshIndicator` 与 `LazyColumn` 在同一个 `Box(pullRefresh)` 内平行放置

## 已知坑点 / 踩坑记录

### 后端时区
服务器（198.46.252.178）时间为 UTC，所有时间函数（`now()`、`today()`、`isInTimeWindow()`）已改为北京时间 UTC+8 计算。见 `helper.js`。

### 任务通知的接收方判断
`PUT /update` 和 `POST /edit-request/:taskId` 需要通知"编辑者以外的那一方"：
```javascript
const notifyUserId = existing.user_id === req.userId ? existing.creator_id : existing.user_id;
```
即：如果编辑者是执行人 → 通知创建者；编辑者是创建者 → 通知执行者。

### 一次性任务首页展示
一次性任务打卡后状态标记为 `DONE`，但看板 API（`dashboard-route.js`）会额外把当天完成的一次性任务合并进列表，确保当天仍然展示（按钮显示"已打卡"），第二天自动消失。

### Room 缓存与多 filter 冲突
`refreshTasks(filter)` 网络失败时，只有 `mine` 和 `created` 回退到 Room 缓存（`db.taskDao().getByUserId`）。
`for_partner` / `from_partner` / `done` 失败时不覆盖列表（保留上次数据）。

### `MutableStateFlow` + Equals 不 emit
`MutableStateFlow` 用 `equals()` 比较新旧值。若实体类所有字段相等，即使生成新对象也不 emit。
**解决**：奖励列表用 `_rewardVersion` 计数器触发刷新，或用 `mutableStateListOf`。

### Dao SQL 必须用数据库列名
Room `@Query` 中的 WHERE/SET 子句必须用蛇形列名（`user_id`、`created_at`），不是 Kotlin 驼峰属性名。

### Retrofit `Map<String, Any?>` 不能用
必须用 `Map<String, @JvmSuppressWildcards Any>`，否则运行时崩溃。

### 导航回弹导致协程取消
```kotlin
viewModel.createTask(...)      // 异步
navController.popBackStack()   // 立即执行，协程被取消
```
应等回调：`viewModel.createTask(..., onDone = { popBackStack() })`

## 账号（测试用）

| 用户名 | 密码 |
|--------|------|
| test001 | 123456 |
| aaa | 123456 |
| bbb | 123456 |
