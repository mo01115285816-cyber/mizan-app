package com.example.feature.blocked

/**
 * Architectural contract for Blocked / Quota Exhausted State.
 */
interface BlockedContract {
    data class State(
        val title: String = "اكتملت حصتك الشهرية",
        val subtitle: String = "تم إيقاف الإنترنت مؤقتاً",
        val statusBadgeText: String = "بانتظار تمديد الحصة",
        val returnNoticeText: String = "سيعود الاتصال عند تحديث الحد المسموح",
        val isBlocked: Boolean = true
    )
}
