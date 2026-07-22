package com.roninx.shared.models

import kotlinx.serialization.Serializable

@Serializable
data class Anime(
    val title: String,
    val originalTitle: String? = null,
    val synonyms: List<String> = emptyList(),
    val image: String,
    val banner: String? = null,
    val epCount: Int? = null,
    val score: String = "N/A",
    val synopsis: String = "",
    val isManga: Boolean = false
)
