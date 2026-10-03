package app.habivance.ui.components

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import java.time.LocalTime
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.math.sin

private val AMBER = Color(0xFFF59E0B)
private const val DIAL_SIZE_DP = 200
private const val DIGIT_H_DP = 56
private const val DIGIT_W_DP = 34
private const val DIGIT_SIZE_SP = 48f
private const val DRAG_SENSITIVITY = 0.012f

@Composable
fun TimePickerDialog(
    initialHour: Int,
    initialMinute: Int,
    onDismiss: () -> Unit,
    onConfirm: (hour: Int, minute: Int) -> Unit,
    title: String = "Set reminder time",
) {
    var hour by remember { mutableStateOf(initialHour.coerceIn(0, 23)) }
    var minute by remember { mutableStateOf(initialMinute.coerceIn(0, 59)) }
    var activeHand by remember { mutableStateOf(ActiveHand.MINUTE) }

    val haptic = LocalHapticFeedback.current

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.widthIn(min = 320.dp, max = 360.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 20.dp)
                )

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    Color.Transparent,
                                    AMBER.copy(alpha = 0.06f),
                                    AMBER.copy(alpha = 0.06f),
                                    Color.Transparent
                                )
                            )
                        )
                        .padding(vertical = 4.dp, horizontal = 12.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        DraggablePair(
                            value = hour,
                            onValueChange = { hour = it.coerceIn(0, 23) },
                            haptic = haptic,
                            minValue = 0,
                            maxValue = 23
                        )
                        Spacer(Modifier.width(6.dp))
                        AmpmToggle(
                            isPm = hour >= 12,
                            onSelectAm = {
                                if (hour >= 12) hour -= 12
                            },
                            onSelectPm = {
                                if (hour < 12) hour += 12
                            }
                        )
                        Spacer(Modifier.width(6.dp))
                        DraggablePair(
                            value = minute,
                            onValueChange = { minute = it.coerceIn(0, 59) },
                            haptic = haptic,
                            minValue = 0,
                            maxValue = 59
                        )
                    }
                }

                Spacer(Modifier.height(20.dp))

                AnalogDial(
                    hour = hour,
                    minute = minute,
                    activeHand = activeHand,
                    onHourChange = { hour = it },
                    onMinuteChange = {
                        if (it != minute) {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        }
                        minute = it
                    },
                    onActiveHandChange = { activeHand = it }
                )

                Spacer(Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = { onConfirm(hour, minute) },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AMBER,
                            contentColor = Color(0xFF0B0C0E)
                        )
                    ) {
                        Text("OK", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
private fun AmpmToggle(
    isPm: Boolean,
    onSelectAm: () -> Unit,
    onSelectPm: () -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(3.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(horizontal = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(
                    if (!isPm) AMBER.copy(alpha = 0.25f)
                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                )
                .clickable(onClick = onSelectAm)
                .padding(horizontal = 10.dp, vertical = 5.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "AM",
                fontSize = 15.sp,
                fontWeight = if (!isPm) FontWeight.Bold else FontWeight.Medium,
                color = if (!isPm) AMBER else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(
                    if (isPm) AMBER.copy(alpha = 0.25f)
                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                )
                .clickable(onClick = onSelectPm)
                .padding(horizontal = 10.dp, vertical = 5.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "PM",
                fontSize = 15.sp,
                fontWeight = if (isPm) FontWeight.Bold else FontWeight.Medium,
                color = if (isPm) AMBER else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun DraggablePair(
    value: Int,
    onValueChange: (Int) -> Unit,
    haptic: HapticFeedback,
    minValue: Int,
    maxValue: Int,
) {
    val onSurface = MaterialTheme.colorScheme.onSurface
    val surface = MaterialTheme.colorScheme.surface
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val digitHeightPx = with(density) { DIGIT_H_DP.dp.toPx() }
    val digitSizePx = with(density) { DIGIT_SIZE_SP.sp.toPx() }
    val totalHeightPx = digitHeightPx * 3f
    val range = maxValue - minValue + 1

    val currentValue by rememberUpdatedState(value)
    val currentOnChange by rememberUpdatedState(onValueChange)

    // Continuous visual offset in "digit units". Drawn center shifts by this * digitHeightPx.
    var visualOffsetUnits by remember { mutableFloatStateOf(0f) }
    // Value that is currently drawn as "center". Differs from `value` between commits.
    var drawingValue by remember { mutableStateOf(value) }
    val settleAnim = remember { Animatable(0f) }
    var isDragging by remember { mutableStateOf(false) }

    // When value changes externally (analog dial, etc.) and we're not dragging,
    // animate a smooth slide from the previous digit to the new one.
    LaunchedEffect(value) {
        if (isDragging) {
            drawingValue = value
            visualOffsetUnits = 0f
            return@LaunchedEffect
        }
        if (value != drawingValue) {
            val oldIdx = drawingValue
            val newIdx = value
            var rawDiff = (newIdx - oldIdx)
            // wrap to shortest path within [minValue..maxValue]
            if (rawDiff > range / 2) rawDiff -= range
            if (rawDiff < -range / 2) rawDiff += range
            settleAnim.snapTo(-rawDiff.toFloat())
            settleAnim.animateTo(0f, tween(durationMillis = 220))
            drawingValue = value
        }
    }

    val totalOffsetUnits = visualOffsetUnits + settleAnim.value

    Box(
        modifier = Modifier
            .width((DIGIT_W_DP * 2).dp)
            .height((DIGIT_H_DP * 3).dp)
            .pointerInput(minValue, maxValue) {
                detectVerticalDragGestures(
                    onDragStart = {
                        isDragging = true
                        visualOffsetUnits = 0f
                        drawingValue = currentValue
                        scope.launch { settleAnim.snapTo(0f) }
                    },
                    onVerticalDrag = { change, dragAmount ->
                        change.consume()
                        // Convert finger movement to digit units and add to visual offset.
                        visualOffsetUnits += dragAmount / digitHeightPx

                        // Whenever the offset crosses a full unit, commit the change
                        // and roll the offset back by exactly one unit so continuity is preserved.
                        while (visualOffsetUnits <= -1f) {
                            visualOffsetUnits += 1f
                            val next = if (drawingValue + 1 > maxValue) minValue else drawingValue + 1
                            if (next != drawingValue) {
                                drawingValue = next
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                currentOnChange(next)
                            }
                        }
                        while (visualOffsetUnits >= 1f) {
                            visualOffsetUnits -= 1f
                            val next = if (drawingValue - 1 < minValue) maxValue else drawingValue - 1
                            if (next != drawingValue) {
                                drawingValue = next
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                currentOnChange(next)
                            }
                        }
                    },
                    onDragEnd = {
                        isDragging = false
                        // Animate any leftover fractional offset back to 0.
                        val leftover = visualOffsetUnits
                        visualOffsetUnits = 0f
                        scope.launch {
                            settleAnim.snapTo(leftover)
                            settleAnim.animateTo(0f, tween(durationMillis = 180))
                        }
                    },
                    onDragCancel = {
                        isDragging = false
                        visualOffsetUnits = 0f
                        scope.launch { settleAnim.snapTo(0f) }
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val textPaint = Paint().apply {
                isAntiAlias = true
                color = onSurface.toArgb()
                textSize = digitSizePx
                textAlign = Paint.Align.CENTER
                typeface = Typeface.MONOSPACE
                letterSpacing = 0f
            }
            val fadePaint = Paint().apply {
                isAntiAlias = true
                color = onSurface.toArgb()
                textSize = digitSizePx
                textAlign = Paint.Align.CENTER
                typeface = Typeface.MONOSPACE
                alpha = 90
            }

            val cx = size.width / 2f
            // Draw the digits shifted by the continuous visual offset.
            val cy = totalHeightPx / 2f + totalOffsetUnits * digitHeightPx
            val baselineOffset = digitSizePx * 0.35f

            fun wrap(v: Int): Int {
                var x = (v - minValue) % range
                if (x < 0) x += range
                return x + minValue
            }

            val center = drawingValue
            val above1 = wrap(center - 1)
            val above2 = wrap(center - 2)
            val below1 = wrap(center + 1)
            val below2 = wrap(center + 2)

            drawIntoCanvas { c ->
                c.nativeCanvas.drawText("%02d".format(above2), cx, cy - 2f * digitHeightPx + baselineOffset, fadePaint)
                c.nativeCanvas.drawText("%02d".format(above1), cx, cy - digitHeightPx + baselineOffset, fadePaint)
                c.nativeCanvas.drawText("%02d".format(center), cx, cy + baselineOffset, textPaint)
                c.nativeCanvas.drawText("%02d".format(below1), cx, cy + digitHeightPx + baselineOffset, fadePaint)
                c.nativeCanvas.drawText("%02d".format(below2), cx, cy + 2f * digitHeightPx + baselineOffset, fadePaint)
            }

            drawRect(
                brush = Brush.verticalGradient(
                    0f to surface,
                    0.22f to Color.Transparent,
                    0.78f to Color.Transparent,
                    1f to surface
                )
            )
        }
    }
}

private enum class ActiveHand { HOUR, MINUTE }

@Composable
private fun AnalogDial(
    hour: Int,
    minute: Int,
    activeHand: ActiveHand,
    onHourChange: (Int) -> Unit,
    onMinuteChange: (Int) -> Unit,
    onActiveHandChange: (ActiveHand) -> Unit,
) {
    val onSurface = MaterialTheme.colorScheme.onSurface
    val outline = MaterialTheme.colorScheme.outlineVariant

    // Read latest values / callbacks inside pointerInput without re-keying the gesture.
    val latestHour by rememberUpdatedState(hour)
    val latestMinute by rememberUpdatedState(minute)
    val latestOnHourChange by rememberUpdatedState(onHourChange)
    val latestOnMinuteChange by rememberUpdatedState(onMinuteChange)
    val latestOnActiveHandChange by rememberUpdatedState(onActiveHandChange)

    // Opening animation only. After it plays, angles are derived directly from hour/minute.
    var hasPlayedOpening by remember { mutableStateOf(false) }
    val hourAngleAnim = remember { Animatable(0f) }
    val minuteAngleAnim = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        val now = LocalTime.now()
        val startHourAngle = ((now.hour % 12) + now.minute / 60f) * 30f
        val startMinuteAngle = now.minute * 6f
        val targetHourAngle = ((latestHour % 12) + latestMinute / 60f) * 30f
        val targetMinuteAngle = latestMinute * 6f
        hourAngleAnim.snapTo(startHourAngle)
        minuteAngleAnim.snapTo(startMinuteAngle)
        coroutineScope {
            val h = async { hourAngleAnim.animateTo(targetHourAngle, tween(300)) }
            val m = async { minuteAngleAnim.animateTo(targetMinuteAngle, tween(300)) }
            h.await(); m.await()
        }
        hasPlayedOpening = true
    }

    val drawnHourAngle = if (hasPlayedOpening) {
        ((hour % 12) + minute / 60f) * 30f
    } else hourAngleAnim.value
    val drawnMinuteAngle = if (hasPlayedOpening) minute * 6f else minuteAngleAnim.value

    Box(
        modifier = Modifier
            .size(DIAL_SIZE_DP.dp)
            .pointerInput(Unit) {
                if (!hasPlayedOpening) return@pointerInput

                var lockedHand: ActiveHand? = null

                detectDragGestures(
                    onDragStart = { start ->
                        val cx = size.width / 2f
                        val cy = size.height / 2f
                        val r = minOf(size.width, size.height) / 2f - 4.dp.toPx()

                        val hAngle = ((latestHour % 12) + latestMinute / 60f) * 30f
                        val mAngle = latestMinute * 6f

                        val hRad = Math.toRadians((hAngle - 90f).toDouble())
                        val mRad = Math.toRadians((mAngle - 90f).toDouble())

                        val hourTip = Offset(
                            cx + r * 0.5f * cos(hRad).toFloat(),
                            cy + r * 0.5f * sin(hRad).toFloat()
                        )
                        val minTip = Offset(
                            cx + r * 0.78f * cos(mRad).toFloat(),
                            cy + r * 0.78f * sin(mRad).toFloat()
                        )

                        val dHour = distancePointToSegment(start, Offset(cx, cy), hourTip)
                        val dMin = distancePointToSegment(start, Offset(cx, cy), minTip)

                        lockedHand = if (dHour < dMin) ActiveHand.HOUR else ActiveHand.MINUTE
                        latestOnActiveHandChange(lockedHand!!)
                    },
                    onDrag = { change, _ ->
                        val locked = lockedHand ?: return@detectDragGestures
                        val cx = size.width / 2f
                        val cy = size.height / 2f
                        val dx = change.position.x - cx
                        val dy = change.position.y - cy
                        val angleDeg = (Math.toDegrees(atan2(dx.toDouble(), -dy.toDouble())) + 360.0) % 360.0

                        if (locked == ActiveHand.MINUTE) {
                            val newMin = ((angleDeg / 6.0).roundToInt() % 60 + 60) % 60
                            latestOnMinuteChange(newMin)
                        } else {
                            val newHour12 = ((angleDeg / 30.0).roundToInt() % 12 + 12) % 12
                            val candidates = listOf(newHour12, newHour12 + 12)
                            val newHour = candidates.minByOrNull { abs(it - latestHour) } ?: latestHour
                            latestOnHourChange(newHour.coerceIn(0, 23))
                        }
                        change.consume()
                    },
                    onDragEnd = {
                        lockedHand = null
                    },
                    onDragCancel = {
                        lockedHand = null
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(DIAL_SIZE_DP.dp)) {
            val radius = size.minDimension / 2f
            val center = Offset(size.width / 2f, size.height / 2f)
            val ringRadius = radius - 4.dp.toPx()

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(onSurface.copy(alpha = 0.05f), Color.Transparent),
                    center = center,
                    radius = ringRadius
                ),
                radius = ringRadius,
                center = center
            )

            drawCircle(
                color = outline,
                radius = ringRadius,
                center = center,
                style = Stroke(width = 1.dp.toPx())
            )

            for (i in 0 until 60) {
                val isMajor = i % 5 == 0
                if (!isMajor) {
                    val angRad = Math.toRadians((i * 6f - 90f).toDouble())
                    val c = cos(angRad).toFloat()
                    val s = sin(angRad).toFloat()
                    drawLine(
                        color = onSurface.copy(alpha = 0.18f),
                        start = center + Offset(ringRadius * 0.93f * c, ringRadius * 0.93f * s),
                        end = center + Offset(ringRadius * 0.98f * c, ringRadius * 0.98f * s),
                        strokeWidth = 1.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }
            }
            for (i in 0 until 12) {
                val angRad = Math.toRadians((i * 30f - 90f).toDouble())
                val c = cos(angRad).toFloat()
                val s = sin(angRad).toFloat()
                drawLine(
                    color = onSurface.copy(alpha = 0.55f),
                    start = center + Offset(ringRadius * 0.88f * c, ringRadius * 0.88f * s),
                    end = center + Offset(ringRadius * 0.98f * c, ringRadius * 0.98f * s),
                    strokeWidth = 2.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }

            val hourDotRad = Math.toRadians((((hour % 12) * 30f) - 90f).toDouble())
            drawCircle(
                color = AMBER,
                radius = 4.dp.toPx(),
                center = center + Offset(
                    ringRadius * 0.94f * cos(hourDotRad).toFloat(),
                    ringRadius * 0.94f * sin(hourDotRad).toFloat()
                )
            )
            val minDotRad = Math.toRadians(((minute * 6f) - 90f).toDouble())
            drawCircle(
                color = AMBER,
                radius = 3.dp.toPx(),
                center = center + Offset(
                    ringRadius * 0.94f * cos(minDotRad).toFloat(),
                    ringRadius * 0.94f * sin(minDotRad).toFloat()
                )
            )

            val hourRad = Math.toRadians((drawnHourAngle - 90f).toDouble())
            val hourC = cos(hourRad).toFloat()
            val hourS = sin(hourRad).toFloat()
            val hourTip = center + Offset(ringRadius * 0.50f * hourC, ringRadius * 0.50f * hourS)
            val hourTail = center - Offset(ringRadius * 0.12f * hourC, ringRadius * 0.12f * hourS)
            if (activeHand == ActiveHand.HOUR) {
                drawLine(AMBER.copy(alpha = 0.08f), hourTail, hourTip, strokeWidth = 11.dp.toPx(), cap = StrokeCap.Round)
                drawLine(AMBER.copy(alpha = 0.18f), hourTail, hourTip, strokeWidth = 7.dp.toPx(), cap = StrokeCap.Round)
                drawLine(AMBER, hourTail, hourTip, strokeWidth = 3.dp.toPx(), cap = StrokeCap.Round)
            } else {
                drawLine(onSurface.copy(alpha = 0.45f), hourTail, hourTip, strokeWidth = 3.dp.toPx(), cap = StrokeCap.Round)
            }

            val minRad = Math.toRadians((drawnMinuteAngle - 90f).toDouble())
            val minC = cos(minRad).toFloat()
            val minS = sin(minRad).toFloat()
            val minTip = center + Offset(ringRadius * 0.78f * minC, ringRadius * 0.78f * minS)
            val minTail = center - Offset(ringRadius * 0.14f * minC, ringRadius * 0.14f * minS)
            if (activeHand == ActiveHand.MINUTE) {
                drawLine(AMBER.copy(alpha = 0.08f), minTail, minTip, strokeWidth = 10.dp.toPx(), cap = StrokeCap.Round)
                drawLine(AMBER.copy(alpha = 0.18f), minTail, minTip, strokeWidth = 6.dp.toPx(), cap = StrokeCap.Round)
                drawLine(AMBER, minTail, minTip, strokeWidth = 2.dp.toPx(), cap = StrokeCap.Round)
            } else {
                drawLine(onSurface.copy(alpha = 0.45f), minTail, minTip, strokeWidth = 2.dp.toPx(), cap = StrokeCap.Round)
            }

            drawCircle(color = onSurface, radius = 5.dp.toPx(), center = center)
            drawCircle(color = AMBER, radius = 2.dp.toPx(), center = center)
        }

        Text(
            text = "%02d:%02d".format(hour, minute),
            style = MaterialTheme.typography.labelLarge,
            color = onSurface.copy(alpha = 0.6f),
            modifier = Modifier
                .align(Alignment.Center)
                .padding(top = 110.dp)
        )
    }
}

private fun distancePointToSegment(p: Offset, a: Offset, b: Offset): Float {
    val abx = b.x - a.x
    val aby = b.y - a.y
    val apx = p.x - a.x
    val apy = p.y - a.y
    val abLenSq = abx * abx + aby * aby
    if (abLenSq == 0f) return hypot(apx, apy)
    val t = ((apx * abx + apy * aby) / abLenSq).coerceIn(0f, 1f)
    return hypot(p.x - (a.x + t * abx), p.y - (a.y + t * aby))
}
