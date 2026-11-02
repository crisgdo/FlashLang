package com.cristiano.flashlang.data.local

import androidx.sqlite.db.SupportSQLiteDatabase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy

class AppDatabaseMigrationTest {

    private class RecordingDatabase : InvocationHandler {
        val executedSql = mutableListOf<String>()

        override fun invoke(proxy: Any?, method: Method, args: Array<out Any?>?): Any? {
            if (method.name == "execSQL" && args != null && args.isNotEmpty()) {
                executedSql.add(args[0] as String)
                return null
            }
            return when (method.returnType) {
                java.lang.Boolean.TYPE -> false
                java.lang.Integer.TYPE -> 0
                java.lang.Long.TYPE -> 0L
                java.lang.Float.TYPE -> 0f
                java.lang.Double.TYPE -> 0.0
                java.lang.Void.TYPE -> null
                else -> null
            }
        }

        fun create(): SupportSQLiteDatabase {
            return Proxy.newProxyInstance(
                SupportSQLiteDatabase::class.java.classLoader,
                arrayOf(SupportSQLiteDatabase::class.java),
                this,
            ) as SupportSQLiteDatabase
        }
    }

    @Test
    fun migrationVersions_areCorrectlyChained() {
        assertEquals(1, AppDatabase.MIGRATION_1_2.startVersion)
        assertEquals(2, AppDatabase.MIGRATION_1_2.endVersion)

        assertEquals(2, AppDatabase.MIGRATION_2_3.startVersion)
        assertEquals(3, AppDatabase.MIGRATION_2_3.endVersion)

        assertEquals(3, AppDatabase.MIGRATION_3_4.startVersion)
        assertEquals(4, AppDatabase.MIGRATION_3_4.endVersion)

        assertEquals(4, AppDatabase.MIGRATION_4_5.startVersion)
        assertEquals(5, AppDatabase.MIGRATION_4_5.endVersion)

        assertEquals(5, AppDatabase.MIGRATION_5_6.startVersion)
        assertEquals(6, AppDatabase.MIGRATION_5_6.endVersion)
    }

    @Test
    fun migration1To2_executesExpectedStatements() {
        val recorder = RecordingDatabase()
        val db = recorder.create()

        AppDatabase.MIGRATION_1_2.migrate(db)

        val sqls = recorder.executedSql
        assertEquals(5, sqls.size)
        assertTrue(sqls.any { it.contains("ALTER TABLE cards ADD COLUMN easeFactor REAL NOT NULL DEFAULT 2.5") })
        assertTrue(sqls.any { it.contains("ALTER TABLE cards ADD COLUMN intervalDays INTEGER NOT NULL DEFAULT 0") })
        assertTrue(sqls.any { it.contains("ALTER TABLE cards ADD COLUMN repetitions INTEGER NOT NULL DEFAULT 0") })
        assertTrue(sqls.any { it.contains("ALTER TABLE cards ADD COLUMN dueAt INTEGER NOT NULL DEFAULT 0") })
        assertTrue(sqls.any { it.startsWith("UPDATE cards SET dueAt =") })
    }

    @Test
    fun migration2To3_executesExpectedStatements() {
        val recorder = RecordingDatabase()
        val db = recorder.create()

        AppDatabase.MIGRATION_2_3.migrate(db)

        val sqls = recorder.executedSql
        assertEquals(2, sqls.size)
        assertTrue(sqls.any { it.contains("ALTER TABLE cards ADD COLUMN ipa TEXT") })
        assertTrue(sqls.any { it.contains("ALTER TABLE cards ADD COLUMN note TEXT") })
    }

    @Test
    fun migration3To4_executesExpectedStatements() {
        val recorder = RecordingDatabase()
        val db = recorder.create()

        AppDatabase.MIGRATION_3_4.migrate(db)

        val sqls = recorder.executedSql
        assertEquals(1, sqls.size)
        val createTableSql = sqls.first()
        assertTrue(createTableSql.contains("CREATE TABLE IF NOT EXISTS user_prompts"))
        assertTrue(createTableSql.contains("id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL"))
        assertTrue(createTableSql.contains("title TEXT NOT NULL"))
        assertTrue(createTableSql.contains("description TEXT NOT NULL"))
        assertTrue(createTableSql.contains("body TEXT NOT NULL"))
        assertTrue(createTableSql.contains("generatesDeck INTEGER NOT NULL"))
        assertTrue(createTableSql.contains("createdAt INTEGER NOT NULL"))
        assertTrue(createTableSql.contains("updatedAt INTEGER NOT NULL"))
    }

    @Test
    fun migration4To5_executesExpectedStatements() {
        val recorder = RecordingDatabase()
        val db = recorder.create()

        AppDatabase.MIGRATION_4_5.migrate(db)

        val sqls = recorder.executedSql
        assertEquals(1, sqls.size)
        assertTrue(sqls.first().contains("ALTER TABLE cards ADD COLUMN format TEXT NOT NULL DEFAULT 'basic'"))
    }

    @Test
    fun migration5To6_executesExpectedStatements() {
        val recorder = RecordingDatabase()
        val db = recorder.create()

        AppDatabase.MIGRATION_5_6.migrate(db)

        val sqls = recorder.executedSql
        assertEquals(1, sqls.size)
        assertTrue(sqls.first().contains("ALTER TABLE decks ADD COLUMN category TEXT"))
    }

    @Test
    fun allMigrations_executeSequentiallyWithoutErrors() {
        val recorder = RecordingDatabase()
        val db = recorder.create()

        val migrations = listOf(
            AppDatabase.MIGRATION_1_2,
            AppDatabase.MIGRATION_2_3,
            AppDatabase.MIGRATION_3_4,
            AppDatabase.MIGRATION_4_5,
            AppDatabase.MIGRATION_5_6,
        )

        migrations.forEach { it.migrate(db) }

        assertEquals(10, recorder.executedSql.size)
    }

    @Test
    fun exportedSchema_whenGenerated_matchesVersion6Structure() {
        val possiblePaths = listOf(
            File("schemas/com.cristiano.flashlang.data.local.AppDatabase/6.json"),
            File("app/schemas/com.cristiano.flashlang.data.local.AppDatabase/6.json"),
        )
        val schemaFile = possiblePaths.firstOrNull { it.exists() }
        if (schemaFile != null) {
            val content = schemaFile.readText()
            assertTrue(content.contains("\"version\": 6"))
            assertTrue(content.contains("\"tableName\": \"decks\""))
            assertTrue(content.contains("\"tableName\": \"cards\""))
            assertTrue(content.contains("\"tableName\": \"user_prompts\""))
            assertTrue(content.contains("\"columnName\": \"category\""))
            assertTrue(content.contains("\"columnName\": \"easeFactor\""))
            assertTrue(content.contains("\"columnName\": \"format\""))
            assertTrue(content.contains("\"columnName\": \"ipa\""))
            assertTrue(content.contains("\"columnName\": \"note\""))
        }
    }
}

