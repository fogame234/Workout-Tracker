package com.workout.tracker.ui.walking

import com.workout.tracker.testing.FakeWorkoutRepository
import com.workout.tracker.testing.MainDispatcherRule
import com.workout.tracker.testing.withDefaultLocale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.util.Locale

class WalkingViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun viewModel(repo: FakeWorkoutRepository = FakeWorkoutRepository()) =
        WalkingViewModel(repo)

    // -------------------------------------------------------------- unit conversion

    @Test
    fun `entering miles fills in kilometres`() {
        val vm = viewModel()

        vm.onMilesChange("3")

        assertEquals(4.83, vm.uiState.value.km.toDouble(), 0.01)
    }

    @Test
    fun `entering kilometres fills in miles`() {
        val vm = viewModel()

        vm.onKmChange("5")

        assertEquals(3.11, vm.uiState.value.miles.toDouble(), 0.01)
    }

    @Test
    fun `clearing the distance clears the converted field`() {
        val vm = viewModel()
        vm.onMilesChange("3")

        vm.onMilesChange("")

        assertEquals("", vm.uiState.value.km)
    }

    // ---------------------------------------------------- comma decimal locale (H-2)

    /** The converted field is parsed back with toDoubleOrNull, which only accepts '.'. */
    @Test
    fun `converted miles stay parseable in a comma decimal locale`() {
        withDefaultLocale(Locale.GERMANY) {
            val vm = viewModel()

            vm.onKmChange("5")

            assertEquals(3.11, vm.uiState.value.miles.toDoubleOrNull()!!, 0.01)
        }
    }

    @Test
    fun `a walk entered in kilometres saves in a comma decimal locale`() {
        val repo = FakeWorkoutRepository()

        withDefaultLocale(Locale.GERMANY) {
            val vm = viewModel(repo)
            vm.onKmChange("5")
            vm.onDurMinChange("45")

            vm.save()

            assertNull("save was blocked: ${vm.uiState.value.errorMessage}", vm.uiState.value.errorMessage)
        }

        val walk = repo.walkingLogs.single()
        assertEquals(3.11, walk.distanceMiles, 0.01)
        assertEquals(5.0, walk.distanceKm, 0.01)
        assertEquals(2700L, walk.durationSeconds)
    }

    @Test
    fun `converted kilometres stay parseable in a comma decimal locale`() {
        withDefaultLocale(Locale.GERMANY) {
            val vm = viewModel()

            vm.onMilesChange("3")

            assertEquals(4.83, vm.uiState.value.km.toDoubleOrNull()!!, 0.01)
        }
    }

    // ------------------------------------------------------------------- validation

    @Test
    fun `a walk requires a distance`() {
        val repo = FakeWorkoutRepository()
        val vm = viewModel(repo)
        vm.onDurMinChange("30")

        vm.save()

        assertEquals("Enter a distance", vm.uiState.value.errorMessage)
        assertTrue(repo.walkingLogs.isEmpty())
    }

    @Test
    fun `a walk rejects a zero distance`() {
        val repo = FakeWorkoutRepository()
        val vm = viewModel(repo)
        vm.onMilesChange("0")
        vm.onDurMinChange("30")

        vm.save()

        assertEquals("Enter a distance", vm.uiState.value.errorMessage)
        assertTrue(repo.walkingLogs.isEmpty())
    }

    @Test
    fun `a walk requires a duration`() {
        val repo = FakeWorkoutRepository()
        val vm = viewModel(repo)
        vm.onMilesChange("3")

        vm.save()

        assertEquals("Enter the duration", vm.uiState.value.errorMessage)
        assertTrue(repo.walkingLogs.isEmpty())
    }

    // ------------------------------------------------------------------------ saving

    @Test
    fun `a successful save clears the form`() {
        val vm = viewModel()
        vm.onMilesChange("3")
        vm.onDurMinChange("45")
        vm.onNotesChange("easy pace")

        vm.save()

        val state = vm.uiState.value
        assertEquals("", state.miles)
        assertEquals("", state.km)
        assertEquals("", state.durationMinutes)
        assertEquals("", state.durationSeconds)
        assertEquals("", state.notes)
    }

    @Test
    fun `minutes and seconds combine into total seconds`() {
        val repo = FakeWorkoutRepository()
        val vm = viewModel(repo)
        vm.onMilesChange("2")
        vm.onDurMinChange("32")
        vm.onDurSecChange("15")

        vm.save()

        assertEquals(1935L, repo.walkingLogs.single().durationSeconds)
    }

    @Test
    fun `blank notes are stored as null rather than an empty string`() {
        val repo = FakeWorkoutRepository()
        val vm = viewModel(repo)
        vm.onMilesChange("2")
        vm.onDurMinChange("30")

        vm.save()

        assertNull(repo.walkingLogs.single().notes)
    }

    @Test
    fun `kilometres are derived from miles when only miles are entered`() {
        val repo = FakeWorkoutRepository()
        val vm = viewModel(repo)
        vm.onMilesChange("3")
        vm.onDurMinChange("45")

        vm.save()

        assertEquals(4.83, repo.walkingLogs.single().distanceKm, 0.01)
    }
}
