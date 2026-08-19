package com.example.core.common

/**
 * Generic state representation for asynchronous operations.
 */
sealed interface Resource<out T> {
    object Idle : Resource<Nothing>
    object Loading : Resource<Nothing>
    data class Success<out T>(val data: T) : Resource<T>
    data class Error(val message: String, val throwable: Throwable? = null) : Resource<Nothing>
}
