package com.example.example

import com.google.gson.annotations.SerializedName


data class Response(

    @SerializedName("code") var code: Int? = null,
    @SerializedName("description") var description: String? = null

)