package com.example.core.model

import com.example.core.common.NetworkDetails

/**
 * Real-time network and home connection state.
 */
data class ConnectionState(
    val isConnected: Boolean = false,
    val isWifi: Boolean = false,
    val isHomeWifi: Boolean = false,
    val currentSsid: String = "",
    val networkDetails: NetworkDetails? = null
)
