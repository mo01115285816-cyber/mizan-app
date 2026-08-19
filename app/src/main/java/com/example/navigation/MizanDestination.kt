package com.example.navigation

/**
 * Type-safe navigation destination placeholders for Mizan.
 *
 * TODO: Full navigation graph with arguments will be configured in navigation phase.
 */
sealed interface MizanDestination {
    object Setup : MizanDestination
    object Home : MizanDestination
    object Blocked : MizanDestination
}
