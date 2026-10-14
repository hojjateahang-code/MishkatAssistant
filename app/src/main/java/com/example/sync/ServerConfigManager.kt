package com.example.sync

import android.content.Context
import android.content.SharedPreferences

object ServerConfigManager {

    private const val PREFS_NAME = "mishkat_server_prefs"

    private const val KEY_ENDPOINT = "key_minio_endpoint"
    private const val KEY_ACCESS_KEY = "key_minio_access_key"
    private const val KEY_SECRET_KEY = "key_minio_secret_key"
    private const val KEY_BUCKET = "key_minio_bucket"
    private const val KEY_PREFIX = "key_minio_prefix"
    private const val KEY_GEMINI_API_KEY = "key_gemini_api_key"

    const val DEFAULT_ENDPOINT = "https://gift.nodrive.ir"
    const val DEFAULT_ACCESS_KEY = "ycvug2CTkf7gDpCnIVIS"
    const val DEFAULT_SECRET_KEY = "NOM0zd28HIkjZM7fcNKegOwa4N8GhwqmkocOi1ES"
    const val DEFAULT_BUCKET = "09107739189main"
    const val DEFAULT_PREFIX = "meshkat/"
    const val DEFAULT_GEMINI_API_KEY = ""

    private var prefs: SharedPreferences? = null

    fun init(context: Context) {
        if (prefs == null) {
            prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val currentKey = prefs?.getString(KEY_ACCESS_KEY, null)
            if (currentKey.isNullOrBlank() || currentKey == "ycvug2CTkf7gDpCnlVIS") {
                prefs?.edit()?.putString(KEY_ACCESS_KEY, DEFAULT_ACCESS_KEY)?.apply()
            }
            val currentSecret = prefs?.getString(KEY_SECRET_KEY, null)
            if (currentSecret.isNullOrBlank()) {
                prefs?.edit()?.putString(KEY_SECRET_KEY, DEFAULT_SECRET_KEY)?.apply()
            }
        }
    }

    fun getMinioEndpoint(): String {
        return prefs?.getString(KEY_ENDPOINT, null)?.ifBlank { DEFAULT_ENDPOINT } ?: DEFAULT_ENDPOINT
    }

    fun getMinioAccessKey(): String {
        return prefs?.getString(KEY_ACCESS_KEY, null)?.ifBlank { DEFAULT_ACCESS_KEY } ?: DEFAULT_ACCESS_KEY
    }

    fun getMinioSecretKey(): String {
        return prefs?.getString(KEY_SECRET_KEY, null)?.ifBlank { DEFAULT_SECRET_KEY } ?: DEFAULT_SECRET_KEY
    }

    fun getMinioBucket(): String {
        return prefs?.getString(KEY_BUCKET, null)?.ifBlank { DEFAULT_BUCKET } ?: DEFAULT_BUCKET
    }

    fun getMinioPrefix(): String {
        return prefs?.getString(KEY_PREFIX, null)?.ifBlank { DEFAULT_PREFIX } ?: DEFAULT_PREFIX
    }

    fun getGeminiApiKey(): String {
        return prefs?.getString(KEY_GEMINI_API_KEY, null)?.ifBlank { DEFAULT_GEMINI_API_KEY } ?: DEFAULT_GEMINI_API_KEY
    }

    fun saveConfig(
        endpoint: String,
        accessKey: String,
        secretKey: String,
        bucket: String,
        prefix: String,
        geminiApiKey: String
    ) {
        prefs?.edit()?.apply {
            putString(KEY_ENDPOINT, endpoint.trim().trimEnd('/'))
            putString(KEY_ACCESS_KEY, accessKey.trim())
            putString(KEY_SECRET_KEY, secretKey.trim())
            putString(KEY_BUCKET, bucket.trim())
            putString(KEY_PREFIX, prefix.trim())
            putString(KEY_GEMINI_API_KEY, geminiApiKey.trim())
            apply()
        }
    }

    fun resetToDefaults() {
        prefs?.edit()?.apply {
            putString(KEY_ENDPOINT, DEFAULT_ENDPOINT)
            putString(KEY_ACCESS_KEY, DEFAULT_ACCESS_KEY)
            putString(KEY_SECRET_KEY, DEFAULT_SECRET_KEY)
            putString(KEY_BUCKET, DEFAULT_BUCKET)
            putString(KEY_PREFIX, DEFAULT_PREFIX)
            putString(KEY_GEMINI_API_KEY, DEFAULT_GEMINI_API_KEY)
            apply()
        }
    }
}
