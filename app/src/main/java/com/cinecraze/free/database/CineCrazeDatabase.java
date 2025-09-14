package com.cinecraze.free.database;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.cinecraze.free.database.dao.EntryDao;
import com.cinecraze.free.database.dao.SegmentedEntryDao;
import com.cinecraze.free.database.dao.CacheMetadataDao;
import com.cinecraze.free.database.entities.EntryEntity;
import com.cinecraze.free.database.entities.SegmentedEntryEntity;
import com.cinecraze.free.database.entities.CacheMetadataEntity;

import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

@Database(
    entities = {EntryEntity.class, SegmentedEntryEntity.class, CacheMetadataEntity.class, com.cinecraze.free.database.entities.DownloadItemEntity.class},
    version = 5,
    exportSchema = false
)
public abstract class CineCrazeDatabase extends RoomDatabase {
    
    private static final String DATABASE_NAME = "cinecraze_database";
    private static CineCrazeDatabase instance;
    
    public abstract EntryDao entryDao();
    public abstract SegmentedEntryDao segmentedEntryDao();
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

    static final Migration MIGRATION_4_5 = new Migration(4, 5) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            // Create segmented_entries table
            database.execSQL("CREATE TABLE IF NOT EXISTS segmented_entries (" +
                "id INTEGER PRIMARY KEY NOT NULL, " +
                "title TEXT, " +
                "sub_category TEXT, " +
                "main_category TEXT, " +
                "country TEXT, " +
                "description TEXT, " +
                "poster TEXT, " +
                "thumbnail TEXT, " +
                "rating TEXT, " +
                "duration TEXT, " +
                "year TEXT, " +
                "parental_rating TEXT, " +
                "has_servers INTEGER NOT NULL, " +
                "has_seasons INTEGER NOT NULL, " +
                "has_related INTEGER NOT NULL, " +
                "episode_count INTEGER NOT NULL, " +
                "season_count INTEGER NOT NULL, " +
                "last_updated INTEGER NOT NULL" +
                ")");
            
            // Create indexes for better performance
            database.execSQL("CREATE INDEX IF NOT EXISTS index_segmented_entries_main_category ON segmented_entries (main_category)");
            database.execSQL("CREATE INDEX IF NOT EXISTS index_segmented_entries_sub_category ON segmented_entries (sub_category)");
            database.execSQL("CREATE INDEX IF NOT EXISTS index_segmented_entries_country ON segmented_entries (country)");
            database.execSQL("CREATE INDEX IF NOT EXISTS index_segmented_entries_year ON segmented_entries (year)");
            database.execSQL("CREATE INDEX IF NOT EXISTS index_segmented_entries_title ON segmented_entries (title)");
            database.execSQL("CREATE INDEX IF NOT EXISTS index_segmented_entries_parental_rating ON segmented_entries (parental_rating)");
            database.execSQL("CREATE INDEX IF NOT EXISTS index_segmented_entries_episode_count ON segmented_entries (episode_count)");
            database.execSQL("CREATE INDEX IF NOT EXISTS index_segmented_entries_season_count ON segmented_entries (season_count)");
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