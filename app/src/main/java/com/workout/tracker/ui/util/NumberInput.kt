package com.workout.tracker.ui.util

import java.util.Locale

/**
 * Formats for a text field the app parses back with [String.toDoubleOrNull], which only
 * accepts '.'. Display-only text can use the device locale.
 */
fun Double.toInputString(decimals: Int): String =
    String.format(Locale.US, "%.${decimals}f", this)
