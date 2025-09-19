package com.cinecraze.free.repository;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.cinecraze.free.database.CineCrazeDatabase;
import com.cinecraze.free.database.DatabaseUtils;
import com.cinecraze.free.database.entities.CacheMetadataEntity;
import com.cinecraze.free.database.entities.EntryEntity;
import com.cinecraze.free.models.Category;
import com.cinecraze.free.models.Entry;
import com.cinecraze.free.models.Playlist;
import com.cinecraze.free.models.PlaylistIndex;
import android.content.SharedPreferences;
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

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DataRepository {

    private static final String TAG = "DataRepository";
    private static final String CACHE_KEY_PLAYLIST = "playlist_data";
    private static final long CACHE_EXPIRY_HOURS = 24; // Cache expires after 24 hours
    public static final int DEFAULT_PAGE_SIZE = 20; // Default items per page

    private CineCrazeDatabase database;
    private ApiService apiService;
    private Gson gson = new Gson();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private SharedPreferences parentalPrefs;
    private List<String> allowedRatings;
    private boolean includeUnrated;

    public interface DataCallback {
        void onSuccess(List<Entry> entries);
        void onError(String error);
    }

    public interface PaginatedDataCallback {
        void onSuccess(List<Entry> entries, boolean hasMorePages, int totalCount);
        void onError(String error);
    }

    public interface UpdateCallback {
        void onUpdateAvailable();
        void onNoUpdate();
        void onError(String error);
    }

    public interface CacheValidationCallback {
        void onResult(boolean isValid);
    }

    public DataRepository(Context context) {
        database = CineCrazeDatabase.getInstance(context);
        apiService = RetrofitClient.getClient().create(ApiService.class);
        parentalPrefs = context.getSharedPreferences("ParentalControls", Context.MODE_PRIVATE);
        loadParentalControlSettings();
    }

    private void loadParentalControlSettings() {
        Set<String> selectedRatings = parentalPrefs.getStringSet("selected_ratings", new HashSet<>(Arrays.asList("G", "PG", "PG-13", "R", "NC-17")));
        includeUnrated = parentalPrefs.getBoolean("unrated", true);
        allowedRatings = new ArrayList<>(selectedRatings);
    }

    public void hasValidCache(CacheValidationCallback callback) {
        executor.execute(() -> {
            try {
                CacheMetadataEntity metadata = database.cacheMetadataDao().getMetadata(CACHE_KEY_PLAYLIST);
                boolean isValid = metadata != null && isCacheValid(metadata.getLastUpdated());
                mainHandler.post(() -> callback.onResult(isValid));
            } catch (Exception e) {
                Log.e(TAG, "Error checking cache validity: " + e.getMessage(), e);
                mainHandler.post(() -> callback.onResult(false));
            }
        });
    }

    public void getPlaylistData(DataCallback callback) {
        executor.execute(() -> {
            CacheMetadataEntity metadata = database.cacheMetadataDao().getMetadata(CACHE_KEY_PLAYLIST);

            if (metadata != null && isCacheValid(metadata.getLastUpdated())) {
                Log.d(TAG, "Using cached data");
                loadFromCache(callback);
            } else {
                Log.d(TAG, "Cache expired or empty, fetching from API");
                mainHandler.post(() -> fetchFromApi(callback));
            }
        });
    }

    public void forceRefreshData(DataCallback callback) {
        Log.d(TAG, "Force refreshing data from API");
        fetchFromApi(callback);
    }

    public void ensureDataAvailable(DataCallback callback) {
        hasValidCache(isValid -> {
            if (isValid) {
                Log.d(TAG, "Cache is available and valid - ready for pagination");
                callback.onSuccess(new ArrayList<>());
                checkForUpdates(new UpdateCallback() {
                    @Override
                    public void onUpdateAvailable() {
                        Log.d(TAG, "An update is available, user will be notified.");
                        // Notification should be handled by the UI layer
                    }

                    @Override
                    public void onNoUpdate() {
                        Log.d(TAG, "No new updates.");
                    }

                    @Override
                    public void onError(String error) {
                        Log.e(TAG, "Update check failed: " + error);
                    }
                });
            } else {
                Log.d(TAG, "No valid cache - fetching data to populate cache");
                fetchFromApi(new DataCallback() {
                    @Override
                    public void onSuccess(List<Entry> entries) {
                        Log.d(TAG, "Data cached successfully - ready for pagination");
                        callback.onSuccess(new ArrayList<>());
                    }

                    @Override
                    public void onError(String error) {
                        callback.onError(error);
                    }
                });
            }
        });
    }

    public void getPaginatedData(int page, int pageSize, PaginatedDataCallback callback) {
        loadParentalControlSettings(); // Refresh settings
        executor.execute(() -> {
            try {
                int offset = page * pageSize;
                List<EntryEntity> entities = database.entryDao().getEntriesPagedWithRating(allowedRatings, includeUnrated, pageSize, offset);
                List<Entry> entries = DatabaseUtils.entitiesToEntries(entities);
                int totalCount = database.entryDao().getEntriesCountWithRating(allowedRatings, includeUnrated);
                boolean hasMorePages = (offset + pageSize) < totalCount;

                Log.d(TAG, "Loaded page " + page + " with " + entries.size() + " items. Total: " + totalCount + ", HasMore: " + hasMorePages);
                mainHandler.post(() -> callback.onSuccess(entries, hasMorePages, totalCount));
            } catch (Exception e) {
                Log.e(TAG, "Error loading paginated data: " + e.getMessage(), e);
                mainHandler.post(() -> callback.onError("Error loading page: " + e.getMessage()));
            }
        });
    }

    public void getPaginatedDataByCategory(String category, int page, int pageSize, PaginatedDataCallback callback) {
        loadParentalControlSettings(); // Refresh settings
        executor.execute(() -> {
            try {
                int offset = page * pageSize;
                List<EntryEntity> entities = database.entryDao().getEntriesByCategoryPagedWithRating(category, allowedRatings, includeUnrated, pageSize, offset);
                List<Entry> entries = DatabaseUtils.entitiesToEntries(entities);
                int totalCount = database.entryDao().getEntriesCountByCategoryWithRating(category, allowedRatings, includeUnrated);
                boolean hasMorePages = (offset + pageSize) < totalCount;

                Log.d(TAG, "Loaded category '" + category + "' page " + page + " with " + entries.size() + " items. Total: " + totalCount);
                mainHandler.post(() -> callback.onSuccess(entries, hasMorePages, totalCount));
            } catch (Exception e) {
                Log.e(TAG, "Error loading paginated category data: " + e.getMessage(), e);
                mainHandler.post(() -> callback.onError("Error loading category page: " + e.getMessage()));
            }
        });
    }

    public void searchPaginated(String searchQuery, int page, int pageSize, PaginatedDataCallback callback) {
        loadParentalControlSettings(); // Refresh settings
        executor.execute(() -> {
            try {
                int offset = page * pageSize;
                List<EntryEntity> entities = database.entryDao().searchByTitlePagedWithRating(searchQuery, allowedRatings, includeUnrated, pageSize, offset);
                List<Entry> entries = DatabaseUtils.entitiesToEntries(entities);
                int totalCount = database.entryDao().getSearchResultsCountWithRating(searchQuery, allowedRatings, includeUnrated);
                boolean hasMorePages = (offset + pageSize) < totalCount;

                Log.d(TAG, "Search '" + searchQuery + "' page " + page + " with " + entries.size() + " results. Total: " + totalCount);
                mainHandler.post(() -> callback.onSuccess(entries, hasMorePages, totalCount));
            } catch (Exception e) {
                Log.e(TAG, "Error searching with pagination: " + e.getMessage(), e);
                mainHandler.post(() -> callback.onError("Error searching: " + e.getMessage()));
            }
        });
    }

    public void refreshData(DataCallback callback) {
        Log.d(TAG, "Force refreshing data from API");
        fetchFromApi(callback);
    }

    public List<Entry> getEntriesByCategory(String category) {
        List<EntryEntity> entities = database.entryDao().getEntriesByCategory(category);
        return DatabaseUtils.entitiesToEntries(entities);
    }

    public List<Entry> searchByTitle(String title) {
        List<EntryEntity> entities = database.entryDao().searchByTitle(title);
        return DatabaseUtils.entitiesToEntries(entities);
    }

    public List<Entry> getAllCachedEntries() {
        List<EntryEntity> entities = database.entryDao().getAllEntries();
        return DatabaseUtils.entitiesToEntries(entities);
    }

    public int getTotalEntriesCount() {
        return database.entryDao().getEntriesCount();
    }

    public int getLocalDataVersion() {
        try {
            CacheMetadataEntity metadata = database.cacheMetadataDao().getMetadata(CACHE_KEY_PLAYLIST);
            if (metadata != null && metadata.getDataVersion() != null) {
                return Integer.parseInt(metadata.getDataVersion());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting local data version: " + e.getMessage(), e);
        }
        return 0;
    }

    public List<String> getUniqueGenres() {
        try {
            List<String> genres = database.entryDao().getUniqueGenres();
            // Filter out null and empty values
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

    public List<String> getUniqueCountries() {
        try {
            List<String> countries = database.entryDao().getUniqueCountries();
            // Filter out null and empty values
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

    public List<String> getUniqueYears() {
        try {
            List<String> years = database.entryDao().getUniqueYears();
            // Filter out null, empty, and zero values
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

    public void getPaginatedFilteredData(String genre, String country, String year, int page, int pageSize, PaginatedDataCallback callback) {
        loadParentalControlSettings(); // Refresh settings
        executor.execute(() -> {
            try {
                int offset = page * pageSize;
                List<EntryEntity> entities = database.entryDao().getEntriesFilteredPagedWithRating(
                    genre == null || genre.isEmpty() ? null : genre,
                    country == null || country.isEmpty() ? null : country,
                    year == null || year.isEmpty() ? null : year,
                    allowedRatings, includeUnrated, pageSize, offset
                );
                List<Entry> entries = DatabaseUtils.entitiesToEntries(entities);
                int totalCount = database.entryDao().getEntriesFilteredCountWithRating(
                    genre == null || genre.isEmpty() ? null : genre,
                    country == null || country.isEmpty() ? null : country,
                    year == null || year.isEmpty() ? null : year,
                    allowedRatings, includeUnrated
                );
                boolean hasMorePages = (offset + pageSize) < totalCount;

                Log.d(TAG, "Loaded filtered page " + page + " with " + entries.size() + " items. Total: " + totalCount);
                mainHandler.post(() -> callback.onSuccess(entries, hasMorePages, totalCount));
            } catch (Exception e) {
                Log.e(TAG, "Error loading filtered paginated data: " + e.getMessage(), e);
                mainHandler.post(() -> callback.onError("Error loading filtered page: " + e.getMessage()));
            }
        });
    }

    public List<Entry> getTopRatedEntries(int count) {
        try {
            List<EntryEntity> entities = database.entryDao().getTopRatedEntries(count);
            return DatabaseUtils.entitiesToEntries(entities);
        } catch (Exception e) {
            Log.e(TAG, "Error getting top rated entries: " + e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    private boolean isCacheValid(long lastUpdated) {
        long currentTime = System.currentTimeMillis();
        long cacheAge = currentTime - lastUpdated;
        long expiryTime = TimeUnit.HOURS.toMillis(CACHE_EXPIRY_HOURS);
        return cacheAge < expiryTime;
    }

    private void loadFromCache(DataCallback callback) {
        executor.execute(() -> {
            try {
                List<EntryEntity> entities = database.entryDao().getAllEntries();
                List<Entry> entries = DatabaseUtils.entitiesToEntries(entities);

                if (!entries.isEmpty()) {
                    mainHandler.post(() -> callback.onSuccess(entries));
                } else {
                    Log.d(TAG, "Cache is empty, fetching from API");
                    mainHandler.post(() -> fetchFromApi(callback));
                }
            } catch (Exception e) {
                Log.e(TAG, "Error loading from cache: " + e.getMessage(), e);
                mainHandler.post(() -> fetchFromApi(callback));
            }
        });
    }

    private static class EntryWithCategory {
        final Entry entry;
        final String mainCategory;

        EntryWithCategory(Entry entry, String mainCategory) {
            this.entry = entry;
            this.mainCategory = mainCategory;
        }
    }

    public void checkForUpdates(UpdateCallback callback) {
        apiService.getPlaylistIndex().enqueue(new Callback<PlaylistIndex>() {
            @Override
            public void onResponse(Call<PlaylistIndex> call, Response<PlaylistIndex> response) {
                if (response.isSuccessful() && response.body() != null) {
                    PlaylistIndex playlistIndex = response.body();
                    executor.execute(() -> {
                        CacheMetadataEntity metadata = database.cacheMetadataDao().getMetadata(CACHE_KEY_PLAYLIST);
                        int remoteVersion = playlistIndex.getVersion();
                        int localVersion = 0;
                        if (metadata != null && metadata.getDataVersion() != null) {
                            try {
                                localVersion = Integer.parseInt(metadata.getDataVersion());
                            } catch (NumberFormatException e) {
                                Log.e(TAG, "Invalid local version format", e);
                            }
                        }

                        if (remoteVersion > localVersion) {
                            mainHandler.post(callback::onUpdateAvailable);
                        } else {
                            mainHandler.post(callback::onNoUpdate);
                        }
                    });
                } else {
                    mainHandler.post(() -> callback.onError("Failed to check for updates."));
                }
            }

            @Override
            public void onFailure(Call<PlaylistIndex> call, Throwable t) {
                mainHandler.post(() -> callback.onError("Network error: " + t.getMessage()));
            }
        });
    }

    private void fetchFromApi(DataCallback callback) {
        apiService.getPlaylistIndex().enqueue(new Callback<PlaylistIndex>() {
            @Override
            public void onResponse(Call<PlaylistIndex> call, Response<PlaylistIndex> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getPlaylists() != null) {
                    PlaylistIndex playlistIndex = response.body();
                    executor.execute(() -> {
                        CacheMetadataEntity metadata = database.cacheMetadataDao().getMetadata(CACHE_KEY_PLAYLIST);

                        int remoteVersion = playlistIndex.getVersion();
                        int localVersion = 0;
                        List<String> localPlaylistUrls = new ArrayList<>();

                        if (metadata != null && metadata.getDataVersion() != null) {
                            try {
                                localVersion = Integer.parseInt(metadata.getDataVersion());
                            } catch (NumberFormatException e) {
                                Log.e(TAG, "Invalid local version format", e);
                            }
                        }

                        if (metadata != null && metadata.getPlaylistUrls() != null) {
                            Type listType = new TypeToken<List<String>>() {}.getType();
                            localPlaylistUrls = gson.fromJson(metadata.getPlaylistUrls(), listType);
                        }

                        List<String> remotePlaylistUrls = playlistIndex.getPlaylists();

                        if (remoteVersion > localVersion) {
                            Log.d(TAG, "New version found. Remote: " + remoteVersion + ", Local: " + localVersion + ". Fetching all playlists.");
                            mainHandler.post(() -> fetchAllPlaylists(remotePlaylistUrls, remoteVersion, remotePlaylistUrls, callback));
                        } else {
                            Log.d(TAG, "Data is up to date.");
                            loadFromCache(callback);
                        }
                    });
                } else {
                    mainHandler.post(() -> callback.onError("Failed to load playlist index."));
                }
            }

            @Override
            public void onFailure(Call<PlaylistIndex> call, Throwable t) {
                mainHandler.post(() -> callback.onError("Network error: " + t.getMessage()));
            }
        });
    }

    private void fetchAllPlaylists(List<String> playlistUrls, int version, List<String> allRemoteUrls, DataCallback finalCallback) {
        List<EntryWithCategory> aggregatedEntries = new ArrayList<>();
        java.util.Set<String> uniqueEntryTitles = new java.util.HashSet<>();
        java.util.concurrent.atomic.AtomicInteger counter = new java.util.concurrent.atomic.AtomicInteger(playlistUrls.size());

        if (playlistUrls.isEmpty()) {
            cacheData(new ArrayList<>(), version, allRemoteUrls, () -> finalCallback.onSuccess(new ArrayList<>()));
            return;
        }

        for (String url : playlistUrls) {
            apiService.getPlaylist(url).enqueue(new Callback<Playlist>() {
                @Override
                public void onResponse(Call<Playlist> call, Response<Playlist> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        Playlist playlist = response.body();
                        if (playlist.getCategories() != null) {
                            for (Category category : playlist.getCategories()) {
                                if (category != null && category.getEntries() != null) {
                                    for (Entry entry : category.getEntries()) {
                                        if (entry != null && entry.getTitle() != null && !uniqueEntryTitles.contains(entry.getTitle())) {
                                            synchronized (aggregatedEntries) {
                                                aggregatedEntries.add(new EntryWithCategory(entry, category.getMainCategory()));
                                                uniqueEntryTitles.add(entry.getTitle());
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    if (counter.decrementAndGet() == 0) {
                        cacheData(aggregatedEntries, version, allRemoteUrls, () -> {
                            List<Entry> allEntries = new ArrayList<>();
                            for (EntryWithCategory ewc : aggregatedEntries) {
                                allEntries.add(ewc.entry);
                            }
                            finalCallback.onSuccess(allEntries);
                        });
                    }
                }

                @Override
                public void onFailure(Call<Playlist> call, Throwable t) {
                    Log.e(TAG, "API call failed for playlist " + url + ": " + t.getMessage(), t);
                    if (counter.decrementAndGet() == 0) {
                        if (!aggregatedEntries.isEmpty()) {
                            cacheData(aggregatedEntries, version, allRemoteUrls, () -> {
                                List<Entry> allEntries = new ArrayList<>();
                                for (EntryWithCategory ewc : aggregatedEntries) {
                                    allEntries.add(ewc.entry);
                                }
                                finalCallback.onSuccess(allEntries);
                            });
                        } else {
                            finalCallback.onError("Failed to load any new playlists.");
                        }
                    }
                }
            });
        }
    }

    private void cacheData(List<EntryWithCategory> entriesWithCategories, int version, List<String> allPlaylistUrls, Runnable onComplete) {
        executor.execute(() -> {
            try {
                // database.entryDao().deleteAll(); // This is removed because OnConflictStrategy.REPLACE handles it.
                List<EntryEntity> entitiesToInsert = new ArrayList<>();
                for (EntryWithCategory ewc : entriesWithCategories) {
                    entitiesToInsert.add(DatabaseUtils.entryToEntity(ewc.entry, ewc.mainCategory));
                }
                if (!entitiesToInsert.isEmpty()) {
                    database.entryDao().insertAll(entitiesToInsert);
                }

                CacheMetadataEntity metadata = database.cacheMetadataDao().getMetadata(CACHE_KEY_PLAYLIST);
                if (metadata == null) {
                    metadata = new CacheMetadataEntity();
                    metadata.setKey(CACHE_KEY_PLAYLIST);
                }

                metadata.setLastUpdated(System.currentTimeMillis());
                metadata.setDataVersion(String.valueOf(version));
                metadata.setPlaylistUrls(gson.toJson(allPlaylistUrls));

                database.cacheMetadataDao().insert(metadata);
                Log.d(TAG, "Data cached successfully: " + entitiesToInsert.size() + " entries. Version: " + version);
            } catch (Exception e) {
                Log.e(TAG, "Error caching data: " + e.getMessage(), e);
            } finally {
                mainHandler.post(onComplete);
            }
        });
    }
}