package com.cinecraze.free.database;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.cinecraze.free.database.dao.EntryDao;
import com.cinecraze.free.database.dao.TempEntryDao;
import com.cinecraze.free.database.dao.CacheMetadataDao;
import com.cinecraze.free.database.entities.EntryEntity;
import com.cinecraze.free.database.entities.CacheMetadataEntity;

import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

@Database(
    entities = {EntryEntity.class, CacheMetadataEntity.class, com.cinecraze.free.database.entities.DownloadItemEntity.class, com.cinecraze.free.database.entities.TempEntryEntity.class},
    version = 5, // Incremented version because of new table
    exportSchema = false
)
public abstract class CineCrazeDatabase extends RoomDatabase {

    private static final String DATABASE_NAME = "cinecraze_database";
    private static CineCrazeDatabase instance;

    public abstract EntryDao entryDao();
    public abstract TempEntryDao tempEntryDao();
    public abstract CacheMetadataDao cacheMetadataDao();
    public abstract com.cinecraze.free.database.dao.DownloadItemDao downloadItemDao();

    static final Migration MIGRATION_2_3 = new Migration(2, 3) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE cache_metadata ADD COLUMN playlist_urls TEXT");
        }
    };

    static final Migration MIGRATION_3_4 = new Migration(3, 4) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE entries ADD COLUMN parental_rating TEXT");
        }
    };

    // Migration to add the new temp table
    static final Migration MIGRATION_4_5 = new Migration(4, 5) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            database.execSQL("CREATE TABLE IF NOT EXISTS `entries_temp` (`id` INTEGER NOT NULL, `title` TEXT, `sub_category` TEXT, `country` TEXT, `description` TEXT, `poster` TEXT, `thumbnail` TEXT, `rating` TEXT, `duration` TEXT, `year` TEXT, `main_category` TEXT, `servers_json` TEXT, `seasons_json` TEXT, `related_json` TEXT, `parental_rating` TEXT, PRIMARY KEY(`id`))");
        }
    };

    public static synchronized CineCrazeDatabase getInstance(Context context) {
        if (instance == null) {
            instance = Room.databaseBuilder(
                context.getApplicationContext(),
                CineCrazeDatabase.class,
                DATABASE_NAME
            )
            .addMigrations(MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
            .allowMainThreadQueries() // For simplicity, but ideally use background threads
            .build();
        }
        return instance;
    }
}