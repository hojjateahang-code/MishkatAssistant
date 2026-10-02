package com.example.sync

import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.net.URL
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

object MinioSyncClient {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    fun getEndpoint(): String = try {
        BuildConfig.MINIO_ENDPOINT.ifBlank { "https://gift.nodrive.ir" }.trimEnd('/')
    } catch (e: Throwable) {
        "https://gift.nodrive.ir"
    }

    fun getBucket(): String = try {
        BuildConfig.MINIO_BUCKET.ifBlank { "09107739189main" }
    } catch (e: Throwable) {
        "09107739189main"
    }

    fun getPrefix(): String = try {
        BuildConfig.MINIO_PREFIX.ifBlank { "meshkat/" }
    } catch (e: Throwable) {
        "meshkat/"
    }

    private fun getAccessKey(): String = try {
        BuildConfig.MINIO_ACCESS_KEY_ID.ifBlank { "ycvug2CTkf7gDpCnlVIS" }
    } catch (e: Throwable) {
        "ycvug2CTkf7gDpCnlVIS"
    }

    private fun getSecretKey(): String = try {
        BuildConfig.MINIO_SECRET_ACCESS_KEY.ifBlank { "NOM0zd28HIkjZM7fcNKegOwa4N8GhwqmkocOi1ES" }
    } catch (e: Throwable) {
        "NOM0zd28HIkjZM7fcNKegOwa4N8GhwqmkocOi1ES"
    }

    /**
     * Uploads the backup JSON payload to MinIO using AWS SigV4
     */
    suspend fun uploadBackup(
        jsonContent: String,
        fileName: String = "backup_latest.json"
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val endpoint = getEndpoint()
            val bucket = getBucket()
            val prefix = getPrefix().trimStart('/')
            val objectKey = "$prefix$fileName"
            val fullUrlStr = "$endpoint/$bucket/$objectKey"

            val url = URL(fullUrlStr)
            val host = url.host
            val canonicalUri = url.path

            val payloadBytes = jsonContent.toByteArray(Charsets.UTF_8)
            val payloadHash = sha256Hex(payloadBytes)

            val now = Date()
            val amzFormat = SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            val dateFormat = SimpleDateFormat("yyyyMMdd", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            val amzDate = amzFormat.format(now)
            val dateStamp = dateFormat.format(now)

            val region = "us-east-1"
            val service = "s3"
            val signedHeaders = "host;x-amz-content-sha256;x-amz-date"
            val canonicalHeaders = "host:$host\nx-amz-content-sha256:$payloadHash\nx-amz-date:$amzDate\n"

            val canonicalRequest = "PUT\n$canonicalUri\n\n$canonicalHeaders\n$signedHeaders\n$payloadHash"
            val credentialScope = "$dateStamp/$region/$service/aws4_request"
            val stringToSign = "AWS4-HMAC-SHA256\n$amzDate\n$credentialScope\n${sha256Hex(canonicalRequest.toByteArray(Charsets.UTF_8))}"

            val signingKey = getSignatureKey(getSecretKey(), dateStamp, region, service)
            val signature = hex(hmacSha256(signingKey, stringToSign))

            val authHeader = "AWS4-HMAC-SHA256 Credential=${getAccessKey()}/$credentialScope, SignedHeaders=$signedHeaders, Signature=$signature"

            val requestBody = payloadBytes.toRequestBody("application/json; charset=utf-8".toMediaType())
            val request = Request.Builder()
                .url(fullUrlStr)
                .put(requestBody)
                .addHeader("host", host)
                .addHeader("x-amz-date", amzDate)
                .addHeader("x-amz-content-sha256", payloadHash)
                .addHeader("Authorization", authHeader)
                .build()

            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                Result.success("پشتیبان با موفقیت در فضای ابری ذخیره شد. ($objectKey)")
            } else {
                val errBody = response.body?.string() ?: ""
                Result.failure(Exception("خطا در آپلود ابری (${response.code}): $errBody"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Downloads the latest backup JSON from MinIO
     */
    suspend fun downloadLatestBackup(
        fileName: String = "backup_latest.json"
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val endpoint = getEndpoint()
            val bucket = getBucket()
            val prefix = getPrefix().trimStart('/')
            val objectKey = "$prefix$fileName"
            val fullUrlStr = "$endpoint/$bucket/$objectKey"

            val url = URL(fullUrlStr)
            val host = url.host
            val canonicalUri = url.path

            val payloadHash = sha256Hex(ByteArray(0))

            val now = Date()
            val amzFormat = SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            val dateFormat = SimpleDateFormat("yyyyMMdd", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            val amzDate = amzFormat.format(now)
            val dateStamp = dateFormat.format(now)

            val region = "us-east-1"
            val service = "s3"
            val signedHeaders = "host;x-amz-content-sha256;x-amz-date"
            val canonicalHeaders = "host:$host\nx-amz-content-sha256:$payloadHash\nx-amz-date:$amzDate\n"

            val canonicalRequest = "GET\n$canonicalUri\n\n$canonicalHeaders\n$signedHeaders\n$payloadHash"
            val credentialScope = "$dateStamp/$region/$service/aws4_request"
            val stringToSign = "AWS4-HMAC-SHA256\n$amzDate\n$credentialScope\n${sha256Hex(canonicalRequest.toByteArray(Charsets.UTF_8))}"

            val signingKey = getSignatureKey(getSecretKey(), dateStamp, region, service)
            val signature = hex(hmacSha256(signingKey, stringToSign))

            val authHeader = "AWS4-HMAC-SHA256 Credential=${getAccessKey()}/$credentialScope, SignedHeaders=$signedHeaders, Signature=$signature"

            val request = Request.Builder()
                .url(fullUrlStr)
                .get()
                .addHeader("host", host)
                .addHeader("x-amz-date", amzDate)
                .addHeader("x-amz-content-sha256", payloadHash)
                .addHeader("Authorization", authHeader)
                .build()

            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val bodyStr = response.body?.string()
                if (!bodyStr.isNullOrBlank()) {
                    Result.success(bodyStr)
                } else {
                    Result.failure(Exception("فایل پشتیبان در سرور خالی است."))
                }
            } else {
                val errBody = response.body?.string() ?: ""
                Result.failure(Exception("خطا در دریافت فایل از سرور (${response.code}): $errBody"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun sha256Hex(data: ByteArray): String {
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(data)
        return hex(digest)
    }

    private fun hmacSha256(key: ByteArray, data: String): ByteArray {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(key, "HmacSHA256"))
        return mac.doFinal(data.toByteArray(Charsets.UTF_8))
    }

    private fun hex(bytes: ByteArray): String {
        val sb = StringBuilder(bytes.size * 2)
        for (b in bytes) {
            sb.append(String.format("%02x", b.toInt() and 0xff))
        }
        return sb.toString()
    }

    private fun getSignatureKey(key: String, dateStamp: String, regionName: String, serviceName: String): ByteArray {
        val kSecret = ("AWS4$key").toByteArray(Charsets.UTF_8)
        val kDate = hmacSha256(kSecret, dateStamp)
        val kRegion = hmacSha256(kDate, regionName)
        val kService = hmacSha256(kRegion, serviceName)
        return hmacSha256(kService, "aws4_request")
    }
}
