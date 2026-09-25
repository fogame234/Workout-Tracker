package com.workout.tracker.testing

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description

/**
 * Swaps Dispatchers.Main for a test dispatcher so viewModelScope works off-device.
 * Unconfined, so work launched in a ViewModel's init has run by the time it returns.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule(
    private val dispatcher: kotlinx.coroutines.CoroutineDispatcher = UnconfinedTestDispatcher(),
) : TestWatcher() {

    override fun starting(description: Description) = Dispatchers.setMain(dispatcher)

    override fun finished(description: Description) = Dispatchers.resetMain()
}

/** Runs [block] under [locale], restoring the previous default afterwards. */
fun <T> withDefaultLocale(locale: java.util.Locale, block: () -> T): T {
    val previous = java.util.Locale.getDefault()
    java.util.Locale.setDefault(locale)
    return try {
        block()
    } finally {
        java.util.Locale.setDefault(previous)
    }
}
