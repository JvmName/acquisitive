package dev.jvmname.acquisitive.network

import dev.jvmname.acquisitive.network.model.HnItem
import dev.jvmname.acquisitive.network.model.ItemId
import dev.jvmname.acquisitive.network.model.User
import dev.jvmname.acquisitive.network.model.UserId
import dev.jvmname.acquisitive.util.ItemIdArray
import retrofit2.http.GET
import retrofit2.http.Path

interface HnStoryApi {
    @GET("topstories.json")
    suspend fun getTopStories(): ItemIdArray?

    @GET("newstories.json")
    suspend fun getNewStories(): ItemIdArray?

    @GET("showstories.json")
    suspend fun getShowStories(): ItemIdArray?

    @GET("askstories.json")
    suspend fun getAskStories(): ItemIdArray?

    @GET("jobstories.json")
    suspend fun getJobStories(): ItemIdArray?

    @GET("beststories.json")
    suspend fun getBestStories(): ItemIdArray?

    @GET("item/{itemId}.json")
    suspend fun getItem(@Path("itemId") id: ItemId): HnItem

    @GET("user/{userId}.json")
    suspend fun getUser(@Path("userId") id: UserId): User?


    //TODO login, favorite, vote, etc. (see: UserServicesClient)
}

interface HnUserApi{
//    @POST("login")
//    suspend fun login(
//        @Field("acct") String username, @Field("pw") String password
//    ) : Response<Unit>
    //TODO create account needs different params
}