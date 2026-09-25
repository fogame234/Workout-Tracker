package com.workout.tracker.data.guide

import com.workout.tracker.testing.DatabaseTest
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ExerciseGuidesTest : DatabaseTest() {

    private val guides = ExerciseGuides()

    /** Checks the real seeded data, so adding an exercise without a guide fails here. */
    @Test
    fun `every seeded exercise has a guide`() = runTest {
        val seededNames = exerciseDao.getAll().map { it.name }.distinct()

        assertTrue("Seeder produced no exercises", seededNames.isNotEmpty())

        val missing = seededNames.filterNot { guides.hasGuide(it) }
        assertEquals("Seeded exercises with no guide", emptyList<String>(), missing)
    }

    @Test
    fun `every seeded exercise guide has usable content`() = runTest {
        val seededNames = exerciseDao.getAll().map { it.name }.distinct()

        seededNames.forEach { name ->
            val guide = guides.forName(name)
            assertNotNull("No guide for $name", guide)
            requireNotNull(guide)

            assertTrue("$name: summary is blank", guide.summary.isNotBlank())
            assertTrue("$name: has no steps", guide.steps.isNotEmpty())
            assertTrue("$name: has a blank step", guide.steps.none { it.isBlank() })
            assertTrue("$name: has a blank tip", guide.tips.none { it.isBlank() })
            assertTrue("$name: musclesWorked is blank", guide.musclesWorked.isNotBlank())
        }
    }

    @Test
    fun `lookup of an unknown exercise returns null rather than throwing`() {
        assertEquals(null, guides.forName("Kettlebell Juggling"))
        assertEquals(false, guides.hasGuide("Kettlebell Juggling"))
    }
}
