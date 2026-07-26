package com.developersancho.pantrixrortyanddemo.di

import com.developersancho.pantrixrortyanddemo.network.RickMortyApi
import com.pantrix.api.Pantrix
import com.pantrix.okhttp.api.PantrixEventListenerFactory
import com.pantrix.okhttp.api.PantrixOkHttpApplicationInterceptor
import com.squareup.moshi.Moshi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun moshi(): Moshi = Moshi.Builder().build()

    /**
     * The one place Pantrix touches the app's networking. Both halves are needed and they do
     * different jobs: the interceptor sees the request/response pair (method, url, status, bodies),
     * the event listener supplies the timings the interceptor cannot observe.
     *
     * `getOkHttpEventCollector()` returns null when HTTP tracking isn't available, so this degrades
     * to a plain client instead of failing.
     */
    @Provides
    @Singleton
    fun okHttpClient(): OkHttpClient = OkHttpClient.Builder()
        .apply {
            Pantrix.getOkHttpEventCollector()?.let { collector ->
                addInterceptor(PantrixOkHttpApplicationInterceptor(collector))
                eventListenerFactory(PantrixEventListenerFactory(collector = collector, delegate = null))
            }
        }
        .build()

    @Provides
    @Singleton
    fun retrofit(client: OkHttpClient, moshi: Moshi): Retrofit = Retrofit.Builder()
        .baseUrl("https://rickandmortyapi.com/api/")
        .client(client)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    @Provides
    @Singleton
    fun rickMortyApi(retrofit: Retrofit): RickMortyApi = retrofit.create(RickMortyApi::class.java)
}
