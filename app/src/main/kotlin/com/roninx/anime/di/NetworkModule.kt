package com.roninx.anime.di

import com.roninx.anime.data.api.AniListApi
import com.roninx.anime.data.api.GitHubApi
import com.roninx.anime.data.api.JikanApi
import com.roninx.anime.data.api.KitsuApi
import com.roninx.anime.data.api.ShikimoriApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            })
            .build()
    }

    @Provides
    @Singleton
    fun provideJikanApi(okHttpClient: OkHttpClient): JikanApi {
        return Retrofit.Builder()
            .baseUrl("https://api.jikan.moe/v4/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(JikanApi::class.java)
    }

    @Provides
    @Singleton
    fun provideAniListApi(okHttpClient: OkHttpClient): AniListApi {
        return Retrofit.Builder()
            .baseUrl("https://graphql.anilist.co/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(AniListApi::class.java)
    }

    @Provides
    @Singleton
    fun provideGitHubApi(okHttpClient: OkHttpClient): GitHubApi {
        return Retrofit.Builder()
            .baseUrl("https://api.github.com/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(GitHubApi::class.java)
    }

    @Provides
    @Singleton
    fun provideKitsuApi(okHttpClient: OkHttpClient): KitsuApi {
        return Retrofit.Builder()
            .baseUrl("https://kitsu.app/api/edge/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(KitsuApi::class.java)
    }

    @Provides
    @Singleton
    fun provideShikimoriApi(okHttpClient: OkHttpClient): ShikimoriApi {
        val clientWithUserAgent = okHttpClient.newBuilder()
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .header("User-Agent", "RoninX-Anime-Client")
                    .build()
                chain.proceed(request)
            }
            .build()

        return Retrofit.Builder()
            .baseUrl("https://shikimori.one/api/")
            .client(clientWithUserAgent)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ShikimoriApi::class.java)
    }
}
