package com.checkin.partner.data.dao

import androidx.room.*
import com.checkin.partner.data.entity.*

@Dao
interface UserDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(user: UserEntity)
    @Query("SELECT * FROM user WHERE userId = :userId")
    suspend fun getById(userId: String): UserEntity?
    @Delete
    suspend fun delete(user: UserEntity)
    @Query("DELETE FROM user")
    suspend fun clearAll()
}

@Dao
interface PairDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(pair: PairEntity)
    @Query("SELECT * FROM pair LIMIT 1")
    suspend fun get(): PairEntity?
    @Query("DELETE FROM pair")
    suspend fun clear()
}

@Dao
interface TaskDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(tasks: List<TaskEntity>)
    @Query("SELECT * FROM task WHERE user_id = :userId AND status != 'DONE' ORDER BY created_at DESC")
    suspend fun getByUserId(userId: String): List<TaskEntity>
    @Query("SELECT * FROM task WHERE creator_id = :creatorId ORDER BY created_at DESC")
    suspend fun getByCreatorId(creatorId: String): List<TaskEntity>
    @Query("SELECT * FROM task WHERE taskId = :taskId")
    suspend fun getById(taskId: String): TaskEntity?
    @Query("DELETE FROM task WHERE taskId = :taskId")
    suspend fun deleteById(taskId: String)
    @Query("DELETE FROM task")
    suspend fun clearAll()
}

@Dao
interface CheckinDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(records: List<CheckinRecordEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: CheckinRecordEntity)
    @Query("SELECT * FROM checkin_record WHERE user_id = :userId ORDER BY checkin_time DESC LIMIT 50")
    suspend fun getByUserId(userId: String): List<CheckinRecordEntity>
    @Query("SELECT * FROM checkin_record WHERE user_id = :userId AND checkin_time LIKE :date || '%'")
    suspend fun getByUserAndDate(userId: String, date: String): List<CheckinRecordEntity>
    @Query("SELECT * FROM checkin_record WHERE task_id = :taskId AND user_id = :userId AND checkin_time LIKE :date || '%' AND is_makeup = 0 LIMIT 1")
    suspend fun getTodayByTaskAndUser(taskId: String, userId: String, date: String): CheckinRecordEntity?
    @Query("DELETE FROM checkin_record WHERE record_id = :recordId")
    suspend fun deleteByRecordId(recordId: String)
    @Query("DELETE FROM checkin_record")
    suspend fun clearAll()
}

@Dao
interface RewardDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(rewards: List<RewardEntity>)
    @Query("SELECT * FROM reward ORDER BY created_at DESC")
    suspend fun getAll(): List<RewardEntity>
    @Query("DELETE FROM reward")
    suspend fun clearAll()
}

@Dao
interface PointDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(transactions: List<PointTransactionEntity>)
    @Query("SELECT * FROM point_transaction WHERE user_id = :userId ORDER BY created_at DESC LIMIT :limit")
    suspend fun getByUserId(userId: String, limit: Int = 100): List<PointTransactionEntity>
    @Query("DELETE FROM point_transaction")
    suspend fun clearAll()
}

@Dao
interface NotificationDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(list: List<NotificationEntity>)
    @Query("SELECT * FROM notification WHERE user_id = :userId ORDER BY created_at DESC LIMIT 50")
    suspend fun getByUserId(userId: String): List<NotificationEntity>
    @Query("SELECT COUNT(*) FROM notification WHERE user_id = :userId AND is_read = 0")
    suspend fun unreadCount(userId: String): Int
    @Query("UPDATE notification SET is_read = 1 WHERE user_id = :userId")
    suspend fun markAllRead(userId: String)
    @Query("DELETE FROM notification")
    suspend fun clearAll()
}

@Dao
interface AchievementDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(list: List<AchievementEntity>)
    @Query("SELECT * FROM achievement ORDER BY code")
    suspend fun getAll(): List<AchievementEntity>
    @Query("DELETE FROM achievement")
    suspend fun clearAll()
}

@Dao
interface ConfigDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(config: UserConfigEntity)
    @Query("SELECT * FROM user_config WHERE userId = :userId")
    suspend fun getByUserId(userId: String): UserConfigEntity?
    @Query("DELETE FROM user_config")
    suspend fun clear()
}

@Dao
interface PendingCheckinDao {
    @Insert
    suspend fun insert(pending: PendingCheckinEntity)
    @Query("SELECT * FROM pending_checkin ORDER BY created_at ASC")
    suspend fun getAll(): List<PendingCheckinEntity>
    @Query("DELETE FROM pending_checkin WHERE id = :id")
    suspend fun deleteById(id: Long)
    @Query("UPDATE pending_checkin SET attempts = attempts + 1 WHERE id = :id")
    suspend fun incrementAttempts(id: Long)
    @Query("DELETE FROM pending_checkin WHERE user_id = :userId")
    suspend fun clearByUserId(userId: String)
    @Query("DELETE FROM pending_checkin")
    suspend fun clearAll()
}
