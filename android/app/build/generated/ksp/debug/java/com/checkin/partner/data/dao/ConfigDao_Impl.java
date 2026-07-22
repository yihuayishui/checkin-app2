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
import com.checkin.partner.data.entity.UserConfigEntity;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;
import kotlin.Unit;
import kotlin.coroutines.Continuation;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class ConfigDao_Impl implements ConfigDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<UserConfigEntity> __insertionAdapterOfUserConfigEntity;

  private final SharedSQLiteStatement __preparedStmtOfClear;

  public ConfigDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfUserConfigEntity = new EntityInsertionAdapter<UserConfigEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `user_config` (`userId`,`reminder_time`,`notify_checkin`,`notify_reward`,`notify_pair`,`personal_ratio`,`pool_ratio`,`streak_penalty_on`,`theme_mode`) VALUES (?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final UserConfigEntity entity) {
        statement.bindString(1, entity.getUserId());
        if (entity.getReminderTime() == null) {
          statement.bindNull(2);
        } else {
          statement.bindString(2, entity.getReminderTime());
        }
        final int _tmp = entity.getNotifyCheckin() ? 1 : 0;
        statement.bindLong(3, _tmp);
        final int _tmp_1 = entity.getNotifyReward() ? 1 : 0;
        statement.bindLong(4, _tmp_1);
        final int _tmp_2 = entity.getNotifyPair() ? 1 : 0;
        statement.bindLong(5, _tmp_2);
        statement.bindDouble(6, entity.getPersonalRatio());
        statement.bindDouble(7, entity.getPoolRatio());
        final int _tmp_3 = entity.getStreakPenaltyOn() ? 1 : 0;
        statement.bindLong(8, _tmp_3);
        statement.bindString(9, entity.getThemeMode());
      }
    };
    this.__preparedStmtOfClear = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM user_config";
        return _query;
      }
    };
  }

  @Override
  public Object insert(final UserConfigEntity config,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfUserConfigEntity.insert(config);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object clear(final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfClear.acquire();
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
          __preparedStmtOfClear.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object getByUserId(final String userId,
      final Continuation<? super UserConfigEntity> $completion) {
    final String _sql = "SELECT * FROM user_config WHERE userId = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, userId);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<UserConfigEntity>() {
      @Override
      @Nullable
      public UserConfigEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfUserId = CursorUtil.getColumnIndexOrThrow(_cursor, "userId");
          final int _cursorIndexOfReminderTime = CursorUtil.getColumnIndexOrThrow(_cursor, "reminder_time");
          final int _cursorIndexOfNotifyCheckin = CursorUtil.getColumnIndexOrThrow(_cursor, "notify_checkin");
          final int _cursorIndexOfNotifyReward = CursorUtil.getColumnIndexOrThrow(_cursor, "notify_reward");
          final int _cursorIndexOfNotifyPair = CursorUtil.getColumnIndexOrThrow(_cursor, "notify_pair");
          final int _cursorIndexOfPersonalRatio = CursorUtil.getColumnIndexOrThrow(_cursor, "personal_ratio");
          final int _cursorIndexOfPoolRatio = CursorUtil.getColumnIndexOrThrow(_cursor, "pool_ratio");
          final int _cursorIndexOfStreakPenaltyOn = CursorUtil.getColumnIndexOrThrow(_cursor, "streak_penalty_on");
          final int _cursorIndexOfThemeMode = CursorUtil.getColumnIndexOrThrow(_cursor, "theme_mode");
          final UserConfigEntity _result;
          if (_cursor.moveToFirst()) {
            final String _tmpUserId;
            _tmpUserId = _cursor.getString(_cursorIndexOfUserId);
            final String _tmpReminderTime;
            if (_cursor.isNull(_cursorIndexOfReminderTime)) {
              _tmpReminderTime = null;
            } else {
              _tmpReminderTime = _cursor.getString(_cursorIndexOfReminderTime);
            }
            final boolean _tmpNotifyCheckin;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfNotifyCheckin);
            _tmpNotifyCheckin = _tmp != 0;
            final boolean _tmpNotifyReward;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfNotifyReward);
            _tmpNotifyReward = _tmp_1 != 0;
            final boolean _tmpNotifyPair;
            final int _tmp_2;
            _tmp_2 = _cursor.getInt(_cursorIndexOfNotifyPair);
            _tmpNotifyPair = _tmp_2 != 0;
            final float _tmpPersonalRatio;
            _tmpPersonalRatio = _cursor.getFloat(_cursorIndexOfPersonalRatio);
            final float _tmpPoolRatio;
            _tmpPoolRatio = _cursor.getFloat(_cursorIndexOfPoolRatio);
            final boolean _tmpStreakPenaltyOn;
            final int _tmp_3;
            _tmp_3 = _cursor.getInt(_cursorIndexOfStreakPenaltyOn);
            _tmpStreakPenaltyOn = _tmp_3 != 0;
            final String _tmpThemeMode;
            _tmpThemeMode = _cursor.getString(_cursorIndexOfThemeMode);
            _result = new UserConfigEntity(_tmpUserId,_tmpReminderTime,_tmpNotifyCheckin,_tmpNotifyReward,_tmpNotifyPair,_tmpPersonalRatio,_tmpPoolRatio,_tmpStreakPenaltyOn,_tmpThemeMode);
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
