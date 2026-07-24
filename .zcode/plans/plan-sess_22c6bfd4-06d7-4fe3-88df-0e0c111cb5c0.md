## 头像上传实现计划

### 现状
- **后端 API 已就绪**：`POST /api/v1/upload/image`（文件上传）+ `PUT /api/v1/user/avatar`（保存 URL），静态文件服务已配置
- **Coil 依赖**：`io.coil-kt:coil-compose:2.5.0` 已在 `build.gradle.kts` 中但从未使用
- **Avatar 显示**：Profile 页和首页顶部都用 `AccountCircle` 图标占位，没有实际头像
- **ApiService**：`updateAvatar()` 已声明但从未被调用；文件上传接口不存在

### 实施步骤

#### 1. ApiService.kt — 添加图片上传接口
新增 Retrofit `@Multipart` 方法 `uploadImage()`，对应 `POST /api/v1/upload/image`，接收 `MultipartBody.Part` 返回 URL。

#### 2. AppViewModel.kt — 添加头像上传逻辑
- 新增 `_avatarUrl: MutableStateFlow<String?>` 和 `avatarUrl: StateFlow<String?>`
- 新增 `updateAvatarFromUri(uri: Uri)` 方法：
  1. 从 URI 读文件内容 → 构造 `MultipartBody.Part`
  2. 调用 `uploadImage()` → 获取服务器返回的 URL
  3. 调用 `updateAvatar()` → 保存到用户记录
  4. 更新 `_avatarUrl`
- 在 `init` / `login` / `register` 时从 API 拉取 `avatarUrl` 填充 `_avatarUrl`

#### 3. ProfileScreens.kt — 替换头像 UI
- 用 Coil `AsyncImage` 替换 `Icon(Icons.Filled.AccountCircle)`
- 头像增加 `.clickable` → 启动系统图片选择器（`ActivityResultContracts.GetContent`）
- 选中图片后调 `viewModel.updateAvatarFromUri(uri)`
- 加载中显示 CircularProgressIndicator

#### 4. HomeScreen.kt — 首页顶部头像
- 首页 TopAppBar 右侧"我的"按钮，从 `AccountCircle` 图标改为 Coil `AsyncImage`（带 placeholder）

#### 5. PairScreen.kt — 搭档头像（可选）
- 搭档卡片和搜索结果中显示头像（如果有的话）

### 涉及文件
| 文件 | 改动 |
|------|------|
| `network/api/ApiService.kt` | 添加 `@Multipart uploadImage()` 方法 |
| `network/dto/ApiModels.kt` | 添加 `ImageUploadResult` DTO |
| `viewmodel/AppViewModel.kt` | 添加 `_avatarUrl` + `updateAvatarFromUri()` |
| `ui/profile/ProfileScreens.kt` | 头像显示 + 图片选择 + 上传触发 |
| `ui/home/HomeScreen.kt` | 首页顶部头像 |
| `ui/pair/PairScreen.kt` | 搭档头像（可选） |

### 注意事项
- 图片选择使用 `ActivityResultContracts.GetContent()` 标准 API，无需额外权限（读取已选图片无需权限）
- Coil 已添加依赖，直接使用 `AsyncImage` 不需要额外初始化
- 上传使用 OkHttp 的 `RequestBody` + `MultipartBody`，Retrofit 原生支持
- 服务端不需要任何改动