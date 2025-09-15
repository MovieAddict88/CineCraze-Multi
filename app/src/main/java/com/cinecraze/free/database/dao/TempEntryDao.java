package com.cinecraze.free.database.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.cinecraze.free.database.entities.TempEntryEntity;

import java.util.List;

@Dao
public interface TempEntryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<TempEntryEntity> entries);

    @Query("DELETE FROM entries_temp")
    void deleteAll();
}
