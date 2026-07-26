package com.developersancho.pantrixrortyanddemo.network

import com.developersancho.pantrixrortyanddemo.network.model.RMCharacter
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
}
