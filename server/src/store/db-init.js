// 数据库初始化（better-sqlite3）
const Database = require('better-sqlite3');
const path = require('path');
const fs = require('fs');
const config = require('../config');

let db = null;

/**
 * 获取数据库实例
 */
function getDb() {
  if (!db) throw new Error('数据库尚未初始化，请先调用 initDatabase()');
  return db;
}

/**
 * 初始化数据库并建表
 */
function initDatabase() {
  const dbDir = path.dirname(config.DB_PATH);
  if (!fs.existsSync(dbDir)) {
    fs.mkdirSync(dbDir, { recursive: true });
  }

  db = new Database(config.DB_PATH);

  // 性能与安全
  db.pragma('journal_mode = WAL');
  db.pragma('foreign_keys = ON');
  db.pragma('synchronous = NORMAL');

  createTables();

  console.log(`[DB] 数据库初始化完成: ${config.DB_PATH}`);
  return db;
}

/**
 * 创建所有表
 */
function createTables() {
  // ── 用户表 ──
  db.exec(`
    CREATE TABLE IF NOT EXISTS user (
      id              TEXT PRIMARY KEY,
      username        TEXT NOT NULL UNIQUE,
      password        TEXT NOT NULL,
      avatar_url      TEXT,
      personal_points INTEGER NOT NULL DEFAULT 0,
      pool_points     INTEGER NOT NULL DEFAULT 0,
      is_vacation     INTEGER NOT NULL DEFAULT 0,
      fcm_token       TEXT,
      created_at      TEXT NOT NULL DEFAULT (datetime('now')),
      updated_at      TEXT NOT NULL DEFAULT (datetime('now'))
    )
  `);

  // ── 搭档关系表 ──
  db.exec(`
    CREATE TABLE IF NOT EXISTS pair (
      id                TEXT PRIMARY KEY,
      user_a            TEXT NOT NULL,
      user_b            TEXT NOT NULL,
      status            TEXT NOT NULL DEFAULT 'PENDING',
      requested_by      TEXT,
      unbind_requested_by TEXT,
      created_at        TEXT NOT NULL DEFAULT (datetime('now')),
      updated_at        TEXT NOT NULL DEFAULT (datetime('now')),
      FOREIGN KEY (user_a) REFERENCES user(id),
      FOREIGN KEY (user_b) REFERENCES user(id)
    )
  `);

  // ── 任务表 ──
  db.exec(`
    CREATE TABLE IF NOT EXISTS task (
      task_id         TEXT PRIMARY KEY,
      user_id         TEXT NOT NULL,
      creator_id      TEXT NOT NULL,
      name            TEXT NOT NULL,
      frequency       TEXT NOT NULL DEFAULT 'DAILY',
      point_per_check INTEGER NOT NULL DEFAULT 10,
      start_time      TEXT,
      end_time        TEXT,
      is_active       INTEGER NOT NULL DEFAULT 1,
      status          TEXT NOT NULL DEFAULT 'ACTIVE',
      created_at      TEXT NOT NULL DEFAULT (datetime('now')),
      updated_at      TEXT NOT NULL DEFAULT (datetime('now')),
      FOREIGN KEY (user_id) REFERENCES user(id),
      FOREIGN KEY (creator_id) REFERENCES user(id)
    )
  `);

  // ── 打卡记录表 ──
  db.exec(`
    CREATE TABLE IF NOT EXISTS checkin_record (
      id            INTEGER PRIMARY KEY AUTOINCREMENT,
      record_id     TEXT NOT NULL UNIQUE,
      task_id       TEXT NOT NULL,
      user_id       TEXT NOT NULL,
      checkin_time  TEXT NOT NULL,
      note          TEXT,
      image_url     TEXT,
      is_makeup     INTEGER NOT NULL DEFAULT 0,
      created_at    TEXT NOT NULL DEFAULT (datetime('now')),
      FOREIGN KEY (task_id) REFERENCES task(task_id),
      FOREIGN KEY (user_id) REFERENCES user(id)
    )
  `);

  // ── 打卡留言表 ──
  db.exec(`
    CREATE TABLE IF NOT EXISTS checkin_comment (
      id            INTEGER PRIMARY KEY AUTOINCREMENT,
      record_id     TEXT NOT NULL,
      user_id       TEXT NOT NULL,
      content       TEXT NOT NULL,
      created_at    TEXT NOT NULL DEFAULT (datetime('now')),
      FOREIGN KEY (record_id) REFERENCES checkin_record(record_id),
      FOREIGN KEY (user_id) REFERENCES user(id)
    )
  `);

  // ── 奖励表 ──
  db.exec(`
    CREATE TABLE IF NOT EXISTS reward (
      id              TEXT PRIMARY KEY,
      creator_id      TEXT NOT NULL,
      name            TEXT NOT NULL,
      required_points INTEGER NOT NULL,
      expires_at      TEXT,
      status          TEXT NOT NULL DEFAULT 'ACTIVE',
      applicant_id    TEXT,
      created_at      TEXT NOT NULL DEFAULT (datetime('now')),
      updated_at      TEXT NOT NULL DEFAULT (datetime('now')),
      FOREIGN KEY (creator_id) REFERENCES user(id)
    )
  `);

  // 兼容旧表：添加 pending_delete_by 列
  try { db.exec("ALTER TABLE task ADD COLUMN pending_delete_by TEXT"); } catch(e) {}
  try { db.exec("ALTER TABLE reward ADD COLUMN pending_delete_by TEXT"); } catch(e) {}
  try { db.exec("ALTER TABLE reward ADD COLUMN claim_requested_by TEXT"); } catch(e) {}
  // 任务编辑确认
  try { db.exec("ALTER TABLE task ADD COLUMN pending_edit_by TEXT"); } catch(e) {}
  try { db.exec("ALTER TABLE task ADD COLUMN pending_edit_data TEXT"); } catch(e) {}

  // ── 积分流水表 ──
  db.exec(`
    CREATE TABLE IF NOT EXISTS point_transaction (
      id              INTEGER PRIMARY KEY AUTOINCREMENT,
      transaction_id  TEXT NOT NULL UNIQUE,
      user_id         TEXT NOT NULL,
      partner_id      TEXT,
      amount          INTEGER NOT NULL,
      type            TEXT NOT NULL,
      category        TEXT NOT NULL DEFAULT 'CHECKIN',
      description     TEXT,
      created_at      TEXT NOT NULL DEFAULT (datetime('now')),
      FOREIGN KEY (user_id) REFERENCES user(id)
    )
  `);

  // ── 补签卡表 ──
  db.exec(`
    CREATE TABLE IF NOT EXISTS makeup_card (
      id          INTEGER PRIMARY KEY AUTOINCREMENT,
      user_id     TEXT NOT NULL,
      total_cards INTEGER NOT NULL DEFAULT 2,
      used_cards  INTEGER NOT NULL DEFAULT 0,
      month       TEXT NOT NULL,
      last_updated TEXT NOT NULL DEFAULT (datetime('now')),
      FOREIGN KEY (user_id) REFERENCES user(id)
    )
  `);

  // ── 通知表 ──
  db.exec(`
    CREATE TABLE IF NOT EXISTS notification (
      id          INTEGER PRIMARY KEY AUTOINCREMENT,
      user_id     TEXT NOT NULL,
      type        TEXT NOT NULL,
      title       TEXT NOT NULL,
      body        TEXT,
      related_id  TEXT,
      is_read     INTEGER NOT NULL DEFAULT 0,
      created_at  TEXT NOT NULL DEFAULT (datetime('now')),
      FOREIGN KEY (user_id) REFERENCES user(id)
    )
  `);

  // ── 成就表 ──
  db.exec(`
    CREATE TABLE IF NOT EXISTS achievement (
      id          INTEGER PRIMARY KEY AUTOINCREMENT,
      user_id     TEXT NOT NULL,
      code        TEXT NOT NULL,
      unlocked_at TEXT NOT NULL DEFAULT (datetime('now')),
      FOREIGN KEY (user_id) REFERENCES user(id),
      UNIQUE(user_id, code)
    )
  `);

  // ── 用户设置表 ──
  db.exec(`
    CREATE TABLE IF NOT EXISTS user_config (
      user_id             TEXT PRIMARY KEY,
      reminder_time       TEXT,
      notify_checkin      INTEGER NOT NULL DEFAULT 1,
      notify_reward       INTEGER NOT NULL DEFAULT 1,
      notify_pair         INTEGER NOT NULL DEFAULT 1,
      personal_ratio      REAL NOT NULL DEFAULT 0.5,
      pool_ratio          REAL NOT NULL DEFAULT 0.5,
      streak_penalty_on   INTEGER NOT NULL DEFAULT 0,
      streak_penalty_days INTEGER NOT NULL DEFAULT 0,
      theme_mode          TEXT NOT NULL DEFAULT 'system',
      FOREIGN KEY (user_id) REFERENCES user(id)
    )
  `);

  // ── 引导状态表 ──
  db.exec(`
    CREATE TABLE IF NOT EXISTS guide_status (
      user_id   TEXT PRIMARY KEY,
      is_done   INTEGER NOT NULL DEFAULT 0,
      FOREIGN KEY (user_id) REFERENCES user(id)
    )
  `);

  // ── 索引 ──
  db.exec('CREATE INDEX IF NOT EXISTS idx_pair_user_a ON pair(user_a)');
  db.exec('CREATE INDEX IF NOT EXISTS idx_pair_user_b ON pair(user_b)');
  db.exec('CREATE INDEX IF NOT EXISTS idx_pair_status ON pair(status)');
  db.exec('CREATE INDEX IF NOT EXISTS idx_task_user_id ON task(user_id)');
  db.exec('CREATE INDEX IF NOT EXISTS idx_task_creator ON task(creator_id)');
  db.exec('CREATE INDEX IF NOT EXISTS idx_task_status ON task(status)');
  db.exec('CREATE INDEX IF NOT EXISTS idx_checkin_user ON checkin_record(user_id)');
  db.exec('CREATE INDEX IF NOT EXISTS idx_checkin_task ON checkin_record(task_id)');
  db.exec('CREATE INDEX IF NOT EXISTS idx_checkin_time ON checkin_record(checkin_time)');
  db.exec('CREATE INDEX IF NOT EXISTS idx_reward_creator ON reward(creator_id)');
  db.exec('CREATE INDEX IF NOT EXISTS idx_reward_status ON reward(status)');
  db.exec('CREATE INDEX IF NOT EXISTS idx_point_user ON point_transaction(user_id)');
  db.exec('CREATE INDEX IF NOT EXISTS idx_makeup_user_month ON makeup_card(user_id, month)');
  db.exec('CREATE INDEX IF NOT EXISTS idx_notification_user ON notification(user_id)');
  db.exec('CREATE INDEX IF NOT EXISTS idx_comment_record ON checkin_comment(record_id)');

  console.log('[DB] 12 张表创建/确认完成');
}

module.exports = { initDatabase, getDb };
