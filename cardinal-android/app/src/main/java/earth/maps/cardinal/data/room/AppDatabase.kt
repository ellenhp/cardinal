/*
 *     Cardinal Maps
 *     Copyright (C) 2025 Cardinal Maps Authors
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package earth.maps.cardinal.data.room

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import earth.maps.cardinal.data.DownloadStatusConverter

@Database(
    entities = [OfflineArea::class, RoutingProfile::class, DownloadedTile::class, SavedList::class, SavedPlace::class, ListItem::class],
    version = 10,
    exportSchema = false
)
@TypeConverters(TileTypeConverter::class, DownloadStatusConverter::class, ItemTypeConverter::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun offlineAreaDao(): OfflineAreaDao
    abstract fun routingProfileDao(): RoutingProfileDao
    abstract fun downloadedTileDao(): DownloadedTileDao
    abstract fun savedListDao(): SavedListDao
    abstract fun savedPlaceDao(): SavedPlaceDao
    abstract fun listItemDao(): ListItemDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE places ADD COLUMN countryCode TEXT")
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS routing_profiles (
                        id TEXT PRIMARY KEY NOT NULL,
                        name TEXT NOT NULL,
                        routingMode TEXT NOT NULL,
                        optionsJson TEXT NOT NULL,
                        isDefault INTEGER NOT NULL DEFAULT 0,
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL
                    )
                """.trimIndent()
                )
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS downloaded_tiles (
                        id TEXT PRIMARY KEY NOT NULL,
                        areaId TEXT NOT NULL,
                        tileType TEXT NOT NULL,
                        downloadTimestamp INTEGER NOT NULL,
                        retryCount INTEGER NOT NULL DEFAULT 0,
                        zoom INTEGER,
                        tileX INTEGER,
                        tileY INTEGER,
                        hierarchyLevel INTEGER,
                        tileIndex INTEGER
                    )
                """.trimIndent()
                )
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE offline_areas ADD COLUMN paused INTEGER NOT NULL DEFAULT 0"
                )
            }
        }

        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE places ADD COLUMN isTransitStop INTEGER NOT NULL DEFAULT 0"
                )
            }
        }

        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Create saved_lists table
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS saved_lists (
                        id TEXT PRIMARY KEY NOT NULL,
                        name TEXT NOT NULL,
                        description TEXT,
                        isRoot INTEGER NOT NULL DEFAULT 0,
                        isCollapsed INTEGER NOT NULL DEFAULT 0,
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL
                    )
                """.trimIndent()
                )

                // Create saved_places table
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS saved_places (
                        id TEXT PRIMARY KEY NOT NULL,
                        placeId INTEGER,
                        customName TEXT,
                        customDescription TEXT,
                        name TEXT NOT NULL,
                        type TEXT NOT NULL,
                        icon TEXT NOT NULL,
                        latitude REAL NOT NULL,
                        longitude REAL NOT NULL,
                        houseNumber TEXT,
                        road TEXT,
                        city TEXT,
                        state TEXT,
                        postcode TEXT,
                        country TEXT,
                        countryCode TEXT,
                        isTransitStop INTEGER NOT NULL DEFAULT 0,
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL
                    )
                """.trimIndent()
                )

                // Create list_items table
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS list_items (
                        listId TEXT NOT NULL,
                        itemId TEXT NOT NULL,
                        itemType TEXT NOT NULL,
                        position INTEGER NOT NULL,
                        addedAt INTEGER NOT NULL,
                        PRIMARY KEY (listId, itemId, itemType),
                        FOREIGN KEY (listId) REFERENCES saved_lists(id) ON DELETE CASCADE
                    )
                """.trimIndent()
                )

                // Add indices for list_items
                db.execSQL("CREATE INDEX IF NOT EXISTS index_list_items_listId_position ON list_items (listId, position)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_list_items_itemId_itemType ON list_items (itemId, itemType)")

                // Add foreign key constraint for list_items
                // Note: SQLite doesn't enforce foreign key constraints by default, but we define it for documentation
            }
        }

        private val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE saved_places ADD COLUMN isPinned INTEGER NOT NULL DEFAULT 0"
                )
            }
        }

        private val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE saved_places ADD COLUMN transitStopId TEXT"
                )
            }
        }


        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext, AppDatabase::class.java, "cardinal_maps_database"
                ).addMigrations(
                    MIGRATION_2_3,
                    MIGRATION_3_4,
                    MIGRATION_4_5,
                    MIGRATION_5_6,
                    MIGRATION_6_7,
                    MIGRATION_7_8,
                    MIGRATION_8_9,
                    MIGRATION_9_10,
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
