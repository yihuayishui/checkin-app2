package com.checkin.partner.data.dao;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import com.checkin.partner.data.entity.RewardEntity;
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
public final class RewardDao_Impl implements RewardDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<RewardEntity> __insertionAdapterOfRewardEntity;

  private final SharedSQLiteStatement __preparedStmtOfClearAll;

  public RewardDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfRewardEntity = new EntityInsertionAdapter<RewardEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `reward` (`id`,`creator_id`,`name`,`required_points`,`expires_at`,`status`,`applicant_id`,`created_at`,`pending_delete_by`,`claim_requested_by`) VALUES (?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final RewardEntity entity) {
        statement.bindString(1, entity.getId());
        statement.bindString(2, entity.getCreatorId());
        statement.bindString(3, entity.getName());
        statement.bindLong(4, entity.getRequiredPoints());
        if (entity.getExpiresAt() == null) {
          statement.bindNull(5);
        } else {
          statement.bindString(5, entity.getExpiresAt());
        }
        statement.bindString(6, entity.getStatus());
        if (entity.getApplicantId() == null) {
          statement.bindNull(7);
        } else {
          statement.bindString(7, entity.getApplicantId());
        }
        statement.bindString(8, entity.getCreatedAt());
        if (entity.getPendingDeleteBy() == null) {
          statement.bindNull(9);
        } else {
          statement.bindString(9, entity.getPendingDeleteBy());
        }
        if (entity.getClaimRequestedBy() == null) {
          statement.bindNull(10);
        } else {
          statement.bindString(10, entity.getClaimRequestedBy());
        }
      }
    };
    this.__preparedStmtOfClearAll = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM reward";
        return _query;
      }
    };
  }

  @Override
  public Object insertAll(final List<RewardEntity> rewards,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfRewardEntity.insert(rewards);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
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
  public Object getAll(final Continuation<? super List<RewardEntity>> $completion) {
    final String _sql = "SELECT * FROM reward ORDER BY created_at DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<RewardEntity>>() {
      @Override
      @NonNull
      public List<RewardEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfCreatorId = CursorUtil.getColumnIndexOrThrow(_cursor, "creator_id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfRequiredPoints = CursorUtil.getColumnIndexOrThrow(_cursor, "required_points");
          final int _cursorIndexOfExpiresAt = CursorUtil.getColumnIndexOrThrow(_cursor, "expires_at");
          final int _cursorIndexOfStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "status");
          final int _cursorIndexOfApplicantId = CursorUtil.getColumnIndexOrThrow(_cursor, "applicant_id");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "created_at");
          final int _cursorIndexOfPendingDeleteBy = CursorUtil.getColumnIndexOrThrow(_cursor, "pending_delete_by");
          final int _cursorIndexOfClaimRequestedBy = CursorUtil.getColumnIndexOrThrow(_cursor, "claim_requested_by");
          final List<RewardEntity> _result = new ArrayList<RewardEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final RewardEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpCreatorId;
            _tmpCreatorId = _cursor.getString(_cursorIndexOfCreatorId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final int _tmpRequiredPoints;
            _tmpRequiredPoints = _cursor.getInt(_cursorIndexOfRequiredPoints);
            final String _tmpExpiresAt;
            if (_cursor.isNull(_cursorIndexOfExpiresAt)) {
              _tmpExpiresAt = null;
            } else {
              _tmpExpiresAt = _cursor.getString(_cursorIndexOfExpiresAt);
            }
            final String _tmpStatus;
            _tmpStatus = _cursor.getString(_cursorIndexOfStatus);
            final String _tmpApplicantId;
            if (_cursor.isNull(_cursorIndexOfApplicantId)) {
              _tmpApplicantId = null;
            } else {
              _tmpApplicantId = _cursor.getString(_cursorIndexOfApplicantId);
            }
            final String _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getString(_cursorIndexOfCreatedAt);
            final String _tmpPendingDeleteBy;
            if (_cursor.isNull(_cursorIndexOfPendingDeleteBy)) {
              _tmpPendingDeleteBy = null;
            } else {
              _tmpPendingDeleteBy = _cursor.getString(_cursorIndexOfPendingDeleteBy);
            }
            final String _tmpClaimRequestedBy;
            if (_cursor.isNull(_cursorIndexOfClaimRequestedBy)) {
              _tmpClaimRequestedBy = null;
            } else {
              _tmpClaimRequestedBy = _cursor.getString(_cursorIndexOfClaimRequestedBy);
            }
            _item = new RewardEntity(_tmpId,_tmpCreatorId,_tmpName,_tmpRequiredPoints,_tmpExpiresAt,_tmpStatus,_tmpApplicantId,_tmpCreatedAt,_tmpPendingDeleteBy,_tmpClaimRequestedBy);
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

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
