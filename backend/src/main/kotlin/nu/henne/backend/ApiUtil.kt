package nu.henne.backend

import com.google.gson.Gson
import okhttp3.OkHttpClient

class ApiUtil {
    companion object {
        val CLIENT = OkHttpClient()
        val GSON = Gson()
    }
}