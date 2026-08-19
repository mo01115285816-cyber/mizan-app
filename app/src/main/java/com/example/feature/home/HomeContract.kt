package com.example.feature.home

import com.example.core.model.AppUsageItem
import com.example.core.model.QuotaInfo

/**
 * Home Feature architectural contract.
 *
 * TODO: UI and ViewModel logic will be implemented in the respective feature phase.
 */
interface HomeContract {
    data class State(
        val quotaInfo: QuotaInfo = QuotaInfo(),
        val topApps: List<AppUsageItem> = emptyList(),
        val isLoading: Boolean = false
    )

    sealed interface Event {
        object OnRefresh : Event
        object OnProfileClicked : Event
    }

    sealed interface Effect {
        data class ShowMessage(val text: String) : Effect
    }
}
