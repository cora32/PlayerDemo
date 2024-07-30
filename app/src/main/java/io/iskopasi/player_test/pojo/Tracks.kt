package com.example.example

import com.google.gson.annotations.SerializedName


data class Tracks(

    @SerializedName("lfid") var lfid: String? = null,
    @SerializedName("language") var language: String? = null,
    @SerializedName("available_translations") var availableTranslations: ArrayList<String> = arrayListOf(),
    @SerializedName("rovi") var rovi: String? = null,
    @SerializedName("gracenote") var gracenote: String? = null,
    @SerializedName("apple") var apple: Int? = null,
    @SerializedName("isrcs") var isrcs: ArrayList<String> = arrayListOf(),
    @SerializedName("instrumental") var instrumental: Boolean? = null,
    @SerializedName("viewable") var viewable: Boolean? = null,
    @SerializedName("has_lrc") var hasLrc: Boolean? = null,
    @SerializedName("has_contentfilter") var hasContentfilter: Boolean? = null,
    @SerializedName("has_emotion") var hasEmotion: Boolean? = null,
    @SerializedName("has_sentiment") var hasSentiment: Boolean? = null,
    @SerializedName("lrc_verified") var lrcVerified: Boolean? = null,
    @SerializedName("title") var title: String? = null,
    @SerializedName("titleSimple") var titleSimple: String? = null,
    @SerializedName("duration") var duration: String? = null,
    @SerializedName("artists") var artists: ArrayList<Artists> = arrayListOf(),
    @SerializedName("artist") var artist: Artist? = Artist(),
    @SerializedName("last_update") var lastUpdate: String? = null,
    @SerializedName("snippet") var snippet: String? = null,
    @SerializedName("context") var context: String? = null,
    @SerializedName("score") var score: Double? = null,
    @SerializedName("glp") var glp: String? = null,
    @SerializedName("slug") var slug: String? = null,
    @SerializedName("album") var album: Album? = Album()

)