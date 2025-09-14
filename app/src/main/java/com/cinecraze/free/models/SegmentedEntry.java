package com.cinecraze.free.models;

import com.google.gson.annotations.SerializedName;

/**
 * Lightweight entry model for segmented JSON processing
 * Contains only essential fields to reduce memory footprint
 */
public class SegmentedEntry {
    
    @SerializedName("id")
    private int id;
    
    @SerializedName("title")
    private String title;
    
    @SerializedName("subCategory")
    private String subCategory;
    
    @SerializedName("mainCategory")
    private String mainCategory;
    
    @SerializedName("country")
    private String country;
    
    @SerializedName("description")
    private String description;
    
    @SerializedName("poster")
    private String poster;
    
    @SerializedName("thumbnail")
    private String thumbnail;
    
    @SerializedName("rating")
    private Object rating;
    
    @SerializedName("duration")
    private String duration;
    
    @SerializedName("year")
    private Object year;
    
    @SerializedName("parentalRating")
    private String parentalRating;
    
    @SerializedName("hasServers")
    private boolean hasServers;
    
    @SerializedName("hasSeasons")
    private boolean hasSeasons;
    
    @SerializedName("hasRelated")
    private boolean hasRelated;
    
    @SerializedName("episodeCount")
    private int episodeCount;
    
    @SerializedName("seasonCount")
    private int seasonCount;

    // Constructors
    public SegmentedEntry() {}
    
    public SegmentedEntry(Entry entry) {
        this.id = entry.getId();
        this.title = entry.getTitle();
        this.subCategory = entry.getSubCategory();
        this.mainCategory = entry.getMainCategory();
        this.country = entry.getCountry();
        this.description = entry.getDescription();
        this.poster = entry.getPoster();
        this.thumbnail = entry.getThumbnail();
        this.rating = entry.getRating();
        this.duration = entry.getDuration();
        this.year = entry.getYear();
        this.parentalRating = entry.getParentalRating();
        
        // Calculate metadata
        this.hasServers = entry.getServers() != null && !entry.getServers().isEmpty();
        this.hasSeasons = entry.getSeasons() != null && !entry.getSeasons().isEmpty();
        this.hasRelated = entry.getRelated() != null && !entry.getRelated().isEmpty();
        
        // Calculate episode and season counts
        this.episodeCount = 0;
        this.seasonCount = 0;
        if (entry.getSeasons() != null) {
            this.seasonCount = entry.getSeasons().size();
            for (Season season : entry.getSeasons()) {
                if (season.getEpisodes() != null) {
                    this.episodeCount += season.getEpisodes().size();
                }
            }
        }
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
    
    public Object getRating() { return rating; }
    public void setRating(Object rating) { this.rating = rating; }
    
    public String getDuration() { return duration; }
    public void setDuration(String duration) { this.duration = duration; }
    
    public Object getYear() { return year; }
    public void setYear(Object year) { this.year = year; }
    
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
    
    // Utility methods
    public float getRatingFloat() {
        if (rating instanceof Number) {
            return ((Number) rating).floatValue();
        } else if (rating instanceof String) {
            try {
                return Float.parseFloat((String) rating);
            } catch (NumberFormatException e) {
                return 0.0f;
            }
        }
        return 0.0f;
    }
    
    public int getYearInt() {
        if (year instanceof Number) {
            return ((Number) year).intValue();
        } else if (year instanceof String) {
            try {
                return Integer.parseInt((String) year);
            } catch (NumberFormatException e) {
                return 0;
            }
        }
        return 0;
    }
    
    public String getImageUrl() {
        return poster != null ? poster : thumbnail;
    }
    
    /**
     * Convert to full Entry object when needed
     */
    public Entry toFullEntry() {
        Entry entry = new Entry();
        entry.setTitle(title);
        entry.setSubCategory(subCategory);
        entry.setMainCategory(mainCategory);
        entry.setCountry(country);
        entry.setDescription(description);
        entry.setPoster(poster);
        entry.setThumbnail(thumbnail);
        entry.setRating(rating);
        entry.setDuration(duration);
        entry.setYear(year);
        entry.setParentalRating(parentalRating);
        // Note: Servers, Seasons, and Related will be loaded separately
        return entry;
    }
}