package com.example.ai

import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object GeminiAssistantService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    suspend fun queryThinkingAssistant(
        prompt: String,
        contextSummary: String
    ): Result<String> = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext Result.failure(
                IllegalStateException("کلید API جمینای تنظیم نشده است. لطفاً کلید API را در پانل Secrets تنظیم کنید.")
            )
        }

        val systemPrompt = """
            شما "دستیار هوشمند مشکاه" هستید؛ یک مشاور و تحلیل‌گر دانا، دقیق و مؤدب برای مدیران، روحانیون، مبلغان و فعالان حوزه علمیه و مسجد.
            وظایف شما:
            ۱. تحلیل دقیق ساعت حضور و غیاب (کسری کار یا اضافه کار) و ارائه راهکارهای بهینه‌سازی زمان.
            ۲. بررسی ریز فعالیت‌ها و پیشنهاد برنامه‌های فرهنگی و قرآنی متناسب با مناسبت‌های ملی و مذهبی.
            ۳. تحلیل حسابرسی مالی، بررسی مانده حساب‌ها، ورودی و خروجی‌های مالی حوزه و مسجد و مراقبت از شفافیت اسناد.
            
            اطلاعات فعلی سیستم کاربر:
            $contextSummary
            
            پاسخ‌های خود را با لحن محترمانه، کاربردی، کاملاً فارسی و روان همراه با راهکارهای عملی ارائه دهید.
        """.trimIndent()

        try {
            // Build JSON payload manually using Android standard JSONObject
            val rootObj = JSONObject()

            // Contents
            val contentsArr = JSONArray()
            val contentObj = JSONObject()
            val partsArr = JSONArray()
            val partObj = JSONObject()
            partObj.put("text", prompt)
            partsArr.put(partObj)
            contentObj.put("parts", partsArr)
            contentsArr.put(contentObj)
            rootObj.put("contents", contentsArr)

            // System Instruction
            val sysContentObj = JSONObject()
            val sysPartsArr = JSONArray()
            val sysPartObj = JSONObject()
            sysPartObj.put("text", systemPrompt)
            sysPartsArr.put(sysPartObj)
            sysContentObj.put("parts", sysPartsArr)
            rootObj.put("systemInstruction", sysContentObj)

            // Generation Config with Thinking Config
            val genConfigObj = JSONObject()
            val thinkingConfigObj = JSONObject()
            thinkingConfigObj.put("thinkingLevel", "HIGH")
            genConfigObj.put("thinkingConfig", thinkingConfigObj)
            rootObj.put("generationConfig", genConfigObj)

            val jsonString = rootObj.toString()
            val requestBody = jsonString.toRequestBody("application/json".toMediaType())

            val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.1-pro-preview:generateContent?key=$apiKey"

            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()

            if (response.isSuccessful && responseBody != null) {
                val resObj = JSONObject(responseBody)
                val candidates = resObj.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val firstCand = candidates.getJSONObject(0)
                    val candContent = firstCand.optJSONObject("content")
                    val candParts = candContent?.optJSONArray("parts")
                    if (candParts != null && candParts.length() > 0) {
                        val text = candParts.getJSONObject(0).optString("text", "")
                        if (text.isNotBlank()) {
                            return@withContext Result.success(text)
                        }
                    }
                }
                Result.failure(Exception("پاسخی از مدل دریافت نشد."))
            } else {
                Result.failure(Exception("خطا در پاسخ سرور (${response.code}): ${responseBody ?: response.message}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
