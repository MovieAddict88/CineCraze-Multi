package com.cinecraze.free.repository;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.cinecraze.free.database.CineCrazeDatabase;
import com.cinecraze.free.database.entities.SegmentedEntryEntity;
import com.cinecraze.free.models.Entry;
import com.cinecraze.free.models.SegmentedEntry;
import com.cinecraze.free.models.Playlist;
import com.cinecraze.free.models.PlaylistIndex;
import com.cinecraze.free.models.Category;
import com.cinecraze.free.net.ApiService;
import com.cinecraze.free.net.RetrofitClient;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Optimized data repository for handling large datasets (50,000+ entries)
 * Uses segmented approach to reduce memory usage and improve performance
 */
public class SegmentedDataRepository {

    private static final String TAG = "SegmentedDataRepository";
    private static final String CACHE_KEY_SEGMENTED = "segmented_data";
    private static final long CACHE_EXPIRY_HOURS = 24;
    public static final int DEFAULT_PAGE_SIZE = 20;
    public static final int BATCH_SIZE = 1000; // Process data in batches of 1000

    private CineCrazeDatabase database;
    private ApiService apiService;
    private Gson gson = new Gson();
    private final ExecutorService executor = Executors.newFixedThreadPool(4); // Multiple threads for better performance
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private Context context;

    public interface SegmentedDataCallback {
        void onSuccess(List<SegmentedEntry> entries);
        void onError(String error);
    }

    public interface PaginatedSegmentedCallback {
        void onSuccess(List<SegmentedEntry> entries, boolean hasMorePages, int totalCount);
        void onError(String error);
    }

    public interface ProgressCallback {
        void onProgress(int current, int total, String message);
        void onComplete();
        void onError(String error);
    }

    public SegmentedDataRepository(Context context) {
        this.context = context;
        this.database = CineCrazeDatabase.getInstance(context);
        this.apiService = RetrofitClient.getClient().create(ApiService.class);
    }

    /**
     * Load segmented data with pagination
     */
    public void getPaginatedSegmentedData(int page, int pageSize, PaginatedSegmentedCallback callback) {
        executor.execute(() -> {
            try {
                int offset = page * pageSize;
                List<SegmentedEntryEntity> entities = database.segmentedEntryDao().getEntriesPaged(pageSize, offset);
                List<SegmentedEntry> entries = convertEntitiesToSegmentedEntries(entities);
                int totalCount = database.segmentedEntryDao().getEntriesCount();
                boolean hasMorePages = (offset + pageSize) < totalCount;

                Log.d(TAG, "Loaded segmented page " + page + " with " + entries.size() + " items. Total: " + totalCount);
                mainHandler.post(() -> callback.onSuccess(entries, hasMorePages, totalCount));
            } catch (Exception e) {
                Log.e(TAG, "Error loading segmented paginated data: " + e.getMessage(), e);
                mainHandler.post(() -> callback.onError("Error loading page: " + e.getMessage()));
            }
        });
    }

    /**
     * Load segmented data by category with pagination
     */
    public void getPaginatedSegmentedDataByCategory(String category, int page, int pageSize, PaginatedSegmentedCallback callback) {
        executor.execute(() -> {
            try {
                int offset = page * pageSize;
                List<SegmentedEntryEntity> entities = database.segmentedEntryDao().getEntriesByCategoryPaged(category, pageSize, offset);
                List<SegmentedEntry> entries = convertEntitiesToSegmentedEntries(entities);
                int totalCount = database.segmentedEntryDao().getEntriesCountByCategory(category);
                boolean hasMorePages = (offset + pageSize) < totalCount;

                Log.d(TAG, "Loaded segmented category '" + category + "' page " + page + " with " + entries.size() + " items");
                mainHandler.post(() -> callback.onSuccess(entries, hasMorePages, totalCount));
            } catch (Exception e) {
                Log.e(TAG, "Error loading segmented category data: " + e.getMessage(), e);
                mainHandler.post(() -> callback.onError("Error loading category page: " + e.getMessage()));
            }
        });
    }

