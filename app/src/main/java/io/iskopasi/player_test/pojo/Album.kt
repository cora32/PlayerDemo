package com.example.example

import com.google.gson.annotations.SerializedName


data class Album(

    @SerializedName("id") var id: String? = null,
    @SerializedName("title") var title: String? = null,
    @SerializedName("releaseYear") var releaseYear: Int? = null,
    @SerializedName("slug") var slug: String? = null

)