package com.checkin.partner.data.dao;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import com.checkin.partner.data.entity.TaskEntity;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;
import kotlin.Unit;
import kotlin.coroutines.Continuation;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class TaskDao_Impl implements TaskDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<TaskEntity> __insertionAdapterOfTaskEntity;

  private final SharedSQLiteStatement __preparedStmtOfDeleteById;

  private final SharedSQLiteStatement __preparedStmtOfClearAll;

  public TaskDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfTaskEntity = new EntityInsertionAdapter<TaskEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `task` (`taskId`,`user_id`,`creator_id`,`name`,`frequency`,`point_per_check`,`start_time`,`end_time`,`is_active`,`status`,`pending_edit_by`,`pending_delete_by`,`created_at`,`require_approval`) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final TaskEntity entity) {
        statement.bindString(1, entity.getTaskId());
        statement.bindString(2, entity.getUserId());
        statement.bindString(3, entity.getCreatorId());
        statement.bindString(4, entity.getName());
        statement.bindString(5, entity.getFrequency());
        statement.bindLong(6, entity.getPointPerCheck());
        if (entity.getStartTime() == null) {
          statement.bindNull(7);
        } else {
          statement.bindString(7, entity.getStartTime());
        }
        if (entity.getEndTime() == null) {
          statement.bindNull(8);
        } else {
          statement.bindString(8, entity.getEndTime());
        }
        final int _tmp = entity.isActive() ? 1 : 0;
        statement.bindLong(9, _tmp);
        statement.bindString(10, entity.getStatus());
        if (entity.getPendingEditBy() == null) {
          statement.bindNull(11);
        } else {
          statement.bindString(11, entity.getPendingEditBy());
        }
        if (entity.getPendingDeleteBy() == null) {
          statement.bindNull(12);
        } else {
          statement.bindString(12, entity.getPendingDeleteBy());
        }
        statement.bindString(13, entity.getCreatedAt());
        final int _tmp_1 = entity.getRequireApproval() ? 1 : 0;
        statement.bindLong(14, _tmp_1);
      }
    };
    this.__preparedStmtOfDeleteById = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM task WHERE taskId = ?";
        return _query;
      }
    };
    this.__preparedStmtOfClearAll = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM task";
        return _query;
      }
    };
  }

  @Override
  public Object insertAll(final List<TaskEntity> tasks,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfTaskEntity.insert(tasks);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteById(final String taskId, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteById.acquire();
        int _argIndex = 1;
        _stmt.bindString(_argIndex, taskId);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfDeleteById.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object clearAll(final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfClearAll.acquire();
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfClearAll.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object getByUserId(final String userId,
      final Continuation<? super List<TaskEntity>> $completion) {
    final String _sql = "SELECT * FROM task WHERE user_id = ? AND status != 'DONE' ORDER BY created_at DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, userId);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<TaskEntity>>() {
      @Override
      @NonNull
      public List<TaskEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfTaskId = CursorUtil.getColumnIndexOrThrow(_cursor, "taskId");
          final int _cursorIndexOfUserId = CursorUtil.getColumnIndexOrThrow(_cursor, "user_id");
          final int _cursorIndexOfCreatorId = CursorUtil.getColumnIndexOrThrow(_cursor, "creator_id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfFrequency = CursorUtil.getColumnIndexOrThrow(_cursor, "frequency");
          final int _cursorIndexOfPointPerCheck = CursorUtil.getColumnIndexOrThrow(_cursor, "point_per_check");
          final int _cursorIndexOfStartTime = CursorUtil.getColumnIndexOrThrow(_cursor, "start_time");
          final int _cursorIndexOfEndTime = CursorUtil.getColumnIndexOrThrow(_cursor, "end_time");
          final int _cursorIndexOfIsActive = CursorUtil.getColumnIndexOrThrow(_cursor, "is_active");
          final int _cursorIndexOfStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "status");
          final int _cursorIndexOfPendingEditBy = CursorUtil.getColumnIndexOrThrow(_cursor, "pending_edit_by");
          final int _cursorIndexOfPendingDeleteBy = CursorUtil.getColumnIndexOrThrow(_cursor, "pending_delete_by");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "created_at");
          final int _cursorIndexOfRequireApproval = CursorUtil.getColumnIndexOrThrow(_cursor, "require_approval");
          final List<TaskEntity> _result = new ArrayList<TaskEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final TaskEntity _item;
            final String _tmpTaskId;
            _tmpTaskId = _cursor.getString(_cursorIndexOfTaskId);
            final String _tmpUserId;
            _tmpUserId = _cursor.getString(_cursorIndexOfUserId);
            final String _tmpCreatorId;
            _tmpCreatorId = _cursor.getString(_cursorIndexOfCreatorId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpFrequency;
            _tmpFrequency = _cursor.getString(_cursorIndexOfFrequency);
            final int _tmpPointPerCheck;
            _tmpPointPerCheck = _cursor.getInt(_cursorIndexOfPointPerCheck);
            final String _tmpStartTime;
            if (_cursor.isNull(_cursorIndexOfStartTime)) {
              _tmpStartTime = null;
            } else {
              _tmpStartTime = _cursor.getString(_cursorIndexOfStartTime);
            }
            final String _tmpEndTime;
            if (_cursor.isNull(_cursorIndexOfEndTime)) {
              _tmpEndTime = null;
            } else {
              _tmpEndTime = _cursor.getString(_cursorIndexOfEndTime);
            }
            final boolean _tmpIsActive;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsActive);
            _tmpIsActive = _tmp != 0;
            final String _tmpStatus;
            _tmpStatus = _cursor.getString(_cursorIndexOfStatus);
            final String _tmpPendingEditBy;
            if (_cursor.isNull(_cursorIndexOfPendingEditBy)) {
              _tmpPendingEditBy = null;
            } else {
              _tmpPendingEditBy = _cursor.getString(_cursorIndexOfPendingEditBy);
            }
            final String _tmpPendingDeleteBy;
            if (_cursor.isNull(_cursorIndexOfPendingDeleteBy)) {
              _tmpPendingDeleteBy = null;
            } else {
              _tmpPendingDeleteBy = _cursor.getString(_cursorIndexOfPendingDeleteBy);
            }
            final String _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getString(_cursorIndexOfCreatedAt);
            final boolean _tmpRequireApproval;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfRequireApproval);
            _tmpRequireApproval = _tmp_1 != 0;
            _item = new TaskEntity(_tmpTaskId,_tmpUserId,_tmpCreatorId,_tmpName,_tmpFrequency,_tmpPointPerCheck,_tmpStartTime,_tmpEndTime,_tmpIsActive,_tmpStatus,_tmpPendingEditBy,_tmpPendingDeleteBy,_tmpCreatedAt,_tmpRequireApproval);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Object getByCreatorId(final String creatorId,
      final Continuation<? super List<TaskEntity>> $completion) {
    final String _sql = "SELECT * FROM task WHERE creator_id = ? ORDER BY created_at DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, creatorId);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<TaskEntity>>() {
      @Override
      @NonNull
      public List<TaskEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfTaskId = CursorUtil.getColumnIndexOrThrow(_cursor, "taskId");
          final int _cursorIndexOfUserId = CursorUtil.getColumnIndexOrThrow(_cursor, "user_id");
          final int _cursorIndexOfCreatorId = CursorUtil.getColumnIndexOrThrow(_cursor, "creator_id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfFrequency = CursorUtil.getColumnIndexOrThrow(_cursor, "frequency");
          final int _cursorIndexOfPointPerCheck = CursorUtil.getColumnIndexOrThrow(_cursor, "point_per_check");
          final int _cursorIndexOfStartTime = CursorUtil.getColumnIndexOrThrow(_cursor, "start_time");
          final int _cursorIndexOfEndTime = CursorUtil.getColumnIndexOrThrow(_cursor, "end_time");
          final int _cursorIndexOfIsActive = CursorUtil.getColumnIndexOrThrow(_cursor, "is_active");
          final int _cursorIndexOfStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "status");
          final int _cursorIndexOfPendingEditBy = CursorUtil.getColumnIndexOrThrow(_cursor, "pending_edit_by");
          final int _cursorIndexOfPendingDeleteBy = CursorUtil.getColumnIndexOrThrow(_cursor, "pending_delete_by");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "created_at");
          final int _cursorIndexOfRequireApproval = CursorUtil.getColumnIndexOrThrow(_cursor, "require_approval");
          final List<TaskEntity> _result = new ArrayList<TaskEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final TaskEntity _item;
            final String _tmpTaskId;
            _tmpTaskId = _cursor.getString(_cursorIndexOfTaskId);
            final String _tmpUserId;
            _tmpUserId = _cursor.getString(_cursorIndexOfUserId);
            final String _tmpCreatorId;
            _tmpCreatorId = _cursor.getString(_cursorIndexOfCreatorId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpFrequency;
            _tmpFrequency = _cursor.getString(_cursorIndexOfFrequency);
            final int _tmpPointPerCheck;
            _tmpPointPerCheck = _cursor.getInt(_cursorIndexOfPointPerCheck);
            final String _tmpStartTime;
            if (_cursor.isNull(_cursorIndexOfStartTime)) {
              _tmpStartTime = null;
            } else {
              _tmpStartTime = _cursor.getString(_cursorIndexOfStartTime);
            }
            final String _tmpEndTime;
            if (_cursor.isNull(_cursorIndexOfEndTime)) {
              _tmpEndTime = null;
            } else {
              _tmpEndTime = _cursor.getString(_cursorIndexOfEndTime);
            }
            final boolean _tmpIsActive;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsActive);
            _tmpIsActive = _tmp != 0;
            final String _tmpStatus;
            _tmpStatus = _cursor.getString(_cursorIndexOfStatus);
            final String _tmpPendingEditBy;
            if (_cursor.isNull(_cursorIndexOfPendingEditBy)) {
              _tmpPendingEditBy = null;
            } else {
              _tmpPendingEditBy = _cursor.getString(_cursorIndexOfPendingEditBy);
            }
            final String _tmpPendingDeleteBy;
            if (_cursor.isNull(_cursorIndexOfPendingDeleteBy)) {
              _tmpPendingDeleteBy = null;
            } else {
              _tmpPendingDeleteBy = _cursor.getString(_cursorIndexOfPendingDeleteBy);
            }
            final String _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getString(_cursorIndexOfCreatedAt);
            final boolean _tmpRequireApproval;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfRequireApproval);
            _tmpRequireApproval = _tmp_1 != 0;
            _item = new TaskEntity(_tmpTaskId,_tmpUserId,_tmpCreatorId,_tmpName,_tmpFrequency,_tmpPointPerCheck,_tmpStartTime,_tmpEndTime,_tmpIsActive,_tmpStatus,_tmpPendingEditBy,_tmpPendingDeleteBy,_tmpCreatedAt,_tmpRequireApproval);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Object getById(final String taskId, final Continuation<? super TaskEntity> $completion) {
    final String _sql = "SELECT * FROM task WHERE taskId = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, taskId);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<TaskEntity>() {
      @Override
      @Nullable
      public TaskEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfTaskId = CursorUtil.getColumnIndexOrThrow(_cursor, "taskId");
          final int _cursorIndexOfUserId = CursorUtil.getColumnIndexOrThrow(_cursor, "user_id");
          final int _cursorIndexOfCreatorId = CursorUtil.getColumnIndexOrThrow(_cursor, "creator_id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfFrequency = CursorUtil.getColumnIndexOrThrow(_cursor, "frequency");
          final int _cursorIndexOfPointPerCheck = CursorUtil.getColumnIndexOrThrow(_cursor, "point_per_check");
          final int _cursorIndexOfStartTime = CursorUtil.getColumnIndexOrThrow(_cursor, "start_time");
          final int _cursorIndexOfEndTime = CursorUtil.getColumnIndexOrThrow(_cursor, "end_time");
          final int _cursorIndexOfIsActive = CursorUtil.getColumnIndexOrThrow(_cursor, "is_active");
          final int _cursorIndexOfStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "status");
          final int _cursorIndexOfPendingEditBy = CursorUtil.getColumnIndexOrThrow(_cursor, "pending_edit_by");
          final int _cursorIndexOfPendingDeleteBy = CursorUtil.getColumnIndexOrThrow(_cursor, "pending_delete_by");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "created_at");
          final int _cursorIndexOfRequireApproval = CursorUtil.getColumnIndexOrThrow(_cursor, "require_approval");
          final TaskEntity _result;
          if (_cursor.moveToFirst()) {
            final String _tmpTaskId;
            _tmpTaskId = _cursor.getString(_cursorIndexOfTaskId);
            final String _tmpUserId;
            _tmpUserId = _cursor.getString(_cursorIndexOfUserId);
            final String _tmpCreatorId;
            _tmpCreatorId = _cursor.getString(_cursorIndexOfCreatorId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpFrequency;
            _tmpFrequency = _cursor.getString(_cursorIndexOfFrequency);
            final int _tmpPointPerCheck;
            _tmpPointPerCheck = _cursor.getInt(_cursorIndexOfPointPerCheck);
            final String _tmpStartTime;
            if (_cursor.isNull(_cursorIndexOfStartTime)) {
              _tmpStartTime = null;
            } else {
              _tmpStartTime = _cursor.getString(_cursorIndexOfStartTime);
            }
            final String _tmpEndTime;
            if (_cursor.isNull(_cursorIndexOfEndTime)) {
              _tmpEndTime = null;
            } else {
              _tmpEndTime = _cursor.getString(_cursorIndexOfEndTime);
            }
            final boolean _tmpIsActive;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsActive);
            _tmpIsActive = _tmp != 0;
            final String _tmpStatus;
            _tmpStatus = _cursor.getString(_cursorIndexOfStatus);
            final String _tmpPendingEditBy;
            if (_cursor.isNull(_cursorIndexOfPendingEditBy)) {
              _tmpPendingEditBy = null;
            } else {
              _tmpPendingEditBy = _cursor.getString(_cursorIndexOfPendingEditBy);
            }
            final String _tmpPendingDeleteBy;
            if (_cursor.isNull(_cursorIndexOfPendingDeleteBy)) {
              _tmpPendingDeleteBy = null;
            } else {
              _tmpPendingDeleteBy = _cursor.getString(_cursorIndexOfPendingDeleteBy);
            }
            final String _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getString(_cursorIndexOfCreatedAt);
            final boolean _tmpRequireApproval;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfRequireApproval);
            _tmpRequireApproval = _tmp_1 != 0;
            _result = new TaskEntity(_tmpTaskId,_tmpUserId,_tmpCreatorId,_tmpName,_tmpFrequency,_tmpPointPerCheck,_tmpStartTime,_tmpEndTime,_tmpIsActive,_tmpStatus,_tmpPendingEditBy,_tmpPendingDeleteBy,_tmpCreatedAt,_tmpRequireApproval);
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
