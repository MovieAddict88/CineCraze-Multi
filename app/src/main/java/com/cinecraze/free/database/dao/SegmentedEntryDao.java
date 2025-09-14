package com.cinecraze.free.database.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Delete;

import com.cinecraze.free.database.entities.SegmentedEntryEntity;

import java.util.List;

@Dao
public interface SegmentedEntryDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<SegmentedEntryEntity> entries);
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(SegmentedEntryEntity entry);
    
    @Query("SELECT * FROM segmented_entries")
    List<SegmentedEntryEntity> getAllEntries();
    
    @Query("SELECT * FROM segmented_entries WHERE main_category = :category")
    List<SegmentedEntryEntity> getEntriesByCategory(String category);
    
    @Query("SELECT * FROM segmented_entries WHERE title LIKE '%' || :title || '%'")
    List<SegmentedEntryEntity> searchByTitle(String title);
    
    @Query("SELECT COUNT(*) FROM segmented_entries")
    int getEntriesCount();
    
    @Delete
    void deleteAll(List<SegmentedEntryEntity> entries);
    
    @Query("DELETE FROM segmented_entries")
    void deleteAll();
    
    @Query("DELETE FROM segmented_entries WHERE main_category = :category")
    void deleteByCategory(String category);
    
    // Optimized pagination queries with indexes
    @Query("SELECT * FROM segmented_entries ORDER BY title ASC LIMIT :limit OFFSET :offset")
    List<SegmentedEntryEntity> getEntriesPaged(int limit, int offset);
    
    @Query("SELECT * FROM segmented_entries WHERE main_category = :category ORDER BY title ASC LIMIT :limit OFFSET :offset")
    List<SegmentedEntryEntity> getEntriesByCategoryPaged(String category, int limit, int offset);
    
    @Query("SELECT * FROM segmented_entries WHERE title LIKE '%' || :title || '%' ORDER BY title ASC LIMIT :limit OFFSET :offset")
    List<SegmentedEntryEntity> searchByTitlePaged(String title, int limit, int offset);
    
    @Query("SELECT COUNT(*) FROM segmented_entries WHERE main_category = :category")
    int getEntriesCountByCategory(String category);
    
    @Query("SELECT COUNT(*) FROM segmented_entries WHERE title LIKE '%' || :title || '%'")
    int getSearchResultsCount(String title);
    
    // Filter queries for unique values
    @Query("SELECT DISTINCT sub_category FROM segmented_entries WHERE sub_category IS NOT NULL AND sub_category != '' ORDER BY sub_category ASC")
    List<String> getUniqueGenres();
    
    @Query("SELECT DISTINCT country FROM segmented_entries WHERE country IS NOT NULL AND country != '' ORDER BY country ASC")
    List<String> getUniqueCountries();
    
    @Query("SELECT DISTINCT year FROM segmented_entries WHERE year IS NOT NULL AND year != '' AND year != '0' ORDER BY year DESC")
    List<String> getUniqueYears();
    
    // Advanced filtering with pagination
    @Query("SELECT * FROM segmented_entries WHERE " +
           "(:genre IS NULL OR sub_category = :genre) AND " +
           "(:country IS NULL OR country = :country) AND " +
           "(:year IS NULL OR year = :year) " +
           "ORDER BY title ASC LIMIT :limit OFFSET :offset")
    List<SegmentedEntryEntity> getEntriesFilteredPaged(String genre, String country, String year, int limit, int offset);
    
    @Query("SELECT COUNT(*) FROM segmented_entries WHERE " +
           "(:genre IS NULL OR sub_category = :genre) AND " +
           "(:country IS NULL OR country = :country) AND " +
           "(:year IS NULL OR year = :year)")
    int getEntriesFilteredCount(String genre, String country, String year);
    
    // Top rated entries
    @Query("SELECT * FROM segmented_entries ORDER BY CAST(rating AS REAL) DESC LIMIT :count")
    List<SegmentedEntryEntity> getTopRatedEntries(int count);
    
    // Parental control queries
    @Query("SELECT * FROM segmented_entries WHERE parental_rating IN (:allowedRatings) OR (:includeUnrated AND parental_rating IS NULL)" +
           "ORDER BY title ASC LIMIT :limit OFFSET :offset")
    List<SegmentedEntryEntity> getEntriesPagedWithRating(List<String> allowedRatings, boolean includeUnrated, int limit, int offset);

    @Query("SELECT COUNT(*) FROM segmented_entries WHERE parental_rating IN (:allowedRatings) OR (:includeUnrated AND parental_rating IS NULL)")
    int getEntriesCountWithRating(List<String> allowedRatings, boolean includeUnrated);

    @Query("SELECT * FROM segmented_entries WHERE (parental_rating IN (:allowedRatings) OR (:includeUnrated AND parental_rating IS NULL)) AND title LIKE '%' || :title || '%'" +
           "ORDER BY title ASC LIMIT :limit OFFSET :offset")
    List<SegmentedEntryEntity> searchByTitlePagedWithRating(String title, List<String> allowedRatings, boolean includeUnrated, int limit, int offset);

    @Query("SELECT COUNT(*) FROM segmented_entries WHERE (parental_rating IN (:allowedRatings) OR (:includeUnrated AND parental_rating IS NULL)) AND title LIKE '%' || :title || '%'")
    int getSearchResultsCountWithRating(String title, List<String> allowedRatings, boolean includeUnrated);

    @Query("SELECT * FROM segmented_entries WHERE main_category = :category AND (parental_rating IN (:allowedRatings) OR (:includeUnrated AND parental_rating IS NULL))" +
           "ORDER BY title ASC LIMIT :limit OFFSET :offset")
    List<SegmentedEntryEntity> getEntriesByCategoryPagedWithRating(String category, List<String> allowedRatings, boolean includeUnrated, int limit, int offset);

    @Query("SELECT COUNT(*) FROM segmented_entries WHERE main_category = :category AND (parental_rating IN (:allowedRatings) OR (:includeUnrated AND parental_rating IS NULL))")
    int getEntriesCountByCategoryWithRating(String category, List<String> allowedRatings, boolean includeUnrated);

    @Query("SELECT * FROM segmented_entries WHERE " +
           "(:genre IS NULL OR sub_category = :genre) AND " +
           "(:country IS NULL OR country = :country) AND " +
           "(:year IS NULL OR year = :year) AND " +
           "(parental_rating IN (:allowedRatings) OR (:includeUnrated AND parental_rating IS NULL)) " +
           "ORDER BY title ASC LIMIT :limit OFFSET :offset")
    List<SegmentedEntryEntity> getEntriesFilteredPagedWithRating(String genre, String country, String year, List<String> allowedRatings, boolean includeUnrated, int limit, int offset);

    @Query("SELECT COUNT(*) FROM segmented_entries WHERE " +
           "(:genre IS NULL OR sub_category = :genre) AND " +
           "(:country IS NULL OR country = :country) AND " +
           "(:year IS NULL OR year = :year) AND " +
           "(parental_rating IN (:allowedRatings) OR (:includeUnrated AND parental_rating IS NULL))")
    int getEntriesFilteredCountWithRating(String genre, String country, String year, List<String> allowedRatings, boolean includeUnrated);
    
    // Performance optimization queries
    @Query("SELECT * FROM segmented_entries WHERE id = :id")
    SegmentedEntryEntity getEntryById(int id);
    
    @Query("SELECT * FROM segmented_entries WHERE episode_count > :minEpisodes ORDER BY episode_count DESC LIMIT :limit")
    List<SegmentedEntryEntity> getLargeSeries(int minEpisodes, int limit);
    
    @Query("SELECT * FROM segmented_entries WHERE season_count > :minSeasons ORDER BY season_count DESC LIMIT :limit")
    List<SegmentedEntryEntity> getLongRunningSeries(int minSeasons, int limit);
    
    // Batch operations for better performance
    @Query("SELECT * FROM segmented_entries WHERE id IN (:ids)")
    List<SegmentedEntryEntity> getEntriesByIds(List<Integer> ids);
    
    @Query("UPDATE segmented_entries SET last_updated = :timestamp WHERE id IN (:ids)")
    void updateLastUpdated(List<Integer> ids, long timestamp);
}