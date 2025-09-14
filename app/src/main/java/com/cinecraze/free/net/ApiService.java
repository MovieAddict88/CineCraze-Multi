package com.cinecraze.free.net;

import com.cinecraze.free.models.Playlist;
import com.cinecraze.free.models.PlaylistIndex;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.Url;

public interface ApiService {
    @Headers({
        "User-Agent: Mozilla/5.0 (Android) CineCraze/1.0",
        "Accept: application/json"
    })
    @GET("playlists.json")
    Call<PlaylistIndex> getPlaylistIndex();

    @Headers({
        "User-Agent: Mozilla/5.0 (Android) CineCraze/1.0",
        "Accept: application/json"
    })
    @GET
    Call<Playlist> getPlaylist(@Url String url);
}