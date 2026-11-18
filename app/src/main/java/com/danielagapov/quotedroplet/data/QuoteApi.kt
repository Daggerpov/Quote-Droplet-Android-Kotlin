package com.danielagapov.quotedroplet.data

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/** The Quote Dropper backend (Daggerpov/Quote-Dropper), shared with the iOS app. */
interface QuoteApi {
    @GET("quotes/random")
    suspend fun randomQuote(
        @Query("classification") classification: String?,
        @Query("maxLength") maxLength: Int?,
    ): Quote

    @GET("quotes")
    suspend fun quotes(
        @Query("author") author: String? = null,
        @Query("search") search: String? = null,
        @Query("category") category: String? = null,
    ): List<Quote>

    @GET("quotes/recent")
    suspend fun recentQuotes(@Query("limit") limit: Int): List<Quote>

    @GET("quotes/top")
    suspend fun topQuotes(@Query("category") category: String): List<Quote>

    @GET("quotes/count")
    suspend fun count(@Query("category") category: String): CountResponse

    @GET("quotes/{id}")
    suspend fun quote(@Path("id") id: Int): Quote

    @POST("quotes/{id}/like")
    suspend fun like(@Path("id") id: Int): Quote

    @DELETE("quotes/{id}/like")
    suspend fun unlike(@Path("id") id: Int): Quote

    @POST("quotes")
    suspend fun addQuote(@Body body: NewQuoteRequest): Response<ResponseBody>

    @GET("quotes/submissions")
    suspend fun submissions(@Header("X-Submitter-ID") submitterId: String): List<Submission>

    // The backend reads feedback as form fields (type, name, content)
    @FormUrlEncoded
    @POST("feedback")
    suspend fun sendFeedback(
        @Field("type") type: String,
        @Field("name") name: String,
        @Field("content") content: String,
    ): Response<ResponseBody>
}
