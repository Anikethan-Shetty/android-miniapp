package com.rakuten.tech.mobile.miniapp.storage.database

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import net.zetetic.database.sqlcipher.SQLiteDatabase

/**
 * Custom SupportFactory for SQLCipher 4.10.0+
 * This factory adapts the new SQLCipher API to work with AndroidX Room
 */
internal class SQLCipherSupportFactory(private val passphrase: ByteArray) : SupportSQLiteOpenHelper.Factory {

    init {
        System.loadLibrary("sqlcipher")
    }

    override fun create(configuration: SupportSQLiteOpenHelper.Configuration): SupportSQLiteOpenHelper {
        return SQLCipherSupportHelper(
            configuration.context,
            configuration.name,
            configuration.callback,
            passphrase
        )
    }

    private class SQLCipherSupportHelper(
        private val context: Context,
        private val name: String?,
        private val callback: SupportSQLiteOpenHelper.Callback,
        private val passphrase: ByteArray
    ) : SupportSQLiteOpenHelper {

        private var database: SQLiteDatabase? = null

        override fun getDatabaseName(): String? = name

        override fun setWriteAheadLoggingEnabled(enabled: Boolean) {
            // WAL mode can be set via PRAGMA after opening
        }

        override fun getWritableDatabase(): SupportSQLiteDatabase {
            if (database == null || !database!!.isOpen) {
                val dbFile = context.getDatabasePath(name)
                dbFile.parentFile?.mkdirs()
                database = SQLiteDatabase.openOrCreateDatabase(
                    dbFile.absolutePath,
                    passphrase,
                    null,
                    null
                )
                database?.let { db ->
                    db.version = callback.version
                    callback.onConfigure(SQLCipherSupportDatabase(db))
                    if (db.version == 0) {
                        callback.onCreate(SQLCipherSupportDatabase(db))
                    } else {
                        callback.onOpen(SQLCipherSupportDatabase(db))
                    }
                }
            }
            return SQLCipherSupportDatabase(database!!)
        }

        override fun getReadableDatabase(): SupportSQLiteDatabase = getWritableDatabase()

        override fun close() {
            database?.close()
            database = null
        }
    }

