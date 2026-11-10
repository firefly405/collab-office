package com.collaboffice.core.network.auth

import com.collaboffice.core.model.ApiResponse
import com.collaboffice.core.model.auth.RefreshTokenRequest
import com.collaboffice.core.model.auth.TokenData
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TokenRefreshAuthenticator @Inject constructor(
    private val tokenManager: TokenManager,
    private val moshi: Moshi
) : Authenticator {

    override fun authenticate(route: Route?, response: Response): Request? {
        // Don't retry if already attempted refresh
        if (response.request.header("X-Retry") != null) return null

        val refreshToken = tokenManager.refreshToken ?: run {
            tokenManager.clear()
            return null
        }

        synchronized(this) {
            // Double-check: another thread may have already refreshed
            val currentToken = tokenManager.accessToken
            val requestToken = response.request.header("Authorization")?.removePrefix("Bearer ")
            if (currentToken != null && currentToken != requestToken) {
                // Token was already refreshed by another thread
                return response.request.newBuilder()
                    .header("Authorization", "Bearer $currentToken")
                    .header("X-Retry", "true")
                    .build()
            }

            // Perform token refresh
            val refreshRequest = RefreshTokenRequest(refreshToken)
            val jsonBody = moshi.adapter(RefreshTokenRequest::class.java).toJson(refreshRequest)
            val body = jsonBody.toRequestBody("application/json".toMediaType())

            val refreshResponse = OkHttpClient().newCall(
                Request.Builder()
                    .url(response.request.url.toString().substringBefore("/api/") + "/api/v1/auth/refresh-token")
                    .post(body)
                    .build()
            ).execute()

            if (refreshResponse.isSuccessful) {
                val type = Types.newParameterizedType(ApiResponse::class.java, TokenData::class.java)
                val adapter = moshi.adapter<ApiResponse<TokenData>>(type)
                val result = adapter.fromJson(refreshResponse.body?.string() ?: "")
                val tokenData = result?.data
                if (tokenData != null) {
                    tokenManager.saveTokens(
                        tokenData.getEffectiveToken(),
                        tokenData.refreshToken ?: refreshToken
                    )
                    return response.request.newBuilder()
                        .header("Authorization", "Bearer ${tokenData.getEffectiveToken()}")
                        .header("X-Retry", "true")
                        .build()
                }
            }

            // Refresh failed
            tokenManager.clear()
            return null
        }
    }
}
