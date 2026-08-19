package com.example.data.remote

import android.content.Context
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.UUID
import java.util.concurrent.TimeUnit

class SupabaseAuthRepository(
    private val context: Context,
    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()
) {
    private val supabaseUrl: String
        get() {
            val fromConfig = try {
                val field = BuildConfig::class.java.getField("SUPABASE_URL")
                val url = field.get(null) as? String ?: ""
                url.trimEnd('/')
            } catch (_: Exception) {
                ""
            }
            return if (fromConfig.isNotBlank() && !fromConfig.contains("placeholder")) {
                fromConfig
            } else {
                DEFAULT_SUPABASE_URL
            }
        }

    private val supabaseAnonKey: String
        get() {
            val fromConfig = try {
                val field = BuildConfig::class.java.getField("SUPABASE_ANON_KEY")
                field.get(null) as? String ?: ""
            } catch (_: Exception) {
                ""
            }
            return if (fromConfig.isNotBlank() && !fromConfig.contains("placeholder")) {
                fromConfig
            } else {
                DEFAULT_SUPABASE_ANON_KEY
            }
        }

    private val masterKeyAlias = try {
        MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
    } catch (_: Exception) {
        "mizan_master_key"
    }

    private val encryptedPrefs = try {
        EncryptedSharedPreferences.create(
            "mizan_secure_auth_prefs",
            masterKeyAlias,
            context,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    } catch (_: Exception) {
        context.getSharedPreferences("mizan_fallback_auth_prefs", Context.MODE_PRIVATE)
    }

    fun getAccessToken(): String? {
        return encryptedPrefs.getString(KEY_ACCESS_TOKEN, null)
    }

    fun getRefreshToken(): String? {
        return encryptedPrefs.getString(KEY_REFRESH_TOKEN, null)
    }

    fun getExpiresAt(): Long {
        return encryptedPrefs.getLong(KEY_EXPIRES_AT, 0L)
    }

    fun getUserId(): String? {
        return encryptedPrefs.getString(KEY_USER_ID, null)
    }

    fun getUserEmail(): String? {
        return encryptedPrefs.getString(KEY_USER_EMAIL, null)
    }

    fun getUserDisplayName(): String? {
        return encryptedPrefs.getString(KEY_USER_NAME, null)
    }

    fun getUserPhotoUrl(): String? {
        return encryptedPrefs.getString(KEY_USER_PHOTO, null)
    }

    fun isSessionExpired(bufferSeconds: Long = 60L): Boolean {
        val expiresAt = getExpiresAt()
        if (expiresAt <= 0L) return true
        val currentTime = System.currentTimeMillis()
        return currentTime >= (expiresAt - (bufferSeconds * 1000L))
    }

    fun hasValidSession(): Boolean {
        val token = getAccessToken()
        val userId = getUserId()
        return !token.isNullOrBlank() && !userId.isNullOrBlank()
    }

    fun saveSession(
        accessToken: String,
        refreshToken: String?,
        userId: String,
        email: String?,
        displayName: String? = null,
        photoUrl: String? = null,
        expiresInSeconds: Long = 3600L
    ) {
        val expiresAt = System.currentTimeMillis() + (expiresInSeconds * 1000L)
        encryptedPrefs.edit()
            .putString(KEY_ACCESS_TOKEN, accessToken)
            .putString(KEY_REFRESH_TOKEN, refreshToken)
            .putLong(KEY_EXPIRES_AT, expiresAt)
            .putString(KEY_USER_ID, userId)
            .putString(KEY_USER_EMAIL, email)
            .putString(KEY_USER_NAME, displayName)
            .putString(KEY_USER_PHOTO, photoUrl)
            .apply()
    }

    suspend fun getValidAccessToken(): String? = withContext(Dispatchers.IO) {
        val currentToken = getAccessToken() ?: return@withContext null
        if (!isSessionExpired()) {
            return@withContext currentToken
        }
        val refreshed = refreshSession()
        if (refreshed) {
            return@withContext getAccessToken()
        }
        return@withContext null
    }

    suspend fun refreshSession(): Boolean = withContext(Dispatchers.IO) {
        val refreshToken = getRefreshToken() ?: return@withContext false
        try {
            val endpoint = "$supabaseUrl/auth/v1/token?grant_type=refresh_token"
            val payload = JSONObject().apply {
                put("refresh_token", refreshToken)
            }

            val request = Request.Builder()
                .url(endpoint)
                .addHeader("apikey", supabaseAnonKey)
                .addHeader("Content-Type", "application/json")
                .post(payload.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = okHttpClient.newCall(request).execute()
            val responseBody = response.body?.string().orEmpty()

            if (response.isSuccessful) {
                val json = JSONObject(responseBody)
                val newAccessToken = json.getString("access_token")
                val newRefreshToken = json.optString("refresh_token", refreshToken)
                val expiresIn = json.optLong("expires_in", 3600L)
                val userObj = json.optJSONObject("user")
                val userId = userObj?.optString("id") ?: getUserId() ?: ""
                val email = userObj?.optString("email") ?: getUserEmail()
                val userMeta = userObj?.optJSONObject("user_metadata")
                val name = userMeta?.optString("full_name") ?: userMeta?.optString("name") ?: getUserDisplayName()
                val photo = userMeta?.optString("avatar_url") ?: userMeta?.optString("picture") ?: getUserPhotoUrl()

                saveSession(
                    accessToken = newAccessToken,
                    refreshToken = newRefreshToken,
                    userId = userId,
                    email = email,
                    displayName = name,
                    photoUrl = photo,
                    expiresInSeconds = expiresIn
                )
                Log.d(TAG, "Successfully refreshed Supabase session for user $userId")
                return@withContext true
            } else {
                Log.e(TAG, "Refresh session failed: ${response.code} $responseBody")
                if (response.code in 400..403) {
                    logout()
                }
                return@withContext false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception refreshing Supabase session", e)
            return@withContext false
        }
    }

    suspend fun logout() = withContext(Dispatchers.IO) {
        val token = getAccessToken()
        if (token != null) {
            try {
                val endpoint = "$supabaseUrl/auth/v1/logout"
                val request = Request.Builder()
                    .url(endpoint)
                    .addHeader("apikey", supabaseAnonKey)
                    .addHeader("Authorization", "Bearer $token")
                    .post("{}".toRequestBody("application/json".toMediaType()))
                    .build()
                okHttpClient.newCall(request).execute().close()
            } catch (e: Exception) {
                Log.w(TAG, "Supabase remote logout request warning: ${e.message}")
            }
        }
        encryptedPrefs.edit().clear().apply()
    }

    suspend fun signInWithGoogleIdToken(
        idToken: String,
        rawNonce: String? = null,
        displayName: String? = null,
        photoUrl: String? = null
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val endpoint = "$supabaseUrl/auth/v1/token?grant_type=id_token"
            val payload = JSONObject().apply {
                put("provider", "google")
                put("id_token", idToken)
                if (!rawNonce.isNullOrBlank()) {
                    put("nonce", rawNonce)
                }
            }

            val request = Request.Builder()
                .url(endpoint)
                .addHeader("apikey", supabaseAnonKey)
                .addHeader("Content-Type", "application/json")
                .post(payload.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = okHttpClient.newCall(request).execute()
            val responseBody = response.body?.string().orEmpty()

            if (response.isSuccessful) {
                val json = JSONObject(responseBody)
                val accessToken = json.getString("access_token")
                val refreshToken = json.optString("refresh_token")
                val expiresIn = json.optLong("expires_in", 3600L)
                val userObj = json.getJSONObject("user")
                val userId = userObj.getString("id")
                val email = userObj.optString("email")
                val userMetadata = userObj.optJSONObject("user_metadata")
                val resolvedName = displayName ?: userMetadata?.optString("full_name") ?: userMetadata?.optString("name")
                val resolvedPhoto = photoUrl ?: userMetadata?.optString("avatar_url") ?: userMetadata?.optString("picture")

                saveSession(
                    accessToken = accessToken,
                    refreshToken = refreshToken,
                    userId = userId,
                    email = email,
                    displayName = resolvedName,
                    photoUrl = resolvedPhoto,
                    expiresInSeconds = expiresIn
                )
                Log.d(TAG, "Successfully signed in Supabase user: $userId")
                return@withContext true
            } else {
                Log.e(TAG, "Google auth error: ${response.code} $responseBody")
                return@withContext false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in signInWithGoogleIdToken", e)
            return@withContext false
        }
    }

    suspend fun fetchHouseholdMembership(): String? = withContext(Dispatchers.IO) {
        val token = getValidAccessToken() ?: return@withContext null
        val userId = getUserId() ?: return@withContext null

        try {
            val endpoint = "$supabaseUrl/rest/v1/household_members?user_id=eq.$userId&select=household_id"
            val request = Request.Builder()
                .url(endpoint)
                .addHeader("apikey", supabaseAnonKey)
                .addHeader("Authorization", "Bearer $token")
                .get()
                .build()

            val response = okHttpClient.newCall(request).execute()
            val body = response.body?.string().orEmpty()

            if (response.isSuccessful) {
                val array = JSONArray(body)
                if (array.length() > 0) {
                    val item = array.getJSONObject(0)
                    return@withContext item.getString("household_id")
                }
            } else {
                Log.e(TAG, "Error fetching household membership: ${response.code} $body")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to query household members", e)
        }
        return@withContext null
    }

    suspend fun acceptInvite(inviteToken: String): Boolean = withContext(Dispatchers.IO) {
        val token = getValidAccessToken() ?: return@withContext false

        try {
            val rpcEndpoint = "$supabaseUrl/rest/v1/rpc/accept_invite"
            val rpcPayload = JSONObject().apply {
                put("invite_token", inviteToken)
            }

            val rpcRequest = Request.Builder()
                .url(rpcEndpoint)
                .addHeader("apikey", supabaseAnonKey)
                .addHeader("Authorization", "Bearer $token")
                .addHeader("Content-Type", "application/json")
                .post(rpcPayload.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val rpcResponse = okHttpClient.newCall(rpcRequest).execute()
            if (rpcResponse.isSuccessful) {
                return@withContext true
            }

            // Fallback: Check invitations table directly
            val checkEndpoint = "$supabaseUrl/rest/v1/invitations?token=eq.$inviteToken&select=household_id,status"
            val checkRequest = Request.Builder()
                .url(checkEndpoint)
                .addHeader("apikey", supabaseAnonKey)
                .addHeader("Authorization", "Bearer $token")
                .get()
                .build()

            val checkResponse = okHttpClient.newCall(checkRequest).execute()
            val checkBody = checkResponse.body?.string().orEmpty()

            if (checkResponse.isSuccessful) {
                val array = JSONArray(checkBody)
                if (array.length() > 0) {
                    val item = array.getJSONObject(0)
                    val householdId = item.getString("household_id")
                    val userId = getUserId() ?: return@withContext false

                    val memberPayload = JSONObject().apply {
                        put("household_id", householdId)
                        put("user_id", userId)
                        put("role", "member")
                    }

                    val insertRequest = Request.Builder()
                        .url("$supabaseUrl/rest/v1/household_members")
                        .addHeader("apikey", supabaseAnonKey)
                        .addHeader("Authorization", "Bearer $token")
                        .addHeader("Content-Type", "application/json")
                        .post(memberPayload.toString().toRequestBody("application/json".toMediaType()))
                        .build()

                    val insertResponse = okHttpClient.newCall(insertRequest).execute()
                    return@withContext insertResponse.isSuccessful
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error accepting invite: $inviteToken", e)
        }
        return@withContext false
    }

    companion object {
        private const val TAG = "SupabaseAuthRepo"
        const val DEFAULT_SUPABASE_URL = "https://ecfgmznpkyekgpqsdhhr.supabase.co"
        const val DEFAULT_SUPABASE_ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImVjZmdtem5wa3lla2dwcXNkaGhyIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODcwMDQxNzAsImV4cCI6MjEwMjU4MDE3MH0.oEsbiq2G9iUmS6kcGGvAFKVf9fraDDB0kmukac6XQjE"

        private const val KEY_ACCESS_TOKEN = "supabase_access_token"
        private const val KEY_REFRESH_TOKEN = "supabase_refresh_token"
        private const val KEY_EXPIRES_AT = "supabase_expires_at"
        private const val KEY_USER_ID = "supabase_user_id"
        private const val KEY_USER_EMAIL = "supabase_user_email"
        private const val KEY_USER_NAME = "supabase_user_name"
        private const val KEY_USER_PHOTO = "supabase_user_photo"

        /**
         * Generates a cryptographic nonce pair: (rawNonce, sha256HashedNonce).
         * - hashedNonce is passed to Google Credential Manager (GetGoogleIdOption)
         * - rawNonce is passed to Supabase Auth (signInWithIdToken / token?grant_type=id_token)
         * Supabase will calculate SHA256(rawNonce) and compare it against the nonce claim in the ID token.
         */
        fun generateCryptoNonce(): Pair<String, String> {
            val randomBytes = ByteArray(32)
            SecureRandom().nextBytes(randomBytes)
            val rawNonce = randomBytes.joinToString("") { "%02x".format(it) }

            val md = MessageDigest.getInstance("SHA-256")
            val digest = md.digest(rawNonce.toByteArray(Charsets.UTF_8))
            val hashedNonce = digest.joinToString("") { "%02x".format(it) }

            return Pair(rawNonce, hashedNonce)
        }
    }
}
