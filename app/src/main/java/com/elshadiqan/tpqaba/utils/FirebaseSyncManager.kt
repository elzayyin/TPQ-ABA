package com.elshadiqan.tpqaba.utils

import android.util.Log
import com.elshadiqan.tpqaba.data.model.AppConfig
import com.elshadiqan.tpqaba.data.model.Kelas
import com.elshadiqan.tpqaba.data.model.Santri
import com.elshadiqan.tpqaba.data.model.Ustadz
import com.elshadiqan.tpqaba.data.model.Absensi
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class FirebaseSyncData(
    val appConfig: AppConfig? = null,
    val kelasList: List<Kelas>? = emptyList(),
    val santriList: List<Santri>? = emptyList(),
    val ustadzList: List<Ustadz>? = emptyList(),
    val absensiList: List<Absensi>? = emptyList()
)

object FirebaseSyncManager {
    private const val TAG = "FirebaseSyncManager"
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    fun buildUrl(baseUrl: String, authSecret: String): String {
        var url = baseUrl.trim()
        if (url.isEmpty()) {
            return ""
        }
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "https://$url"
        }
        if (!url.endsWith("/")) {
            url = "$url/"
        }
        url = "${url}tpq_data.json"
        if (authSecret.isNotEmpty()) {
            url = "$url?auth=$authSecret"
        }
        return url
    }

    suspend fun pushToFirebase(
        baseUrl: String,
        authSecret: String,
        syncData: FirebaseSyncData
    ): Boolean = withContext(Dispatchers.IO) {
        val url = buildUrl(baseUrl, authSecret)
        if (url.isEmpty()) {
            Log.e(TAG, "Empty Database URL supplied.")
            return@withContext false
        }

        try {
            val jsonAdapter = moshi.adapter(FirebaseSyncData::class.java)
            val jsonPayload = jsonAdapter.toJson(syncData)
            
            val requestBody = jsonPayload.toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
            val request = Request.Builder()
                .url(url)
                .put(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    Log.d(TAG, "Successfully pushed data to Firebase RTD.")
                    true
                } else {
                    Log.e(TAG, "Failed pushing data to Firebase. Code: ${response.code}, Message: ${response.message}")
                    false
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception pushing database contents to Firebase", e)
            false
        }
    }

    suspend fun pullFromFirebase(
        baseUrl: String,
        authSecret: String
    ): FirebaseSyncData? = withContext(Dispatchers.IO) {
        val url = buildUrl(baseUrl, authSecret)
        if (url.isEmpty()) {
            Log.e(TAG, "Empty Database URL supplied.")
            return@withContext null
        }

        try {
            val request = Request.Builder()
                .url(url)
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    if (responseBody.isNullOrEmpty() || responseBody == "null") {
                        Log.d(TAG, "Firebase returned empty or null database payload.")
                        return@withContext null
                    }
                    
                    val jsonAdapter = moshi.adapter(FirebaseSyncData::class.java)
                    val data = jsonAdapter.fromJson(responseBody)
                    Log.d(TAG, "Successfully pulled data from Firebase RTD.")
                    data
                } else {
                    Log.e(TAG, "Failed pulling data from Firebase. Code: ${response.code}")
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception pulling database contents from Firebase", e)
            null
        }
    }
}
