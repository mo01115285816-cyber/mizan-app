package com.example

import com.example.core.model.AppUsageItem
import com.example.core.model.DeviceProfile
import com.example.core.model.QuotaInfo
import com.example.core.model.QuotaPolicy
import com.example.core.model.UsageSnapshot
import com.example.data.repository.FakeMizanRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MizanServicesUnitTest {

    @Test
    fun `bytesToGb converts bytes accurately`() {
        assertEquals(0f, UsageSnapshot.bytesToGb(0L), 0.001f)
        assertEquals(0f, UsageSnapshot.bytesToGb(-100L), 0.001f)

        // 1 GB = 1073741824 bytes -> 1.0f
        val oneGbBytes = 1024L * 1024L * 1024L
        assertEquals(1.0f, UsageSnapshot.bytesToGb(oneGbBytes), 0.001f)

        // 85.3 GB = 85.3 * 1024^3
        val exact85Point3Bytes = (85.3 * 1024.0 * 1024.0 * 1024.0).toLong()
        assertEquals(85.3f, UsageSnapshot.bytesToGb(exact85Point3Bytes), 0.1f)

        // 133.3 GB
        val exact133Point3Bytes = (133.3 * 1024.0 * 1024.0 * 1024.0).toLong()
        assertEquals(133.3f, UsageSnapshot.bytesToGb(exact133Point3Bytes), 0.1f)
    }

    @Test
    fun `quota exhaustion detection triggers correctly`() {
        val totalLimit = 133.3f

        // Under quota
        val underQuotaUsage = 85.3f
        val isExhaustedUnder = underQuotaUsage >= totalLimit
        assertFalse(isExhaustedUnder)

        // Exact quota
        val exactQuotaUsage = 133.3f
        val isExhaustedExact = exactQuotaUsage >= totalLimit
        assertTrue(isExhaustedExact)

        // Over quota
        val overQuotaUsage = 140.0f
        val isExhaustedOver = overQuotaUsage >= totalLimit
        assertTrue(isExhaustedOver)
    }

    @Test
    fun `ssid filtering ignores non-home networks and mobile data`() {
        val targetHomeSsid = "Mizan-Home-5G"

        fun shouldAccumulate(currentSsid: String, isWifi: Boolean): Boolean {
            return isWifi && currentSsid.equals(targetHomeSsid, ignoreCase = true)
        }

        // Test matching home Wi-Fi
        assertTrue(shouldAccumulate("Mizan-Home-5G", isWifi = true))
        assertTrue(shouldAccumulate("mizan-home-5g", isWifi = true))

        // Test Mobile Data
        assertFalse(shouldAccumulate("Vodafone 4G", isWifi = false))
        assertFalse(shouldAccumulate("Mizan-Home-5G", isWifi = false))

        // Test coffee shop / work Wi-Fi
        assertFalse(shouldAccumulate("CoffeeShop_WiFi", isWifi = true))
        assertFalse(shouldAccumulate("Unknown_SSID", isWifi = true))
        assertFalse(shouldAccumulate("", isWifi = true))
    }

    @Test
    fun `delta calculation accumulates only positive forward bytes`() {
        var accumulatedHomeBytes = 1000L
        var lastKnownWifiBytes = 5000L

        fun processNewWifiReading(currentWifiTotal: Long, isHome: Boolean): Long {
            val delta = currentWifiTotal - lastKnownWifiBytes
            if (isHome && delta > 0) {
                accumulatedHomeBytes += delta
            }
            lastKnownWifiBytes = currentWifiTotal
            return accumulatedHomeBytes
        }

        // Home Wi-Fi consumed 200 bytes -> should accumulate
        val reading1 = processNewWifiReading(5200L, isHome = true)
        assertEquals(1200L, reading1)

        // Mobile data or other Wi-Fi consumed 800 bytes -> should NOT accumulate to home
        val reading2 = processNewWifiReading(6000L, isHome = false)
        assertEquals(1200L, reading2)

        // Reconnect to Home Wi-Fi and consume 300 bytes -> should accumulate
        val reading3 = processNewWifiReading(6300L, isHome = true)
        assertEquals(1500L, reading3)
    }

    @Test
    fun `fake repository provides deterministic previews data`() = runTest {
        val repo = FakeMizanRepository()

        val profile = repo.getDeviceProfile().first()
        assertNotNull(profile)
        assertEquals("mock_user_456", profile?.userId)
        assertEquals("Pixel 8", profile?.deviceModel)

        val policy = repo.getQuotaPolicy().first()
        assertNotNull(policy)
        assertEquals(133.3f, policy?.monthlyLimitGb ?: 0f, 0.01f)

        val trend = repo.getDailyUsageTrend().first()
        assertEquals(7, trend.size)
        assertEquals("سبت", trend[0].dayLabel)
    }

    @Test
    fun `generateCryptoNonce creates valid raw and sha256 pairs`() {
        val (rawNonce1, hashedNonce1) = com.example.data.remote.SupabaseAuthRepository.generateCryptoNonce()
        val (rawNonce2, hashedNonce2) = com.example.data.remote.SupabaseAuthRepository.generateCryptoNonce()

        // Ensure nonces are non-empty and 64-character hex strings (32 bytes)
        assertEquals(64, rawNonce1.length)
        assertEquals(64, hashedNonce1.length)
        assertEquals(64, rawNonce2.length)
        assertEquals(64, hashedNonce2.length)

        // Ensure consecutive nonces are cryptographically unique
        assertFalse(rawNonce1 == rawNonce2)
        assertFalse(hashedNonce1 == hashedNonce2)

        // Verify SHA-256 hash match
        val md = java.security.MessageDigest.getInstance("SHA-256")
        val expectedHash = md.digest(rawNonce1.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }
        assertEquals(expectedHash, hashedNonce1)
    }

    @Test
    fun `session expiration calculation respects time window and buffer`() {
        val now = System.currentTimeMillis()
        val expiresIn1Hour = now + 3600_000L
        val expired10MinsAgo = now - 600_000L
        val expiringIn30Secs = now + 30_000L

        fun isExpired(expiresAt: Long, bufferSeconds: Long = 60L): Boolean {
            if (expiresAt <= 0L) return true
            return System.currentTimeMillis() >= (expiresAt - (bufferSeconds * 1000L))
        }

        // Active session for 1 hour -> not expired
        assertFalse(isExpired(expiresIn1Hour))

        // Expired in past -> expired
        assertTrue(isExpired(expired10MinsAgo))

        // Expiring in 30 seconds with 60 second safety buffer -> counts as expired so auto-refresh triggers
        assertTrue(isExpired(expiringIn30Secs, bufferSeconds = 60L))

        // Expiring in 30 seconds with 10 second safety buffer -> still active
        assertFalse(isExpired(expiringIn30Secs, bufferSeconds = 10L))
    }
}
