package com.developersancho.pantrixrortyanddemo.feature.shared

import com.developersancho.pantrixrortyanddemo.network.RickMortyApi
import com.developersancho.pantrixrortyanddemo.network.model.RMCharacter
import com.developersancho.pantrixrortyanddemo.network.model.RMEpisode
import com.developersancho.pantrixrortyanddemo.network.model.RMLocation
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RickMortyRepository @Inject constructor(private val api: RickMortyApi) {

    suspend fun characters(page: Int, query: String?): Page<RMCharacter> =
        paged { api.characters(page, query).let { it.results to it.info.next } }

    suspend fun episodes(page: Int, query: String?): Page<RMEpisode> =
        paged { api.episodes(page, query).let { it.results to it.info.next } }

    suspend fun locations(page: Int, query: String?): Page<RMLocation> =
        paged { api.locations(page, query).let { it.results to it.info.next } }

    suspend fun character(id: Int): RMCharacter = api.character(id)
    suspend fun episode(id: Int): RMEpisode = api.episode(id)
    suspend fun location(id: Int): RMLocation = api.location(id)

    /**
     * Resolve `/character/1` style URLs to the characters themselves.
     *
     * The API answers a **single object** for one id and an **array** for several, so a one-element
     * list would fail to deserialize as `List<RMCharacter>`. Handled by routing a single id to the
     * single-item endpoint instead of special-casing the JSON.
     */
    suspend fun charactersByUrls(urls: List<String>, limit: Int = 20): List<RMCharacter> {
        val ids = urls.mapNotNull { it.substringAfterLast('/').toIntOrNull() }.take(limit)
        return when {
            ids.isEmpty() -> emptyList()
            ids.size == 1 -> listOf(api.character(ids.first()))
            else -> api.charactersByIds(ids.joinToString(","))
        }
    }

    /**
     * A search with no matches answers **404**, not an empty page — modelled as an empty result so
     * typing a nonsense name does not look like the network broke.
     */
    private inline fun <T> paged(block: () -> kotlin.Pair<List<T>, String?>): Page<T> = try {
        val (items, next) = block()
        Page(items, next != null)
    } catch (e: retrofit2.HttpException) {
        if (e.code() == 404) Page(emptyList(), hasMore = false) else throw e
    }
}
