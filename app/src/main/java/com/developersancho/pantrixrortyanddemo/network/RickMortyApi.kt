package com.developersancho.pantrixrortyanddemo.network

import com.developersancho.pantrixrortyanddemo.network.model.RMCharacter
import com.developersancho.pantrixrortyanddemo.network.model.RMEpisode
import com.developersancho.pantrixrortyanddemo.network.model.RMLocation
import com.developersancho.pantrixrortyanddemo.network.model.RMPage
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface RickMortyApi {

    @GET("character")
    suspend fun characters(
        @Query("page") page: Int,
        @Query("name") name: String? = null
    ): RMPage<RMCharacter>

    @GET("character/{id}")
    suspend fun character(@Path("id") id: Int): RMCharacter

    /**
     * Batch lookup: `/character/1,2,3`. Retrofit would percent-encode the commas with a plain
     * `@Path`, so `encoded = true` keeps the list separator the API expects.
     */
    @GET("character/{ids}")
    suspend fun charactersByIds(@Path("ids", encoded = true) ids: String): List<RMCharacter>

    @GET("episode")
    suspend fun episodes(
        @Query("page") page: Int,
        @Query("name") name: String? = null
    ): RMPage<RMEpisode>

    @GET("episode/{id}")
    suspend fun episode(@Path("id") id: Int): RMEpisode

    /** Batch lookup — see [charactersByIds] for why the path is pre-encoded. */
    @GET("episode/{ids}")
    suspend fun episodesByIds(@Path("ids", encoded = true) ids: String): List<RMEpisode>

    @GET("location")
    suspend fun locations(
        @Query("page") page: Int,
        @Query("name") name: String? = null
    ): RMPage<RMLocation>

    @GET("location/{id}")
    suspend fun location(@Path("id") id: Int): RMLocation
}
