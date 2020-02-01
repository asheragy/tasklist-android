package org.cerion.tasklist.googleapi

import org.cerion.tasklist.BuildConfig

//Error from tasks API, json encoded with error code and message
class GoogleApiException(message: String, val errorCode: Int) : Exception("Error $errorCode: $message")

class GoogleApi(private val mAuthKey: String) {

    val taskListsApi: GoogleTasklistsApi
        get() = GoogleTasklistsApi(mAuthKey)

    val tasksApi: GoogleTasksApi
        get() = GoogleTasksApi(mAuthKey)

    companion object {
        internal const val API_KEY = BuildConfig.GOOGLE_TASKS_APIKEY
        internal const val mLogFullResults = true
        internal const val TASKS_BASE_URL = "https://www.googleapis.com/tasks/v1/"
    }
}

/* Reference if converting to retrofit
interface GoogleTasksService {
    @GET("users/@me/lists")
    fun getLists(): Call<GoogleTaskLists>
}

@Parcelize
data class GoogleTaskList(
        val id: String,
        val etag: String,
        val title: String,
       // val updated: Date,
        val selfLink: String) : Parcelable

data class GoogleTaskLists(val items: List<GoogleTaskList>)

            val moshi = Moshi.Builder()
                    .add(KotlinJsonAdapterFactory())
                    //.add(Date::class.java, Rfc3339DateJsonAdapter().nullSafe())
                    .build()

            val client = OkHttpClient.Builder()
                    .addInterceptor { chain ->
                        val request = chain.request().newBuilder().addHeader("Authorization", "Bearer $mAuthKey").build()
                        chain.proceed(request)
                    }
                    .proxy(Proxy.NO_PROXY)
                    .build()

            // TODO change pattern for creating this
            val retrofit = Retrofit.Builder()
                    .addConverterFactory(MoshiConverterFactory.create(moshi))
                    //.addCallAdapterFactory(CoroutineCallAdapterFactory())
                    .addConverterFactory(ScalarsConverterFactory.create())
                    .baseUrl(TASKS_BASE_URL)
                    .client(client)
                    .build()

            return retrofit.create(GoogleTasksService::class.java)

 */