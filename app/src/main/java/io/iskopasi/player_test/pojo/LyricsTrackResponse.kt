package com.example.example

import com.google.gson.annotations.SerializedName


data class LyricsTrackResponse(

    @SerializedName("response") var response: Response? = Response(),
    @SerializedName("totalresults") var totalresults: Int? = null,
    @SerializedName("totalpages") var totalpages: Int? = null,
    @SerializedName("tracks") var tracks: ArrayList<Tracks> = arrayListOf()

)