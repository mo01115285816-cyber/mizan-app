package com.example.core.model

import com.example.core.common.NetworkDetails
import com.example.feature.home.DayUsage

/**
 * Architectural states of the Mizan application based on Supabase Auth + Household Invites:
 * - SignedOut: User is not authenticated.
 * - SessionRestoring: App is verifying local credentials.
 * - SigningIn: Authenticating with Google ID Token via Supabase Auth.
 * - WaitingForInvite: User authenticated, but not part of any household yet.
 * - JoiningHousehold: Consuming temporary invite token.
 * - DeviceLinking: Registering device under the user's household.
 * - Ready: Device linked and monitoring active consumption.
 * - QuotaExhausted: Monthly quota reached or device blocked by household admin.
 * - AuthError: Authentication failure.
 * - InviteError: Invalid or expired invite link.
 * - NetworkError: Connectivity or server sync error.
 */
sealed interface AppState {
    data object SignedOut : AppState
    data object SessionRestoring : AppState
    data object SigningIn : AppState
    data object WaitingForInvite : AppState
    data object JoiningHousehold : AppState
    data object DeviceLinking : AppState

    data class Ready(
        val quotaInfo: QuotaInfo,
        val topApps: List<AppUsageItem> = emptyList(),
        val dailyTrend: List<DayUsage> = emptyList(),
        val networkDetails: NetworkDetails? = null,
        val isOffline: Boolean = false
    ) : AppState

    data class QuotaExhausted(
        val reason: String = "اكتملت حصتك الشهرية",
        val isVpnEnforced: Boolean = false
    ) : AppState

    data class AuthError(val message: String) : AppState
    data class InviteError(val message: String) : AppState
    data class NetworkError(val message: String) : AppState
}