    /**
     * Search segmented data with pagination
     */
    public void searchSegmentedPaginated(String searchQuery, int page, int pageSize, PaginatedSegmentedCallback callback) {
        executor.execute(() -> {
            try {
                int offset = page * pageSize;
                List<SegmentedEntryEntity> entities = database.segmentedEntryDao().searchByTitlePaged(searchQuery, pageSize, offset);
                List<SegmentedEntry> entries = convertEntitiesToSegmentedEntries(entities);
                int totalCount = database.segmentedEntryDao().getSearchResultsCount(searchQuery);
                boolean hasMorePages = (offset + pageSize) < totalCount;

                Log.d(TAG, "Search segmented '" + searchQuery + "' page " + page + " with " + entries.size() + " results");
                mainHandler.post(() -> callback.onSuccess(entries, hasMorePages, totalCount));
            } catch (Exception e) {
                Log.e(TAG, "Error searching segmented data: " + e.getMessage(), e);
                mainHandler.post(() -> callback.onError("Error searching: " + e.getMessage()));
            }
        });
    }

    /**
     * Load full entry data when needed (for details view)
     */
    public void getFullEntryData(int entryId, SegmentedDataCallback callback) {
        executor.execute(() -> {
            try {
                // First get the segmented entry
                SegmentedEntryEntity segmentedEntity = database.segmentedEntryDao().getEntryById(entryId);
                if (segmentedEntity == null) {
                    mainHandler.post(() -> callback.onError("Entry not found"));
                    return;
                }

                // Convert to full entry and load additional data if needed
                SegmentedEntry segmentedEntry = convertEntityToSegmentedEntry(segmentedEntity);
                Entry fullEntry = segmentedEntry.toFullEntry();

                // If this entry has complex data, we might need to load it from the original entries table
                // For now, we'll return the segmented entry as a full entry
                List<SegmentedEntry> result = new ArrayList<>();
                result.add(segmentedEntry);

                mainHandler.post(() -> callback.onSuccess(result));
            } catch (Exception e) {
                Log.e(TAG, "Error loading full entry data: " + e.getMessage(), e);
                mainHandler.post(() -> callback.onError("Error loading entry: " + e.getMessage()));
            }
        });
    }

    /**
     * Initialize segmented data from API with progress tracking
     */
    public void initializeSegmentedData(ProgressCallback progressCallback) {
        Log.d(TAG, "Starting segmented data initialization");

        apiService.getPlaylistIndex().enqueue(new Callback<PlaylistIndex>() {
            @Override
            public void onResponse(Call<PlaylistIndex> call, Response<PlaylistIndex> response) {
                if (response.isSuccessful() && response.body() != null) {
                    PlaylistIndex playlistIndex = response.body();
                    fetchAllPlaylistsSegmented(playlistIndex.getPlaylists(), progressCallback);
                } else {
                    mainHandler.post(() -> progressCallback.onError("Failed to load playlist index"));
                }
            }

            @Override
            public void onFailure(Call<PlaylistIndex> call, Throwable t) {
                mainHandler.post(() -> progressCallback.onError("Network error: " + t.getMessage()));
            }
        });
    }

