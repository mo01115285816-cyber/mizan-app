package com.example.feature.setup

/**
 * UI State for the Mizan Setup / First-run Linking screen.
 */
data class SetupUiState(
    val userName: String = "",
    val linkingCode: String = "",
    val isActivating: Boolean = false,
    val userNameError: String? = null,
    val linkingCodeError: String? = null,
    val isFormValid: Boolean = false
)
