
import okhttp3.OkHttpClient
import okhttp3.Request

class ApiClient {

    fun request(url: String): String {
        val request: Request = Request.Builder()
            .url(url)
            .build()

        OkHttpClient().newCall(request).execute().use { response ->
            return response.body!!.string()
        }
    }

}