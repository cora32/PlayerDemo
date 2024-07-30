package com.example.example

import com.google.gson.annotations.SerializedName


data class Artists(

    @SerializedName("name") var name: String? = null,
    @SerializedName("lfid") var lfid: String? = null,
    @SerializedName("slug") var slug: String? = null,
    @SerializedName("is_primary") var isPrimary: Boolean? = null

)