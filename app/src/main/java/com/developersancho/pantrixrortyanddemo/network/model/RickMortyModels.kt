package com.developersancho.pantrixrortyanddemo.network.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/** Rick & Morty paging envelope: `info` + the page's `results`. */
@JsonClass(generateAdapter = true)
data class RMPage<T>(
    @Json(name = "info") val info: RMInfo,
    @Json(name = "results") val results: List<T> = emptyList()
)

@JsonClass(generateAdapter = true)
data class RMInfo(
    @Json(name = "count") val count: Int = 0,
    @Json(name = "pages") val pages: Int = 0,
    /** Absolute URL of the next page, or null on the last one — the only paging cursor the API gives. */
    @Json(name = "next") val next: String? = null,
    @Json(name = "prev") val prev: String? = null
)

@JsonClass(generateAdapter = true)
data class RMCharacter(
    @Json(name = "id") val id: Int,
    @Json(name = "name") val name: String,
    @Json(name = "status") val status: String = "",
    @Json(name = "species") val species: String = "",
    @Json(name = "type") val type: String = "",
    @Json(name = "gender") val gender: String = "",
    @Json(name = "origin") val origin: RMRef? = null,
    @Json(name = "location") val location: RMRef? = null,
    @Json(name = "image") val image: String = "",
    @Json(name = "episode") val episode: List<String> = emptyList()
) {
    /** "Alive · Human · Male" — the subtitle the list row shows. */
    val summary: String get() = listOf(status, species, gender).filter { it.isNotBlank() }.joinToString(" · ")
}

/** A named cross-reference (origin / location); `url` is empty when the API has no record. */
@JsonClass(generateAdapter = true)
data class RMRef(
    @Json(name = "name") val name: String = "",
    @Json(name = "url") val url: String = ""
)

@JsonClass(generateAdapter = true)
data class RMEpisode(
    @Json(name = "id") val id: Int,
    @Json(name = "name") val name: String,
    /** "December 2, 2013" — the API's own formatting, shown verbatim. */
    @Json(name = "air_date") val airDate: String = "",
    /** "S01E01" */
    @Json(name = "episode") val code: String = "",
    @Json(name = "characters") val characters: List<String> = emptyList()
) {
    val summary: String get() = listOf(code, airDate).filter { it.isNotBlank() }.joinToString(" · ")
}

@JsonClass(generateAdapter = true)
data class RMLocation(
    @Json(name = "id") val id: Int,
    @Json(name = "name") val name: String,
    @Json(name = "type") val type: String = "",
    @Json(name = "dimension") val dimension: String = "",
    @Json(name = "residents") val residents: List<String> = emptyList()
) {
    val summary: String get() = listOf(type, dimension).filter { it.isNotBlank() }.joinToString(" · ")
}