    private class SQLCipherSupportDatabase(
        private val delegate: SQLiteDatabase
    ) : SupportSQLiteDatabase {

        override fun compileStatement(sql: String) =
            delegate.compileStatement(sql).let { AndroidSQLiteStatement(it) }

        override fun beginTransaction() = delegate.beginTransaction()

        override fun beginTransactionNonExclusive() = delegate.beginTransactionNonExclusive()

        override fun beginTransactionWithListener(transactionListener: android.database.sqlite.SQLiteTransactionListener) {
            delegate.beginTransactionWithListener(transactionListener)
        }

        override fun beginTransactionWithListenerNonExclusive(transactionListener: android.database.sqlite.SQLiteTransactionListener) {
            delegate.beginTransactionWithListenerNonExclusive(transactionListener)
        }

        override fun endTransaction() = delegate.endTransaction()

        override fun setTransactionSuccessful() = delegate.setTransactionSuccessful()

        override fun inTransaction() = delegate.inTransaction()

        override fun isDbLockedByCurrentThread() = delegate.isDbLockedByCurrentThread

        override fun yieldIfContendedSafely() = delegate.yieldIfContendedSafely()

        override fun yieldIfContendedSafely(sleepAfterYieldDelay: Long) =
            delegate.yieldIfContendedSafely(sleepAfterYieldDelay)

        override fun getVersion(): Int = delegate.version

        override fun setVersion(version: Int) {
            delegate.version = version
        }

        override fun getMaximumSize(): Long = delegate.maximumSize

        override fun setMaximumSize(numBytes: Long): Long {
            delegate.maximumSize = numBytes
            return numBytes
        }

        override fun getPageSize(): Long = delegate.pageSize

        override fun setPageSize(numBytes: Long) {
            delegate.pageSize = numBytes
        }

        override fun query(query: String) = delegate.rawQuery(query, null).let { AndroidCursor(it) }

        override fun query(query: String, bindArgs: Array<out Any?>) =
            delegate.rawQuery(query, bindArgs.map { it?.toString() }.toTypedArray())
                .let { AndroidCursor(it) }

        override fun query(query: androidx.sqlite.db.SupportSQLiteQuery) =
            query(query.sql, emptyArray())

        override fun query(query: androidx.sqlite.db.SupportSQLiteQuery, cancellationSignal: android.os.CancellationSignal?) =
            query(query.sql, emptyArray())

        override fun insert(table: String, conflictAlgorithm: Int, values: android.content.ContentValues) =
            delegate.insertWithOnConflict(table, null, values, conflictAlgorithm)

        override fun delete(table: String, whereClause: String?, whereArgs: Array<out Any?>?) =
            delegate.delete(table, whereClause, whereArgs?.map { it?.toString() }?.toTypedArray())

        override fun update(
            table: String,
            conflictAlgorithm: Int,
            values: android.content.ContentValues,
            whereClause: String?,
            whereArgs: Array<out Any?>?
        ) = delegate.updateWithOnConflict(
            table,
            values,
            whereClause,
            whereArgs?.map { it?.toString() }?.toTypedArray(),
            conflictAlgorithm
        )

        override fun execSQL(sql: String) = delegate.execSQL(sql)

        override fun execSQL(sql: String, bindArgs: Array<out Any?>) =
            delegate.execSQL(sql, bindArgs)

        override fun isReadOnly() = delegate.isReadOnly

        override fun isOpen() = delegate.isOpen

        override fun needUpgrade(newVersion: Int) = delegate.needUpgrade(newVersion)

        override fun getPath(): String? = delegate.path

        override fun setLocale(locale: java.util.Locale) = delegate.setLocale(locale)

        override fun setMaxSqlCacheSize(cacheSize: Int) = delegate.setMaxSqlCacheSize(cacheSize)

        override fun setForeignKeyConstraintsEnabled(enable: Boolean) {
            if (enable) {
                delegate.execSQL("PRAGMA foreign_keys = ON")
            } else {
                delegate.execSQL("PRAGMA foreign_keys = OFF")
            }
        }

        override fun enableWriteAheadLogging() = delegate.enableWriteAheadLogging()

        override fun disableWriteAheadLogging() = delegate.disableWriteAheadLogging()

        override fun isWriteAheadLoggingEnabled() = delegate.isWriteAheadLoggingEnabled

        override fun getAttachedDbs(): List<android.util.Pair<String, String>>? = delegate.attachedDbs

        override fun isDatabaseIntegrityOk() = delegate.isDatabaseIntegrityOk

        override fun close() = delegate.close()
    }

    private class AndroidCursor(private val delegate: android.database.Cursor) : android.database.Cursor by delegate

    private class AndroidSQLiteStatement(
        private val delegate: net.zetetic.database.sqlcipher.SQLiteStatement
    ) : androidx.sqlite.db.SupportSQLiteStatement {

        override fun execute() = delegate.execute()

        override fun executeUpdateDelete() = delegate.executeUpdateDelete()

        override fun executeInsert() = delegate.executeInsert()

        override fun simpleQueryForLong() = delegate.simpleQueryForLong()

        override fun simpleQueryForString() = delegate.simpleQueryForString()

        override fun bindNull(index: Int) = delegate.bindNull(index)

        override fun bindLong(index: Int, value: Long) = delegate.bindLong(index, value)

        override fun bindDouble(index: Int, value: Double) = delegate.bindDouble(index, value)

        override fun bindString(index: Int, value: String) = delegate.bindString(index, value)

        override fun bindBlob(index: Int, value: ByteArray) = delegate.bindBlob(index, value)

        override fun clearBindings() = delegate.clearBindings()

        override fun close() = delegate.close()
    }
}
