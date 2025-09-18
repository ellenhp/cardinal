package earth.maps.cardinal.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [PlaceEntity::class, OfflineArea::class, RoutingProfile::class, DownloadedTile::class],
    version = 6,
    exportSchema = false
)
@TypeConverters(TileTypeConverter::class, DownloadStatusConverter::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun placeDao(): PlaceDao
    abstract fun offlineAreaDao(): OfflineAreaDao
    abstract fun routingProfileDao(): RoutingProfileDao
    abstract fun downloadedTileDao(): DownloadedTileDao

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

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "cardinal_maps_database"
                ).addMigrations(MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
