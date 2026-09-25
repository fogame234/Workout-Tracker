package com.workout.tracker.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class ChartDataPoint(val label: String, val value: Float)

@Composable
fun ProgressChart(
    dataPoints: List<ChartDataPoint>,
    modifier: Modifier = Modifier,
    lineColor: Color = MaterialTheme.colorScheme.primary,
    gridColor: Color = MaterialTheme.colorScheme.outlineVariant,
    labelColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    // Punched-out centre of each dot; follows the surface so it works in dark mode.
    dotFillColor: Color = MaterialTheme.colorScheme.surface,
) {
    val textMeasurer = rememberTextMeasurer()
    val labelStyle = TextStyle(fontSize = 10.sp, color = labelColor)

    if (dataPoints.isEmpty()) return

    val computed = remember(dataPoints) {
        val min = dataPoints.minOf { it.value }
        val max = dataPoints.maxOf { it.value }
        // For a single point, create a range around the value so grid lines look sensible
        val range = if (dataPoints.size == 1) (min * 0.2f).coerceAtLeast(1f)
                    else (max - min).coerceAtLeast(0.1f)
        val adjustedMin = if (dataPoints.size == 1) min - range / 2 else min
        Triple(adjustedMin, max, range)
    }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp)
            .padding(start = 44.dp, end = 12.dp, top = 12.dp, bottom = 28.dp),
    ) {
        val (minVal, _, range) = computed
        val w = size.width
        val h = size.height

        // Grid + Y labels
        for (i in 0..4) {
            val y = h - (h * i / 4)
            drawLine(gridColor, Offset(0f, y), Offset(w, y), strokeWidth = 1f)
            val v = minVal + (range * i / 4)
            val txt = if (v == v.toLong().toFloat()) v.toLong().toString() else "%.1f".format(v)
            val m = textMeasurer.measure(txt, labelStyle)
            drawText(m, topLeft = Offset(-m.size.width - 6f, y - m.size.height / 2f))
        }

        if (dataPoints.size == 1) {
            // Single point — draw centered dot with value label
            val pt = dataPoints.first()
            val center = Offset(w / 2f, h / 2f)
            drawCircle(lineColor, 7f, center)
            drawCircle(dotFillColor, 4f, center)

            val valueTxt = if (pt.value == pt.value.toLong().toFloat()) pt.value.toLong().toString()
                           else "%.1f".format(pt.value)
            val vm = textMeasurer.measure(valueTxt, labelStyle)
            drawText(vm, topLeft = Offset(center.x - vm.size.width / 2f, center.y - vm.size.height - 10f))

            val lm = textMeasurer.measure(pt.label, labelStyle)
            drawText(lm, topLeft = Offset(center.x - lm.size.width / 2f, h + 6f))
        } else {
            val stepX = w / (dataPoints.size - 1).coerceAtLeast(1)

            // Line
            val coords = dataPoints.mapIndexed { i, pt ->
                val x = i * stepX
                val y = h - ((pt.value - minVal) / range * h)
                Offset(x, y)
            }
            val path = Path().apply {
                coords.forEachIndexed { i, o -> if (i == 0) moveTo(o.x, o.y) else lineTo(o.x, o.y) }
            }
            drawPath(path, lineColor, style = Stroke(3f, cap = StrokeCap.Round, join = StrokeJoin.Round))

            // Dots
            coords.forEach { o ->
                drawCircle(lineColor, 5f, o)
                drawCircle(dotFillColor, 3f, o)
            }

            // X labels
            val indices = when {
                dataPoints.size <= 6 -> dataPoints.indices.toList()
                else -> listOf(0, dataPoints.size / 2, dataPoints.lastIndex)
            }
            indices.forEach { i ->
                val m = textMeasurer.measure(dataPoints[i].label, labelStyle)
                drawText(m, topLeft = Offset(coords[i].x - m.size.width / 2f, h + 6f))
            }
        }
    }
}
