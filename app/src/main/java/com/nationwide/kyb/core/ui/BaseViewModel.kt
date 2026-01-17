package com.nationwide.kyb.core.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Base ViewModel with common error handling
 */
abstract class BaseViewModel : ViewModel() {
    
    protected fun handleException(throwable: Throwable) {
        // Log error - actual implementation depends on error handling strategy
        throwable.printStackTrace()
    }
    
    protected fun launchWithErrorHandling(
        errorHandler: ((Throwable) -> Unit)? = null,
        block: suspend CoroutineScope.() -> Unit
    ) {
        viewModelScope.launch(
            CoroutineExceptionHandler { _, throwable ->
                errorHandler?.invoke(throwable) ?: handleException(throwable)
            },
            block = block
        )
    }
}
