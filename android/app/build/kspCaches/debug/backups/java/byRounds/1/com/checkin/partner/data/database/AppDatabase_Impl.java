package com.checkin.partner.data.database;

import androidx.annotation.NonNull;
import androidx.room.DatabaseConfiguration;
import androidx.room.InvalidationTracker;
import androidx.room.RoomDatabase;
import androidx.room.RoomOpenHelper;
import androidx.room.migration.AutoMigrationSpec;
import androidx.room.migration.Migration;
import androidx.room.util.DBUtil;
import androidx.room.util.TableInfo;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.SupportSQLiteOpenHelper;
import com.checkin.partner.data.dao.AchievementDao;
import com.checkin.partner.data.dao.AchievementDao_Impl;
import com.checkin.partner.data.dao.CheckinDao;
import com.checkin.partner.data.dao.CheckinDao_Impl;
import com.checkin.partner.data.dao.ConfigDao;
import com.checkin.partner.data.dao.ConfigDao_Impl;
import com.checkin.partner.data.dao.NotificationDao;
import com.checkin.partner.data.dao.NotificationDao_Impl;
import com.checkin.partner.data.dao.PairDao;
import com.checkin.partner.data.dao.PairDao_Impl;
import com.checkin.partner.data.dao.PointDao;
import com.checkin.partner.data.dao.PointDao_Impl;
import com.checkin.partner.data.dao.RewardDao;
import com.checkin.partner.data.dao.RewardDao_Impl;
import com.checkin.partner.data.dao.TaskDao;
import com.checkin.partner.data.dao.TaskDao_Impl;
import com.checkin.partner.data.dao.UserDao;
import com.checkin.partner.data.dao.UserDao_Impl;
import java.lang.Class;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.Generated;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class AppDatabase_Impl extends AppDatabase {
  private volatile UserDao _userDao;

  private volatile PairDao _pairDao;

  private volatile TaskDao _taskDao;

  private volatile CheckinDao _checkinDao;

  private volatile RewardDao _rewardDao;

  private volatile PointDao _pointDao;

  private volatile NotificationDao _notificationDao;

  private volatile AchievementDao _achievementDao;

  private volatile ConfigDao _configDao;

  @Override
  @NonNull
  protected SupportSQLiteOpenHelper createOpenHelper(@NonNull final DatabaseConfiguration config) {
    final SupportSQLiteOpenHelper.Callback _openCallback = new RoomOpenHelper(config, new RoomOpenHelper.Delegate(6) {
      @Override
      public void createAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `user` (`userId` TEXT NOT NULL, `username` TEXT NOT NULL, `avatarUrl` TEXT, `personalPoints` INTEGER NOT NULL, `poolPoints` INTEGER NOT NULL, `isVacation` INTEGER NOT NULL, `createdAt` TEXT NOT NULL, PRIMARY KEY(`userId`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `pair` (`pairId` TEXT NOT NULL, `status` TEXT NOT NULL, `partnerId` TEXT, `partnerUsername` TEXT, `partnerAvatarUrl` TEXT, `requestedBy` TEXT, `unbindRequestedBy` TEXT, `createdAt` TEXT, PRIMARY KEY(`pairId`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `task` (`taskId` TEXT NOT NULL, `user_id` TEXT NOT NULL, `creator_id` TEXT NOT NULL, `name` TEXT NOT NULL, `frequency` TEXT NOT NULL, `point_per_check` INTEGER NOT NULL, `start_time` TEXT, `end_time` TEXT, `is_active` INTEGER NOT NULL, `status` TEXT NOT NULL, `pending_edit_by` TEXT, `pending_delete_by` TEXT, `created_at` TEXT NOT NULL, `require_approval` INTEGER NOT NULL, PRIMARY KEY(`taskId`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `checkin_record` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `record_id` TEXT NOT NULL, `task_id` TEXT NOT NULL, `user_id` TEXT NOT NULL, `checkin_time` TEXT NOT NULL, `note` TEXT, `image_url` TEXT, `is_makeup` INTEGER NOT NULL, `created_at` TEXT NOT NULL)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `reward` (`id` TEXT NOT NULL, `creator_id` TEXT NOT NULL, `name` TEXT NOT NULL, `required_points` INTEGER NOT NULL, `expires_at` TEXT, `status` TEXT NOT NULL, `applicant_id` TEXT, `created_at` TEXT NOT NULL, `pending_delete_by` TEXT, `claim_requested_by` TEXT, PRIMARY KEY(`id`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `point_transaction` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `transaction_id` TEXT NOT NULL, `user_id` TEXT NOT NULL, `amount` INTEGER NOT NULL, `type` TEXT NOT NULL, `category` TEXT NOT NULL, `description` TEXT, `created_at` TEXT NOT NULL)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `notification` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `user_id` TEXT NOT NULL, `type` TEXT NOT NULL, `title` TEXT NOT NULL, `body` TEXT, `related_id` TEXT, `is_read` INTEGER NOT NULL, `created_at` TEXT NOT NULL)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `achievement` (`code` TEXT NOT NULL, `name` TEXT NOT NULL, `icon` TEXT NOT NULL, `description` TEXT NOT NULL, `unlocked` INTEGER NOT NULL, `unlocked_at` TEXT, PRIMARY KEY(`code`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `user_config` (`userId` TEXT NOT NULL, `reminder_time` TEXT, `notify_checkin` INTEGER NOT NULL, `notify_reward` INTEGER NOT NULL, `notify_pair` INTEGER NOT NULL, `personal_ratio` REAL NOT NULL, `pool_ratio` REAL NOT NULL, `streak_penalty_on` INTEGER NOT NULL, `theme_mode` TEXT NOT NULL, PRIMARY KEY(`userId`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)");
        db.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, '996033e8bb02fcdd4860b33e71ba73b9')");
      }

      @Override
      public void dropAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS `user`");
        db.execSQL("DROP TABLE IF EXISTS `pair`");
        db.execSQL("DROP TABLE IF EXISTS `task`");
        db.execSQL("DROP TABLE IF EXISTS `checkin_record`");
        db.execSQL("DROP TABLE IF EXISTS `reward`");
        db.execSQL("DROP TABLE IF EXISTS `point_transaction`");
        db.execSQL("DROP TABLE IF EXISTS `notification`");
        db.execSQL("DROP TABLE IF EXISTS `achievement`");
        db.execSQL("DROP TABLE IF EXISTS `user_config`");
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onDestructiveMigration(db);
          }
        }
      }

      @Override
      public void onCreate(@NonNull final SupportSQLiteDatabase db) {
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onCreate(db);
          }
        }
      }

      @Override
      public void onOpen(@NonNull final SupportSQLiteDatabase db) {
        mDatabase = db;
        internalInitInvalidationTracker(db);
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onOpen(db);
          }
        }
      }

      @Override
      public void onPreMigrate(@NonNull final SupportSQLiteDatabase db) {
        DBUtil.dropFtsSyncTriggers(db);
      }

      @Override
      public void onPostMigrate(@NonNull final SupportSQLiteDatabase db) {
      }

      @Override
      @NonNull
      public RoomOpenHelper.ValidationResult onValidateSchema(
          @NonNull final SupportSQLiteDatabase db) {
        final HashMap<String, TableInfo.Column> _columnsUser = new HashMap<String, TableInfo.Column>(7);
        _columnsUser.put("userId", new TableInfo.Column("userId", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsUser.put("username", new TableInfo.Column("username", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsUser.put("avatarUrl", new TableInfo.Column("avatarUrl", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsUser.put("personalPoints", new TableInfo.Column("personalPoints", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsUser.put("poolPoints", new TableInfo.Column("poolPoints", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsUser.put("isVacation", new TableInfo.Column("isVacation", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsUser.put("createdAt", new TableInfo.Column("createdAt", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysUser = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesUser = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoUser = new TableInfo("user", _columnsUser, _foreignKeysUser, _indicesUser);
        final TableInfo _existingUser = TableInfo.read(db, "user");
        if (!_infoUser.equals(_existingUser)) {
          return new RoomOpenHelper.ValidationResult(false, "user(com.checkin.partner.data.entity.UserEntity).\n"
                  + " Expected:\n" + _infoUser + "\n"
                  + " Found:\n" + _existingUser);
        }
        final HashMap<String, TableInfo.Column> _columnsPair = new HashMap<String, TableInfo.Column>(8);
        _columnsPair.put("pairId", new TableInfo.Column("pairId", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPair.put("status", new TableInfo.Column("status", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPair.put("partnerId", new TableInfo.Column("partnerId", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPair.put("partnerUsername", new TableInfo.Column("partnerUsername", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPair.put("partnerAvatarUrl", new TableInfo.Column("partnerAvatarUrl", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPair.put("requestedBy", new TableInfo.Column("requestedBy", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPair.put("unbindRequestedBy", new TableInfo.Column("unbindRequestedBy", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPair.put("createdAt", new TableInfo.Column("createdAt", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysPair = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesPair = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoPair = new TableInfo("pair", _columnsPair, _foreignKeysPair, _indicesPair);
        final TableInfo _existingPair = TableInfo.read(db, "pair");
        if (!_infoPair.equals(_existingPair)) {
          return new RoomOpenHelper.ValidationResult(false, "pair(com.checkin.partner.data.entity.PairEntity).\n"
                  + " Expected:\n" + _infoPair + "\n"
                  + " Found:\n" + _existingPair);
        }
        final HashMap<String, TableInfo.Column> _columnsTask = new HashMap<String, TableInfo.Column>(14);
        _columnsTask.put("taskId", new TableInfo.Column("taskId", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTask.put("user_id", new TableInfo.Column("user_id", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTask.put("creator_id", new TableInfo.Column("creator_id", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTask.put("name", new TableInfo.Column("name", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTask.put("frequency", new TableInfo.Column("frequency", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTask.put("point_per_check", new TableInfo.Column("point_per_check", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTask.put("start_time", new TableInfo.Column("start_time", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTask.put("end_time", new TableInfo.Column("end_time", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTask.put("is_active", new TableInfo.Column("is_active", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTask.put("status", new TableInfo.Column("status", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTask.put("pending_edit_by", new TableInfo.Column("pending_edit_by", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTask.put("pending_delete_by", new TableInfo.Column("pending_delete_by", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTask.put("created_at", new TableInfo.Column("created_at", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTask.put("require_approval", new TableInfo.Column("require_approval", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysTask = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesTask = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoTask = new TableInfo("task", _columnsTask, _foreignKeysTask, _indicesTask);
        final TableInfo _existingTask = TableInfo.read(db, "task");
        if (!_infoTask.equals(_existingTask)) {
          return new RoomOpenHelper.ValidationResult(false, "task(com.checkin.partner.data.entity.TaskEntity).\n"
                  + " Expected:\n" + _infoTask + "\n"
                  + " Found:\n" + _existingTask);
        }
        final HashMap<String, TableInfo.Column> _columnsCheckinRecord = new HashMap<String, TableInfo.Column>(9);
        _columnsCheckinRecord.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCheckinRecord.put("record_id", new TableInfo.Column("record_id", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCheckinRecord.put("task_id", new TableInfo.Column("task_id", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCheckinRecord.put("user_id", new TableInfo.Column("user_id", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCheckinRecord.put("checkin_time", new TableInfo.Column("checkin_time", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCheckinRecord.put("note", new TableInfo.Column("note", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCheckinRecord.put("image_url", new TableInfo.Column("image_url", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCheckinRecord.put("is_makeup", new TableInfo.Column("is_makeup", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCheckinRecord.put("created_at", new TableInfo.Column("created_at", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysCheckinRecord = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesCheckinRecord = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoCheckinRecord = new TableInfo("checkin_record", _columnsCheckinRecord, _foreignKeysCheckinRecord, _indicesCheckinRecord);
        final TableInfo _existingCheckinRecord = TableInfo.read(db, "checkin_record");
        if (!_infoCheckinRecord.equals(_existingCheckinRecord)) {
          return new RoomOpenHelper.ValidationResult(false, "checkin_record(com.checkin.partner.data.entity.CheckinRecordEntity).\n"
                  + " Expected:\n" + _infoCheckinRecord + "\n"
                  + " Found:\n" + _existingCheckinRecord);
        }
        final HashMap<String, TableInfo.Column> _columnsReward = new HashMap<String, TableInfo.Column>(10);
        _columnsReward.put("id", new TableInfo.Column("id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsReward.put("creator_id", new TableInfo.Column("creator_id", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsReward.put("name", new TableInfo.Column("name", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsReward.put("required_points", new TableInfo.Column("required_points", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsReward.put("expires_at", new TableInfo.Column("expires_at", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsReward.put("status", new TableInfo.Column("status", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsReward.put("applicant_id", new TableInfo.Column("applicant_id", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsReward.put("created_at", new TableInfo.Column("created_at", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsReward.put("pending_delete_by", new TableInfo.Column("pending_delete_by", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsReward.put("claim_requested_by", new TableInfo.Column("claim_requested_by", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysReward = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesReward = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoReward = new TableInfo("reward", _columnsReward, _foreignKeysReward, _indicesReward);
        final TableInfo _existingReward = TableInfo.read(db, "reward");
        if (!_infoReward.equals(_existingReward)) {
          return new RoomOpenHelper.ValidationResult(false, "reward(com.checkin.partner.data.entity.RewardEntity).\n"
                  + " Expected:\n" + _infoReward + "\n"
                  + " Found:\n" + _existingReward);
        }
        final HashMap<String, TableInfo.Column> _columnsPointTransaction = new HashMap<String, TableInfo.Column>(8);
        _columnsPointTransaction.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPointTransaction.put("transaction_id", new TableInfo.Column("transaction_id", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPointTransaction.put("user_id", new TableInfo.Column("user_id", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPointTransaction.put("amount", new TableInfo.Column("amount", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPointTransaction.put("type", new TableInfo.Column("type", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPointTransaction.put("category", new TableInfo.Column("category", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPointTransaction.put("description", new TableInfo.Column("description", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPointTransaction.put("created_at", new TableInfo.Column("created_at", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysPointTransaction = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesPointTransaction = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoPointTransaction = new TableInfo("point_transaction", _columnsPointTransaction, _foreignKeysPointTransaction, _indicesPointTransaction);
        final TableInfo _existingPointTransaction = TableInfo.read(db, "point_transaction");
        if (!_infoPointTransaction.equals(_existingPointTransaction)) {
          return new RoomOpenHelper.ValidationResult(false, "point_transaction(com.checkin.partner.data.entity.PointTransactionEntity).\n"
                  + " Expected:\n" + _infoPointTransaction + "\n"
                  + " Found:\n" + _existingPointTransaction);
        }
        final HashMap<String, TableInfo.Column> _columnsNotification = new HashMap<String, TableInfo.Column>(8);
        _columnsNotification.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsNotification.put("user_id", new TableInfo.Column("user_id", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsNotification.put("type", new TableInfo.Column("type", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsNotification.put("title", new TableInfo.Column("title", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsNotification.put("body", new TableInfo.Column("body", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsNotification.put("related_id", new TableInfo.Column("related_id", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsNotification.put("is_read", new TableInfo.Column("is_read", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsNotification.put("created_at", new TableInfo.Column("created_at", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysNotification = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesNotification = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoNotification = new TableInfo("notification", _columnsNotification, _foreignKeysNotification, _indicesNotification);
        final TableInfo _existingNotification = TableInfo.read(db, "notification");
        if (!_infoNotification.equals(_existingNotification)) {
          return new RoomOpenHelper.ValidationResult(false, "notification(com.checkin.partner.data.entity.NotificationEntity).\n"
                  + " Expected:\n" + _infoNotification + "\n"
                  + " Found:\n" + _existingNotification);
        }
        final HashMap<String, TableInfo.Column> _columnsAchievement = new HashMap<String, TableInfo.Column>(6);
        _columnsAchievement.put("code", new TableInfo.Column("code", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAchievement.put("name", new TableInfo.Column("name", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAchievement.put("icon", new TableInfo.Column("icon", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAchievement.put("description", new TableInfo.Column("description", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAchievement.put("unlocked", new TableInfo.Column("unlocked", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAchievement.put("unlocked_at", new TableInfo.Column("unlocked_at", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysAchievement = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesAchievement = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoAchievement = new TableInfo("achievement", _columnsAchievement, _foreignKeysAchievement, _indicesAchievement);
        final TableInfo _existingAchievement = TableInfo.read(db, "achievement");
        if (!_infoAchievement.equals(_existingAchievement)) {
          return new RoomOpenHelper.ValidationResult(false, "achievement(com.checkin.partner.data.entity.AchievementEntity).\n"
                  + " Expected:\n" + _infoAchievement + "\n"
                  + " Found:\n" + _existingAchievement);
        }
        final HashMap<String, TableInfo.Column> _columnsUserConfig = new HashMap<String, TableInfo.Column>(9);
        _columnsUserConfig.put("userId", new TableInfo.Column("userId", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsUserConfig.put("reminder_time", new TableInfo.Column("reminder_time", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsUserConfig.put("notify_checkin", new TableInfo.Column("notify_checkin", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsUserConfig.put("notify_reward", new TableInfo.Column("notify_reward", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsUserConfig.put("notify_pair", new TableInfo.Column("notify_pair", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsUserConfig.put("personal_ratio", new TableInfo.Column("personal_ratio", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsUserConfig.put("pool_ratio", new TableInfo.Column("pool_ratio", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsUserConfig.put("streak_penalty_on", new TableInfo.Column("streak_penalty_on", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsUserConfig.put("theme_mode", new TableInfo.Column("theme_mode", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysUserConfig = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesUserConfig = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoUserConfig = new TableInfo("user_config", _columnsUserConfig, _foreignKeysUserConfig, _indicesUserConfig);
        final TableInfo _existingUserConfig = TableInfo.read(db, "user_config");
        if (!_infoUserConfig.equals(_existingUserConfig)) {
          return new RoomOpenHelper.ValidationResult(false, "user_config(com.checkin.partner.data.entity.UserConfigEntity).\n"
                  + " Expected:\n" + _infoUserConfig + "\n"
                  + " Found:\n" + _existingUserConfig);
        }
        return new RoomOpenHelper.ValidationResult(true, null);
      }
    }, "996033e8bb02fcdd4860b33e71ba73b9", "d5fdd1963c21caa994f2d437cce2dcaf");
    final SupportSQLiteOpenHelper.Configuration _sqliteConfig = SupportSQLiteOpenHelper.Configuration.builder(config.context).name(config.name).callback(_openCallback).build();
    final SupportSQLiteOpenHelper _helper = config.sqliteOpenHelperFactory.create(_sqliteConfig);
    return _helper;
  }

  @Override
  @NonNull
  protected InvalidationTracker createInvalidationTracker() {
    final HashMap<String, String> _shadowTablesMap = new HashMap<String, String>(0);
    final HashMap<String, Set<String>> _viewTables = new HashMap<String, Set<String>>(0);
    return new InvalidationTracker(this, _shadowTablesMap, _viewTables, "user","pair","task","checkin_record","reward","point_transaction","notification","achievement","user_config");
  }

  @Override
  public void clearAllTables() {
    super.assertNotMainThread();
    final SupportSQLiteDatabase _db = super.getOpenHelper().getWritableDatabase();
    try {
      super.beginTransaction();
      _db.execSQL("DELETE FROM `user`");
      _db.execSQL("DELETE FROM `pair`");
      _db.execSQL("DELETE FROM `task`");
      _db.execSQL("DELETE FROM `checkin_record`");
      _db.execSQL("DELETE FROM `reward`");
      _db.execSQL("DELETE FROM `point_transaction`");
      _db.execSQL("DELETE FROM `notification`");
      _db.execSQL("DELETE FROM `achievement`");
      _db.execSQL("DELETE FROM `user_config`");
      super.setTransactionSuccessful();
    } finally {
      super.endTransaction();
      _db.query("PRAGMA wal_checkpoint(FULL)").close();
      if (!_db.inTransaction()) {
        _db.execSQL("VACUUM");
      }
    }
  }

  @Override
  @NonNull
  protected Map<Class<?>, List<Class<?>>> getRequiredTypeConverters() {
    final HashMap<Class<?>, List<Class<?>>> _typeConvertersMap = new HashMap<Class<?>, List<Class<?>>>();
    _typeConvertersMap.put(UserDao.class, UserDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(PairDao.class, PairDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(TaskDao.class, TaskDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(CheckinDao.class, CheckinDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(RewardDao.class, RewardDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(PointDao.class, PointDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(NotificationDao.class, NotificationDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(AchievementDao.class, AchievementDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(ConfigDao.class, ConfigDao_Impl.getRequiredConverters());
    return _typeConvertersMap;
  }

  @Override
  @NonNull
  public Set<Class<? extends AutoMigrationSpec>> getRequiredAutoMigrationSpecs() {
    final HashSet<Class<? extends AutoMigrationSpec>> _autoMigrationSpecsSet = new HashSet<Class<? extends AutoMigrationSpec>>();
    return _autoMigrationSpecsSet;
  }

  @Override
  @NonNull
  public List<Migration> getAutoMigrations(
      @NonNull final Map<Class<? extends AutoMigrationSpec>, AutoMigrationSpec> autoMigrationSpecs) {
    final List<Migration> _autoMigrations = new ArrayList<Migration>();
    return _autoMigrations;
  }

  @Override
  public UserDao userDao() {
    if (_userDao != null) {
      return _userDao;
    } else {
      synchronized(this) {
        if(_userDao == null) {
          _userDao = new UserDao_Impl(this);
        }
        return _userDao;
      }
    }
  }

  @Override
  public PairDao pairDao() {
    if (_pairDao != null) {
      return _pairDao;
    } else {
      synchronized(this) {
        if(_pairDao == null) {
          _pairDao = new PairDao_Impl(this);
        }
        return _pairDao;
      }
    }
  }

  @Override
  public TaskDao taskDao() {
    if (_taskDao != null) {
      return _taskDao;
    } else {
      synchronized(this) {
        if(_taskDao == null) {
          _taskDao = new TaskDao_Impl(this);
        }
        return _taskDao;
      }
    }
  }

  @Override
  public CheckinDao checkinDao() {
    if (_checkinDao != null) {
      return _checkinDao;
    } else {
      synchronized(this) {
        if(_checkinDao == null) {
          _checkinDao = new CheckinDao_Impl(this);
        }
        return _checkinDao;
      }
    }
  }

  @Override
  public RewardDao rewardDao() {
    if (_rewardDao != null) {
      return _rewardDao;
    } else {
      synchronized(this) {
        if(_rewardDao == null) {
          _rewardDao = new RewardDao_Impl(this);
        }
        return _rewardDao;
      }
    }
  }

  @Override
  public PointDao pointDao() {
    if (_pointDao != null) {
      return _pointDao;
    } else {
      synchronized(this) {
        if(_pointDao == null) {
          _pointDao = new PointDao_Impl(this);
        }
        return _pointDao;
      }
    }
  }

  @Override
  public NotificationDao notificationDao() {
    if (_notificationDao != null) {
      return _notificationDao;
    } else {
      synchronized(this) {
        if(_notificationDao == null) {
          _notificationDao = new NotificationDao_Impl(this);
        }
        return _notificationDao;
      }
    }
  }

  @Override
  public AchievementDao achievementDao() {
    if (_achievementDao != null) {
      return _achievementDao;
    } else {
      synchronized(this) {
        if(_achievementDao == null) {
          _achievementDao = new AchievementDao_Impl(this);
        }
        return _achievementDao;
      }
    }
  }

  @Override
  public ConfigDao configDao() {
    if (_configDao != null) {
      return _configDao;
    } else {
      synchronized(this) {
        if(_configDao == null) {
          _configDao = new ConfigDao_Impl(this);
        }
        return _configDao;
      }
    }
  }
}
