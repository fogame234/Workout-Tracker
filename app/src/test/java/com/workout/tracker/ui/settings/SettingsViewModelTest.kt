package com.workout.tracker.ui.settings

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.workout.tracker.data.backup.BackupManager
import com.workout.tracker.testing.DatabaseTest
import com.workout.tracker.testing.FakeAppSettings
import com.workout.tracker.testing.MainDispatcherRule
import com.workout.tracker.testing.withDefaultLocale
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

@RunWith(RobolectricTestRunner::class)
class SettingsViewModelTest : DatabaseTest() {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val noonToday: Long = Calendar.getInstance(TimeZone.getDefault()).apply {
        set(Calendar.HOUR_OF_DAY, 12)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    private fun daysAgo(days: Int): Long = Calendar.getInstance(TimeZone.getDefault()).apply {
        timeInMillis = noonToday
        add(Calendar.DAY_OF_MONTH, -days)
    }.timeInMillis

    private fun viewModel(settings: FakeAppSettings): SettingsViewModel {
        val context: Context = ApplicationProvider.getApplicationContext()
        return SettingsViewModel(
            context,
            BackupManager(db, dayDao, exerciseDao, logDao, walkingDao),
            settings,
        )
    }

    private fun stateFor(lastBackupAt: Long?): SettingsUiState =
        withDefaultLocale(Locale.US) { viewModel(FakeAppSettings(lastBackupAt)).uiState.value }

    @Test
    fun `never backed up reads as no backup and counts as stale`() = runTest {
        val state = stateFor(null)

        assertEquals("No backup yet", state.lastBackupLabel)
        assertTrue(state.lastBackupIsStale)
    }

    @Test
    fun `a backup today reads as today`() = runTest {
        val state = stateFor(noonToday)

        assertEquals("Last backup: today", state.lastBackupLabel)
        assertEquals(false, state.lastBackupIsStale)
    }

    @Test
    fun `a backup yesterday reads as yesterday`() = runTest {
        val state = stateFor(daysAgo(1))

        assertEquals("Last backup: yesterday", state.lastBackupLabel)
        assertEquals(false, state.lastBackupIsStale)
    }

    @Test
    fun `an older backup reports how many days ago with the date`() = runTest {
        val state = stateFor(daysAgo(5))

        assertTrue(state.lastBackupLabel, state.lastBackupLabel.startsWith("Last backup: 5 days ago, "))
    }

    @Test
    fun `a recent backup is not flagged as stale`() = runTest {
        assertEquals(false, stateFor(daysAgo(13)).lastBackupIsStale)
    }

    @Test
    fun `a backup two weeks old is flagged as stale`() = runTest {
        assertTrue(stateFor(daysAgo(14)).lastBackupIsStale)
        assertTrue(stateFor(daysAgo(60)).lastBackupIsStale)
    }

    @Test
    fun `the line updates when a backup is recorded`() = runTest {
        val settings = FakeAppSettings(null)
        val vm = withDefaultLocale(Locale.US) { viewModel(settings) }
        assertEquals("No backup yet", vm.uiState.value.lastBackupLabel)

        settings.setLastBackupAt(noonToday)

        assertEquals("Last backup: today", vm.uiState.value.lastBackupLabel)
        assertEquals(false, vm.uiState.value.lastBackupIsStale)
    }
}
