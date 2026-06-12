package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

@Composable
fun DonutChart(
    portions: List<Portion>,
    modifier: Modifier = Modifier,
    centerContent: @Composable BoxScope.() -> Unit = {}
) {
    val totalVal = portions.sumOf { it.value }.toFloat()
    val animatedProgress = remember { Animatable(0f) }

    LaunchedEffect(portions) {
        animatedProgress.snapTo(0f)
        animatedProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 800)
        )
    }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = 14.dp.toPx()
            if (totalVal == 0f) {
                // Draw default dark empty ring
                drawArc(
                    color = Color(0xFF25252D),
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    style = Stroke(width = stroke, cap = StrokeCap.Round)
                )
            } else {
                var currentAngle = -90f
                portions.forEach { portion ->
                    val sweepAngle = (portion.value.toFloat() / totalVal) * 360f * animatedProgress.value
                    if (sweepAngle > 0f) {
                        drawArc(
                            color = portion.color,
                            startAngle = currentAngle,
                            sweepAngle = sweepAngle,
                            useCenter = false,
                            style = Stroke(width = stroke, cap = StrokeCap.Round)
                        )
                        currentAngle += sweepAngle
                    }
                }
            }
        }
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
            content = centerContent
        )
    }
}

data class Portion(
    val categoryName: String,
    val value: Double,
    val color: Color
)
