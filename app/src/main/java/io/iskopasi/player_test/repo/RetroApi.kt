package io.iskopasi.player_test.repo

import com.example.example.LyricsTrackResponse
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Path
import retrofit2.http.Query

interface JsonApi {
    @GET("api/v1/search?reqtype=default&territory=US&searchtype=track&limit=1&output=json")
    suspend fun getTracks(@Query("all") all: String): LyricsTrackResponse?
}

interface HtmlApi {
    @Headers(
        "User-Agent: Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:128.0) Gecko/20100101 Firefox/128.0",
        "Accept: text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/png,image/svg+xml,*/*;q=0.8",
        "Accept-Language: en-US,en;q=0.5",
        "Accept-Encoding: gzip, deflate, br, zstd",
        "Referer: https://lyrics.lyricfind.com/",
        "DNT: 1",
        "Sec-GPC: 1",
        "Connection: keep-alive",
        "Upgrade-Insecure-Requests: 1",
        "Sec-Fetch-Dest: document",
        "Sec-Fetch-Mode: navigate",
        "Sec-Fetch-Site: same-origin",
        "Priority: u=0, i",
        "Pragma: no-cache"
    )
    @GET("lyrics/{slug}")
    suspend fun getLyrics(@Path("slug") slug: String): Document?
}

fun getJSoupDocument(slug: String): Document? = Jsoup
    .connect("https://lyrics.lyricfind.com/lyrics/$slug")
    .header(
        "User-Agent",
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:128.0) Gecko/20100101 Firefox/128.0"
    )
    .header(
        "Accept",
        "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/png,image/svg+xml,*/*;q=0.8"
    )
    .header("Accept-Language", "en-US,en;q=0.5")
    .header("Accept-Encoding", "gzip, deflate, br, zstd")
    .header("Referer", "https://lyrics.lyricfind.com/")
    .header("DNT", "1")
    .header("Sec-GPC", "1")
    .header("Connection", "keep-alive")
    .header("Upgrade-Insecure-Requests", "1")
    .header("Sec-Fetch-Dest", "document")
    .header("Sec-Fetch-Mode", "navigate")
    .header("Sec-Fetch-Site", "same-origin")
    .header("Priority", "u=0, i")
    .header("Pragma", "no-cache")
    .get()