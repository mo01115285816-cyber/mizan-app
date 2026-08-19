package com.example.core.model

/**
 * State of local internet restriction / VPN enforcement.
 */
data class EnforcementState(
    val isEnforced: Boolean = false,
    val reason: String = "",
    val isVpnActive: Boolean = false,
    val isVpnPrepared: Boolean = false,
    val canBypass: Boolean = true
)
