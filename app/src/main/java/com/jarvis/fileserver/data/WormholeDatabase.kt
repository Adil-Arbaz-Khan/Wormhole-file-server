package com.jarvis.fileserver.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class WormholeDbHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        const val DATABASE_NAME = "wormhole_storage.db"
        const val DATABASE_VERSION = 1

        const val TABLE_SHARES = "shares"
        const val COL_ID = "id"
        const val COL_TOKEN = "token"
        const val COL_NAME = "name"
        const val COL_TARGET_PATH = "target_path"
        const val COL_IS_DIRECTORY = "is_directory"
        const val COL_IS_PASSWORD_PROTECTED = "is_password_protected"
        const val COL_PASSWORD_HASH = "password_hash"
        const val COL_PASSWORD_SALT = "password_salt"
        const val COL_ALLOW_DOWNLOAD = "allow_download"
        const val COL_EXPIRES_AT = "expires_at_millis"
        const val COL_MAX_ACCESS = "max_access_count"
        const val COL_ACCESS_COUNT = "access_count"
        const val COL_IS_REVOKED = "is_revoked"
        const val COL_CREATED_AT = "created_at_millis"

        const val TABLE_ADMINS = "admin_devices"
        const val COL_DEV_ID = "device_id"
        const val COL_DEV_NAME = "device_name"
        const val COL_DEV_TOKEN = "admin_token"
        const val COL_DEV_REG_AT = "registered_at_millis"
        const val COL_DEV_LAST_ACTIVE = "last_active_millis"
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS $TABLE_SHARES (
                $COL_ID TEXT PRIMARY KEY,
                $COL_TOKEN TEXT UNIQUE NOT NULL,
                $COL_NAME TEXT NOT NULL,
                $COL_TARGET_PATH TEXT NOT NULL,
                $COL_IS_DIRECTORY INTEGER NOT NULL,
                $COL_IS_PASSWORD_PROTECTED INTEGER NOT NULL,
                $COL_PASSWORD_HASH TEXT,
                $COL_PASSWORD_SALT TEXT,
                $COL_ALLOW_DOWNLOAD INTEGER NOT NULL,
                $COL_EXPIRES_AT INTEGER,
                $COL_MAX_ACCESS INTEGER,
                $COL_ACCESS_COUNT INTEGER NOT NULL DEFAULT 0,
                $COL_IS_REVOKED INTEGER NOT NULL DEFAULT 0,
                $COL_CREATED_AT INTEGER NOT NULL
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS $TABLE_ADMINS (
                $COL_DEV_ID TEXT PRIMARY KEY,
                $COL_DEV_NAME TEXT NOT NULL,
                $COL_DEV_TOKEN TEXT NOT NULL,
                $COL_DEV_REG_AT INTEGER NOT NULL,
                $COL_DEV_LAST_ACTIVE INTEGER NOT NULL
            )
            """.trimIndent()
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Future schema migrations
    }
}

object ShareRepository {
    private var dbHelper: WormholeDbHelper? = null

    private val _sharesFlow = MutableStateFlow<List<ShareRecord>>(emptyList())
    val sharesFlow: StateFlow<List<ShareRecord>> = _sharesFlow.asStateFlow()

    fun init(context: Context) {
        if (dbHelper == null) {
            dbHelper = WormholeDbHelper(context.applicationContext)
            refreshShares()
        }
    }

    @Synchronized
    fun createShare(share: ShareRecord): Boolean {
        val db = dbHelper?.writableDatabase ?: return false
        val cv = ContentValues().apply {
            put(WormholeDbHelper.COL_ID, share.id)
            put(WormholeDbHelper.COL_TOKEN, share.token)
            put(WormholeDbHelper.COL_NAME, share.name)
            put(WormholeDbHelper.COL_TARGET_PATH, share.targetPath)
            put(WormholeDbHelper.COL_IS_DIRECTORY, if (share.isDirectory) 1 else 0)
            put(WormholeDbHelper.COL_IS_PASSWORD_PROTECTED, if (share.isPasswordProtected) 1 else 0)
            put(WormholeDbHelper.COL_PASSWORD_HASH, share.passwordHash)
            put(WormholeDbHelper.COL_PASSWORD_SALT, share.passwordSalt)
            put(WormholeDbHelper.COL_ALLOW_DOWNLOAD, if (share.allowDownload) 1 else 0)
            put(WormholeDbHelper.COL_EXPIRES_AT, share.expiresAtMillis)
            put(WormholeDbHelper.COL_MAX_ACCESS, share.maxAccessCount)
            put(WormholeDbHelper.COL_ACCESS_COUNT, share.accessCount)
            put(WormholeDbHelper.COL_IS_REVOKED, if (share.isRevoked) 1 else 0)
            put(WormholeDbHelper.COL_CREATED_AT, share.createdAtMillis)
        }
        val rowId = db.insertWithOnConflict(WormholeDbHelper.TABLE_SHARES, null, cv, SQLiteDatabase.CONFLICT_REPLACE)
        refreshShares()
        return rowId != -1L
    }

    @Synchronized
    fun getShareByToken(token: String): ShareRecord? {
        val db = dbHelper?.readableDatabase ?: return null
        val cursor = db.query(
            WormholeDbHelper.TABLE_SHARES,
            null,
            "${WormholeDbHelper.COL_TOKEN} = ?",
            arrayOf(token),
            null, null, null
        )
        cursor.use {
            if (it.moveToFirst()) {
                return parseShareRecord(it)
            }
        }
        return null
    }

    @Synchronized
    fun getAllShares(): List<ShareRecord> {
        val db = dbHelper?.readableDatabase ?: return emptyList()
        val list = mutableListOf<ShareRecord>()
        val cursor = db.query(
            WormholeDbHelper.TABLE_SHARES,
            null,
            null, null, null, null,
            "${WormholeDbHelper.COL_CREATED_AT} DESC"
        )
        cursor.use {
            while (it.moveToNext()) {
                list.add(parseShareRecord(it))
            }
        }
        return list
    }

    @Synchronized
    fun incrementAccessCount(token: String) {
        val db = dbHelper?.writableDatabase ?: return
        db.execSQL(
            "UPDATE ${WormholeDbHelper.TABLE_SHARES} SET ${WormholeDbHelper.COL_ACCESS_COUNT} = ${WormholeDbHelper.COL_ACCESS_COUNT} + 1 WHERE ${WormholeDbHelper.COL_TOKEN} = ?",
            arrayOf(token)
        )
        refreshShares()
    }

    @Synchronized
    fun revokeShare(id: String): Boolean {
        val db = dbHelper?.writableDatabase ?: return false
        val cv = ContentValues().apply {
            put(WormholeDbHelper.COL_IS_REVOKED, 1)
        }
        val count = db.update(WormholeDbHelper.TABLE_SHARES, cv, "${WormholeDbHelper.COL_ID} = ?", arrayOf(id))
        refreshShares()
        return count > 0
    }

    @Synchronized
    fun deleteShare(id: String): Boolean {
        val db = dbHelper?.writableDatabase ?: return false
        val count = db.delete(WormholeDbHelper.TABLE_SHARES, "${WormholeDbHelper.COL_ID} = ?", arrayOf(id))
        refreshShares()
        return count > 0
    }

    @Synchronized
    fun registerAdminDevice(record: AdminDeviceRecord): Boolean {
        val db = dbHelper?.writableDatabase ?: return false
        val cv = ContentValues().apply {
            put(WormholeDbHelper.COL_DEV_ID, record.deviceId)
            put(WormholeDbHelper.COL_DEV_NAME, record.deviceName)
            put(WormholeDbHelper.COL_DEV_TOKEN, record.adminToken)
            put(WormholeDbHelper.COL_DEV_REG_AT, record.registeredAtMillis)
            put(WormholeDbHelper.COL_DEV_LAST_ACTIVE, record.lastActiveMillis)
        }
        val rowId = db.insertWithOnConflict(WormholeDbHelper.TABLE_ADMINS, null, cv, SQLiteDatabase.CONFLICT_REPLACE)
        return rowId != -1L
    }

    @Synchronized
    fun validateAdminToken(token: String): Boolean {
        if (token.isBlank()) return false
        val db = dbHelper?.readableDatabase ?: return false
        val cursor = db.query(
            WormholeDbHelper.TABLE_ADMINS,
            null,
            "${WormholeDbHelper.COL_DEV_TOKEN} = ?",
            arrayOf(token),
            null, null, null
        )
        cursor.use {
            if (it.moveToFirst()) {
                val devId = it.getString(it.getColumnIndexOrThrow(WormholeDbHelper.COL_DEV_ID))
                val updateCv = ContentValues().apply {
                    put(WormholeDbHelper.COL_DEV_LAST_ACTIVE, System.currentTimeMillis())
                }
                db.update(WormholeDbHelper.TABLE_ADMINS, updateCv, "${WormholeDbHelper.COL_DEV_ID} = ?", arrayOf(devId))
                return true
            }
        }
        return false
    }

    @Synchronized
    fun getAdminDevices(): List<AdminDeviceRecord> {
        val db = dbHelper?.readableDatabase ?: return emptyList()
        val list = mutableListOf<AdminDeviceRecord>()
        val cursor = db.query(WormholeDbHelper.TABLE_ADMINS, null, null, null, null, null, null)
        cursor.use {
            while (it.moveToNext()) {
                list.add(
                    AdminDeviceRecord(
                        deviceId = it.getString(it.getColumnIndexOrThrow(WormholeDbHelper.COL_DEV_ID)),
                        deviceName = it.getString(it.getColumnIndexOrThrow(WormholeDbHelper.COL_DEV_NAME)),
                        adminToken = it.getString(it.getColumnIndexOrThrow(WormholeDbHelper.COL_DEV_TOKEN)),
                        registeredAtMillis = it.getLong(it.getColumnIndexOrThrow(WormholeDbHelper.COL_DEV_REG_AT)),
                        lastActiveMillis = it.getLong(it.getColumnIndexOrThrow(WormholeDbHelper.COL_DEV_LAST_ACTIVE))
                    )
                )
            }
        }
        return list
    }

    private fun refreshShares() {
        val list = getAllShares()
        _sharesFlow.value = list
    }

    private fun parseShareRecord(c: android.database.Cursor): ShareRecord {
        return ShareRecord(
            id = c.getString(c.getColumnIndexOrThrow(WormholeDbHelper.COL_ID)),
            token = c.getString(c.getColumnIndexOrThrow(WormholeDbHelper.COL_TOKEN)),
            name = c.getString(c.getColumnIndexOrThrow(WormholeDbHelper.COL_NAME)),
            targetPath = c.getString(c.getColumnIndexOrThrow(WormholeDbHelper.COL_TARGET_PATH)),
            isDirectory = c.getInt(c.getColumnIndexOrThrow(WormholeDbHelper.COL_IS_DIRECTORY)) == 1,
            isPasswordProtected = c.getInt(c.getColumnIndexOrThrow(WormholeDbHelper.COL_IS_PASSWORD_PROTECTED)) == 1,
            passwordHash = c.getString(c.getColumnIndexOrThrow(WormholeDbHelper.COL_PASSWORD_HASH)),
            passwordSalt = c.getString(c.getColumnIndexOrThrow(WormholeDbHelper.COL_PASSWORD_SALT)),
            allowDownload = c.getInt(c.getColumnIndexOrThrow(WormholeDbHelper.COL_ALLOW_DOWNLOAD)) == 1,
            expiresAtMillis = if (c.isNull(c.getColumnIndexOrThrow(WormholeDbHelper.COL_EXPIRES_AT))) null else c.getLong(c.getColumnIndexOrThrow(WormholeDbHelper.COL_EXPIRES_AT)),
            maxAccessCount = if (c.isNull(c.getColumnIndexOrThrow(WormholeDbHelper.COL_MAX_ACCESS))) null else c.getInt(c.getColumnIndexOrThrow(WormholeDbHelper.COL_MAX_ACCESS)),
            accessCount = c.getInt(c.getColumnIndexOrThrow(WormholeDbHelper.COL_ACCESS_COUNT)),
            isRevoked = c.getInt(c.getColumnIndexOrThrow(WormholeDbHelper.COL_IS_REVOKED)) == 1,
            createdAtMillis = c.getLong(c.getColumnIndexOrThrow(WormholeDbHelper.COL_CREATED_AT))
        )
    }
}
