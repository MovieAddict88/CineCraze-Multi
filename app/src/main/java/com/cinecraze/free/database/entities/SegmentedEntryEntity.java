package com.cinecraze.free.database.entities;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.ColumnInfo;
import androidx.room.Index;

/**
 * Optimized database entity for segmented entries
 * Stores only essential fields to reduce memory usage and improve query performance
 */
@Entity(
    tableName = "segmented_entries",
    indices = {
        @Index(value = {"main_category"}),
        @Index(value = {"sub_category"}),
        @Index(value = {"country"}),
        @Index(value = {"year"}),
        @Index(value = {"title"}),
        @Index(value = {"parental_rating"}),
        @Index(value = {"main_category", "sub_category"}),
        @Index(value = {"main_category", "country"}),
        @Index(value = {"main_category", "year"}),
        @Index(value = {"episode_count"}),
        @Index(value = {"season_count"})
    }
)
public class SegmentedEntryEntity {
    
    @PrimaryKey
    private int id;
    
    @ColumnInfo(name = "title")
    private String title;
    
    @ColumnInfo(name = "sub_category")
    private String subCategory;
    
    @ColumnInfo(name = "main_category")
    private String mainCategory;
    
    @ColumnInfo(name = "country")
    private String country;
    
    @ColumnInfo(name = "description")
    private String description;
    
    @ColumnInfo(name = "poster")
    private String poster;
    
    @ColumnInfo(name = "thumbnail")
    private String thumbnail;
    
    @ColumnInfo(name = "rating")
    private String rating;
    
    @ColumnInfo(name = "duration")
    private String duration;
    
    @ColumnInfo(name = "year")
    private String year;
    
    @ColumnInfo(name = "parental_rating")
    private String parentalRating;
    
    @ColumnInfo(name = "has_servers")
    private boolean hasServers;
    
    @ColumnInfo(name = "has_seasons")
    private boolean hasSeasons;
    
    @ColumnInfo(name = "has_related")
    private boolean hasRelated;
    
    @ColumnInfo(name = "episode_count")
    private int episodeCount;
    
    @ColumnInfo(name = "season_count")
    private int seasonCount;
    
    @ColumnInfo(name = "last_updated")
    private long lastUpdated;

    // Constructor
    public SegmentedEntryEntity() {
        this.lastUpdated = System.currentTimeMillis();
    }
    
    // Getters and setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    
    public String getSubCategory() { return subCategory; }
    public void setSubCategory(String subCategory) { this.subCategory = subCategory; }
    
    public String getMainCategory() { return mainCategory; }
    public void setMainCategory(String mainCategory) { this.mainCategory = mainCategory; }
    
    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public String getPoster() { return poster; }
    public void setPoster(String poster) { this.poster = poster; }
    
    public String getThumbnail() { return thumbnail; }
    public void setThumbnail(String thumbnail) { this.thumbnail = thumbnail; }
    
    public String getRating() { return rating; }
    public void setRating(String rating) { this.rating = rating; }
    
    public String getDuration() { return duration; }
    public void setDuration(String duration) { this.duration = duration; }
    
    public String getYear() { return year; }
    public void setYear(String year) { this.year = year; }
    
    public String getParentalRating() { return parentalRating; }
    public void setParentalRating(String parentalRating) { this.parentalRating = parentalRating; }
    
    public boolean isHasServers() { return hasServers; }
    public void setHasServers(boolean hasServers) { this.hasServers = hasServers; }
    
    public boolean isHasSeasons() { return hasSeasons; }
    public void setHasSeasons(boolean hasSeasons) { this.hasSeasons = hasSeasons; }
    
    public boolean isHasRelated() { return hasRelated; }
    public void setHasRelated(boolean hasRelated) { this.hasRelated = hasRelated; }
    
    public int getEpisodeCount() { return episodeCount; }
    public void setEpisodeCount(int episodeCount) { this.episodeCount = episodeCount; }
    
    public int getSeasonCount() { return seasonCount; }
    public void setSeasonCount(int seasonCount) { this.seasonCount = seasonCount; }
    
    public long getLastUpdated() { return lastUpdated; }
    public void setLastUpdated(long lastUpdated) { this.lastUpdated = lastUpdated; }
}