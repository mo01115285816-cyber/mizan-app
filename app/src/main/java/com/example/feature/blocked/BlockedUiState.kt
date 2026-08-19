package com.example.feature.blocked

/**
 * State for Blocked / Quota Exhausted Screen.
 */
data class BlockedUiState(
    val title: String = "اكتملت حصتك الشهرية",
    val subtitle: String = "تم إيقاف الإنترنت مؤقتاً",
    val statusBadgeText: String = "بانتظار تمديد الحصة",
    val returnNoticeText: String = "سيعود الاتصال عند تحديث الحد المسموح",
    val isBlocked: Boolean = true
)
