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
import com.checkin.partner.data.entity.PairEntity;
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
public final class PairDao_Impl implements PairDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<PairEntity> __insertionAdapterOfPairEntity;

  private final SharedSQLiteStatement __preparedStmtOfClear;

  public PairDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfPairEntity = new EntityInsertionAdapter<PairEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `pair` (`pairId`,`status`,`partnerId`,`partnerUsername`,`partnerAvatarUrl`,`requestedBy`,`unbindRequestedBy`,`createdAt`) VALUES (?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final PairEntity entity) {
        statement.bindString(1, entity.getPairId());
        statement.bindString(2, entity.getStatus());
        if (entity.getPartnerId() == null) {
          statement.bindNull(3);
        } else {
          statement.bindString(3, entity.getPartnerId());
        }
        if (entity.getPartnerUsername() == null) {
          statement.bindNull(4);
        } else {
          statement.bindString(4, entity.getPartnerUsername());
        }
        if (entity.getPartnerAvatarUrl() == null) {
          statement.bindNull(5);
        } else {
          statement.bindString(5, entity.getPartnerAvatarUrl());
        }
        if (entity.getRequestedBy() == null) {
          statement.bindNull(6);
        } else {
          statement.bindString(6, entity.getRequestedBy());
        }
        if (entity.getUnbindRequestedBy() == null) {
          statement.bindNull(7);
        } else {
          statement.bindString(7, entity.getUnbindRequestedBy());
        }
        if (entity.getCreatedAt() == null) {
          statement.bindNull(8);
        } else {
          statement.bindString(8, entity.getCreatedAt());
        }
      }
    };
    this.__preparedStmtOfClear = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM pair";
        return _query;
      }
    };
  }

  @Override
  public Object insert(final PairEntity pair, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfPairEntity.insert(pair);
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
  public Object get(final Continuation<? super PairEntity> $completion) {
    final String _sql = "SELECT * FROM pair LIMIT 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<PairEntity>() {
      @Override
      @Nullable
      public PairEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfPairId = CursorUtil.getColumnIndexOrThrow(_cursor, "pairId");
          final int _cursorIndexOfStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "status");
          final int _cursorIndexOfPartnerId = CursorUtil.getColumnIndexOrThrow(_cursor, "partnerId");
          final int _cursorIndexOfPartnerUsername = CursorUtil.getColumnIndexOrThrow(_cursor, "partnerUsername");
          final int _cursorIndexOfPartnerAvatarUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "partnerAvatarUrl");
          final int _cursorIndexOfRequestedBy = CursorUtil.getColumnIndexOrThrow(_cursor, "requestedBy");
          final int _cursorIndexOfUnbindRequestedBy = CursorUtil.getColumnIndexOrThrow(_cursor, "unbindRequestedBy");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final PairEntity _result;
          if (_cursor.moveToFirst()) {
            final String _tmpPairId;
            _tmpPairId = _cursor.getString(_cursorIndexOfPairId);
            final String _tmpStatus;
            _tmpStatus = _cursor.getString(_cursorIndexOfStatus);
            final String _tmpPartnerId;
            if (_cursor.isNull(_cursorIndexOfPartnerId)) {
              _tmpPartnerId = null;
            } else {
              _tmpPartnerId = _cursor.getString(_cursorIndexOfPartnerId);
            }
            final String _tmpPartnerUsername;
            if (_cursor.isNull(_cursorIndexOfPartnerUsername)) {
              _tmpPartnerUsername = null;
            } else {
              _tmpPartnerUsername = _cursor.getString(_cursorIndexOfPartnerUsername);
            }
            final String _tmpPartnerAvatarUrl;
            if (_cursor.isNull(_cursorIndexOfPartnerAvatarUrl)) {
              _tmpPartnerAvatarUrl = null;
            } else {
              _tmpPartnerAvatarUrl = _cursor.getString(_cursorIndexOfPartnerAvatarUrl);
            }
            final String _tmpRequestedBy;
            if (_cursor.isNull(_cursorIndexOfRequestedBy)) {
              _tmpRequestedBy = null;
            } else {
              _tmpRequestedBy = _cursor.getString(_cursorIndexOfRequestedBy);
            }
            final String _tmpUnbindRequestedBy;
            if (_cursor.isNull(_cursorIndexOfUnbindRequestedBy)) {
              _tmpUnbindRequestedBy = null;
            } else {
              _tmpUnbindRequestedBy = _cursor.getString(_cursorIndexOfUnbindRequestedBy);
            }
            final String _tmpCreatedAt;
            if (_cursor.isNull(_cursorIndexOfCreatedAt)) {
              _tmpCreatedAt = null;
            } else {
              _tmpCreatedAt = _cursor.getString(_cursorIndexOfCreatedAt);
            }
            _result = new PairEntity(_tmpPairId,_tmpStatus,_tmpPartnerId,_tmpPartnerUsername,_tmpPartnerAvatarUrl,_tmpRequestedBy,_tmpUnbindRequestedBy,_tmpCreatedAt);
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
