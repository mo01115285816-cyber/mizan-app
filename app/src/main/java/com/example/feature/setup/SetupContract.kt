package com.example.feature.setup

/**
 * Setup Screen architectural contract.
 *
 * TODO: UI and ViewModel logic will be implemented in the respective feature phase.
 */
interface SetupContract {
    data class State(
        val userName: String = "",
        val linkingCode: String = "",
        val isLoading: Boolean = false,
        val errorMessage: String? = null
    )

    sealed interface Event {
        data class OnUserNameChanged(val name: String) : Event
        data class OnLinkingCodeChanged(val code: String) : Event
        object OnSubmitClicked : Event
        object OnHelpClicked : Event
    }

    sealed interface Effect {
        object NavigateToHome : Effect
        data class ShowToast(val message: String) : Effect
    }
}
