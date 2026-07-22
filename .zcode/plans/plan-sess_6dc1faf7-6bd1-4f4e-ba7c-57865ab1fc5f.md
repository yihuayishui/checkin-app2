## 修复任务编辑通知逻辑

### 问题
任务 `66777888` 是 bbb 创建给 aaa 的（creator_id=bbb, user_id=aaa）。当 aaa 编辑该任务时，`PUT /update` 的当前条件 `existing.user_id !== req.userId` 判断为 false（aaa === aaa），所以不发送通知，bbb 收不到。

### 修复
不再只比对 `user_id`，而是判断「编辑者以外的那一方」来通知：

```javascript
const notifyUserId = existing.user_id === req.userId ? existing.creator_id : existing.user_id;
```

各场景：
| 场景 | user_id | creator_id | 编辑者 | notifyUserId | 通知去向 |
|------|---------|------------|--------|-------------|---------|
| bbb 编辑给 aaa 的任务 | aaa | bbb | bbb | aaa | ✅ aaa |
| aaa 编辑 bbb 创建的任务 | aaa | bbb | aaa | bbb | ✅ bbb |
| aaa 编辑自己的任务 | aaa | aaa | aaa | aaa | ❌ 不通知 |

### 部署
修改 `task-route.js` 后上传到服务器再 `pm2 restart my-backend-v2`。
