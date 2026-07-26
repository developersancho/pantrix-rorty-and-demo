package com.developersancho.pantrixrortyanddemo.feature.characters

import com.developersancho.pantrixrortyanddemo.network.RickMortyApi
import com.developersancho.pantrixrortyanddemo.network.model.RMCharacter
import javax.inject.Inject
import javax.inject.Singleton

/** One page of characters plus whether the API has more. */
data class CharactersPage(val characters: List<RMCharacter>, val hasMore: Boolean)

@Singleton
class CharactersRepository @Inject constructor(private val api: RickMortyApi) {

    /**
     * The API answers **404** for a search with no matches rather than an empty page, so a miss is
     * modelled as an empty result instead of an error — otherwise typing a nonsense name looks like
     * the network broke.
     */
    suspend fun characters(page: Int, query: String?): CharactersPage = try {
        val response = api.characters(page = page, name = query?.takeIf { it.isNotBlank() })
        CharactersPage(response.results, response.info.next != null)
    } catch (e: retrofit2.HttpException) {
        if (e.code() == 404) CharactersPage(emptyList(), hasMore = false) else throw e
    }

    suspend fun character(id: Int): RMCharacter = api.character(id)
}
