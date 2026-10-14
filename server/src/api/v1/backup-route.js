// 数据库备份路由（供路由器 cron 定期拉取，异地备份）
const { Router } = require('express');
const fs = require('fs');
const os = require('os');
const path = require('path');
const zlib = require('zlib');
const config = require('../../config');
const { getDb } = require('../../store/db-init');

const router = Router();

// 备份密钥：优先环境变量，默认值与五菱备份同风格（内网/隧道专用，不走用户 JWT）
const BACKUP_KEY = process.env.BACKUP_KEY || 'checkin-backup-key-2026';

/**
 * 下载数据库快照（gzip 压缩）
 *
 * 用 better-sqlite3 的 backup() 在线备份，而不是直接读文件：
 * 数据库运行在 WAL 模式，最新数据还在 -wal 文件里，直接拷 .db 会拿到不完整的数据。
 */
router.get('/db', async (req, res) => {
  if (req.headers['x-api-key'] !== BACKUP_KEY) {
    return res.status(401).json({ code: 401, message: '备份密钥无效' });
  }

  const tmpFile = path.join(os.tmpdir(), `checkin-backup-${Date.now()}.db`);
  try {
    const db = getDb();
    await db.backup(tmpFile);

    const buf = fs.readFileSync(tmpFile);
    const gz = zlib.gzipSync(buf, { level: 9 });

    const stamp = new Date().toISOString().slice(0, 10).replace(/-/g, '');
    res.setHeader('Content-Type', 'application/gzip');
    res.setHeader('Content-Disposition', `attachment; filename=checkin-${stamp}.db.gz`);
    res.setHeader('X-Backup-Size', String(buf.length));
    res.send(gz);
    console.log(`[BACKUP] 导出成功 ${buf.length}B -> gzip ${gz.length}B`);
  } catch (err) {
    console.error('[BACKUP] 失败:', err.message);
    if (!res.headersSent) res.status(500).json({ code: 500, message: '备份失败: ' + err.message });
  } finally {
    try { fs.unlinkSync(tmpFile); } catch (_) {}
  }
});

module.exports = router;