    /**
     * Fetch all playlists and process them in segments
     */
    private void fetchAllPlaylistsSegmented(List<String> playlistUrls, ProgressCallback progressCallback) {
        List<SegmentedEntry> allSegmentedEntries = new ArrayList<>();
        AtomicInteger completedPlaylists = new AtomicInteger(0);
        int totalPlaylists = playlistUrls.size();

        if (playlistUrls.isEmpty()) {
            progressCallback.onComplete();
            return;
        }

        for (String url : playlistUrls) {
            apiService.getPlaylist(url).enqueue(new Callback<Playlist>() {
                @Override
                public void onResponse(Call<Playlist> call, Response<Playlist> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        Playlist playlist = response.body();
                        processPlaylistSegmented(playlist, allSegmentedEntries);
                    }

                    int completed = completedPlaylists.incrementAndGet();
                    mainHandler.post(() -> progressCallback.onProgress(completed, totalPlaylists, "Processing playlists..."));

                    if (completed == totalPlaylists) {
                        saveSegmentedDataInBatches(allSegmentedEntries, progressCallback);
                    }
                }

                @Override
                public void onFailure(Call<Playlist> call, Throwable t) {
                    Log.e(TAG, "Failed to fetch playlist: " + url, t);

                    int completed = completedPlaylists.incrementAndGet();
                    mainHandler.post(() -> progressCallback.onProgress(completed, totalPlaylists, "Processing playlists..."));

                    if (completed == totalPlaylists) {
                        if (!allSegmentedEntries.isEmpty()) {
                            saveSegmentedDataInBatches(allSegmentedEntries, progressCallback);
                        } else {
                            mainHandler.post(() -> progressCallback.onError("Failed to load any playlists"));
                        }
                    }
                }
            });
        }
    }

    /**
     * Process playlist and convert entries to segmented format
     */
    private void processPlaylistSegmented(Playlist playlist, List<SegmentedEntry> allSegmentedEntries) {
        if (playlist.getCategories() != null) {
            for (Category category : playlist.getCategories()) {
                if (category != null && category.getEntries() != null) {
                    for (Entry entry : category.getEntries()) {
                        if (entry != null && entry.getTitle() != null) {
                            SegmentedEntry segmentedEntry = new SegmentedEntry(entry);
                            allSegmentedEntries.add(segmentedEntry);
                        }
                    }
                }
            }
        }
    }

    /**
     * Save segmented data in batches to avoid memory issues
     */
    private void saveSegmentedDataInBatches(List<SegmentedEntry> allSegmentedEntries, ProgressCallback progressCallback) {
        executor.execute(() -> {
            try {
                Log.d(TAG, "Saving " + allSegmentedEntries.size() + " segmented entries in batches");

                // Clear existing data
                database.segmentedEntryDao().deleteAll();

                // Process in batches
                int totalBatches = (int) Math.ceil((double) allSegmentedEntries.size() / BATCH_SIZE);

                for (int i = 0; i < allSegmentedEntries.size(); i += BATCH_SIZE) {
                    int endIndex = Math.min(i + BATCH_SIZE, allSegmentedEntries.size());
                    List<SegmentedEntry> batch = allSegmentedEntries.subList(i, endIndex);

                    // Convert to entities
                    List<SegmentedEntryEntity> entities = convertSegmentedEntriesToEntities(batch);

                    // Save batch
                    database.segmentedEntryDao().insertAll(entities);

                    int currentBatch = (i / BATCH_SIZE) + 1;
                    mainHandler.post(() -> progressCallback.onProgress(currentBatch, totalBatches, "Saving data batch " + currentBatch + "/" + totalBatches));
                }

                Log.d(TAG, "Successfully saved " + allSegmentedEntries.size() + " segmented entries");
                mainHandler.post(progressCallback::onComplete);

            } catch (Exception e) {
                Log.e(TAG, "Error saving segmented data: " + e.getMessage(), e);
                mainHandler.post(() -> progressCallback.onError("Error saving data: " + e.getMessage()));
            }
        });
    }

    /**
     * Get unique genres from segmented data
     */
    public List<String> getUniqueGenres() {
        try {
            List<String> genres = database.segmentedEntryDao().getUniqueGenres();
            List<String> filteredGenres = new ArrayList<>();
            for (String genre : genres) {
                if (genre != null && !genre.trim().isEmpty() && !genre.equalsIgnoreCase("null")) {
                    filteredGenres.add(genre.trim());
                }
            }
            return filteredGenres;
        } catch (Exception e) {
            Log.e(TAG, "Error getting unique genres: " + e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    /**
     * Get unique countries from segmented data
     */
    public List<String> getUniqueCountries() {
        try {
            List<String> countries = database.segmentedEntryDao().getUniqueCountries();
            List<String> filteredCountries = new ArrayList<>();
            for (String country : countries) {
                if (country != null && !country.trim().isEmpty() && !country.equalsIgnoreCase("null")) {
                    filteredCountries.add(country.trim());
                }
            }
            return filteredCountries;
        } catch (Exception e) {
            Log.e(TAG, "Error getting unique countries: " + e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    /**
     * Get unique years from segmented data
     */
    public List<String> getUniqueYears() {
        try {
            List<String> years = database.segmentedEntryDao().getUniqueYears();
            List<String> filteredYears = new ArrayList<>();
            for (String year : years) {
                if (year != null && !year.trim().isEmpty() && !year.equalsIgnoreCase("null") && !year.equals("0")) {
                    filteredYears.add(year.trim());
                }
            }
            return filteredYears;
        } catch (Exception e) {
            Log.e(TAG, "Error getting unique years: " + e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    /**
     * Get total count of segmented entries
     */
    public int getTotalSegmentedEntriesCount() {
        return database.segmentedEntryDao().getEntriesCount();
    }

    // Conversion methods
    private List<SegmentedEntry> convertEntitiesToSegmentedEntries(List<SegmentedEntryEntity> entities) {
        List<SegmentedEntry> entries = new ArrayList<>();
        for (SegmentedEntryEntity entity : entities) {
            entries.add(convertEntityToSegmentedEntry(entity));
        }
        return entries;
    }

    private SegmentedEntry convertEntityToSegmentedEntry(SegmentedEntryEntity entity) {
        SegmentedEntry entry = new SegmentedEntry();
        entry.setId(entity.getId());
        entry.setTitle(entity.getTitle());
        entry.setSubCategory(entity.getSubCategory());
        entry.setMainCategory(entity.getMainCategory());
        entry.setCountry(entity.getCountry());
        entry.setDescription(entity.getDescription());
        entry.setPoster(entity.getPoster());
        entry.setThumbnail(entity.getThumbnail());
        entry.setRating(entity.getRating());
        entry.setDuration(entity.getDuration());
        entry.setYear(entity.getYear());
        entry.setParentalRating(entity.getParentalRating());
        entry.setHasServers(entity.isHasServers());
        entry.setHasSeasons(entity.isHasSeasons());
        entry.setHasRelated(entity.isHasRelated());
        entry.setEpisodeCount(entity.getEpisodeCount());
        entry.setSeasonCount(entity.getSeasonCount());
        return entry;
    }

    private List<SegmentedEntryEntity> convertSegmentedEntriesToEntities(List<SegmentedEntry> entries) {
        List<SegmentedEntryEntity> entities = new ArrayList<>();
        for (SegmentedEntry entry : entries) {
            entities.add(convertSegmentedEntryToEntity(entry));
        }
        return entities;
    }

    private SegmentedEntryEntity convertSegmentedEntryToEntity(SegmentedEntry entry) {
        SegmentedEntryEntity entity = new SegmentedEntryEntity();
        entity.setId(entry.getId());
        entity.setTitle(entry.getTitle());
        entity.setSubCategory(entry.getSubCategory());
        entity.setMainCategory(entry.getMainCategory());
        entity.setCountry(entry.getCountry());
        entity.setDescription(entry.getDescription());
        entity.setPoster(entry.getPoster());
        entity.setThumbnail(entry.getThumbnail());
        entity.setRating(entry.getRating().toString());
        entity.setDuration(entry.getDuration());
        entity.setYear(entry.getYear().toString());
        entity.setParentalRating(entry.getParentalRating());
        entity.setHasServers(entry.isHasServers());
        entity.setHasSeasons(entry.isHasSeasons());
        entity.setHasRelated(entry.isHasRelated());
        entity.setEpisodeCount(entry.getEpisodeCount());
        entity.setSeasonCount(entry.getSeasonCount());
        entity.setLastUpdated(System.currentTimeMillis());
        return entity;
    }
}