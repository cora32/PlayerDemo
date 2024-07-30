package com.example.example

import com.google.gson.annotations.SerializedName


data class Artist(

    @SerializedName("name") var name: String? = null

)