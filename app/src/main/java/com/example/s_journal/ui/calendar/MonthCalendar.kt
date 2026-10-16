package com.example.s_journal.ui.calendar

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.example.s_journal.R
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.s_journal.ui.components.CalendarDayCell
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun MonthCalendar(
    currentMonth: YearMonth,
    selectedDate: LocalDate,
    datesWithEntries: Set<LocalDate>,
    onDateSelected: (LocalDate) -> Unit,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onGoToToday: () -> Unit,
    modifier: Modifier = Modifier,
    locale: Locale = Locale.getDefault()
) {
    val monthTitleFormatter = remember(locale) {
        DateTimeFormatter.ofPattern("MMMM yyyy", locale)
    }

    val daysOfWeek = remember(locale) {
        val firstDay = java.time.temporal.WeekFields.of(locale).firstDayOfWeek
        (0L..6L).map { firstDay.plus(it) }
    }

    var totalDragX by remember { mutableFloatStateOf(0f) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .pointerInput(currentMonth) {
                detectHorizontalDragGestures(
                    onDragEnd = {
                        if (totalDragX < -60f) {
                            onNextMonth()
                        } else if (totalDragX > 60f) {
                            onPreviousMonth()
                        }
                        totalDragX = 0f
                    },
                    onDragCancel = {
                        totalDragX = 0f
                    },
                    onHorizontalDrag = { change, dragAmount ->
                        change.consume()
                        totalDragX += dragAmount
                    }
                )
            }
    ) {
        // Month Navigation Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = currentMonth.atDay(1).format(monthTitleFormatter).replaceFirstChar {
                    if (it.isLowerCase()) it.titlecase(locale) else it.toString()
                },
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                ),
                color = MaterialTheme.colorScheme.onSurface
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Today button
                val isCurrentMonthToday = YearMonth.now() == currentMonth
                if (!isCurrentMonthToday || selectedDate != LocalDate.now()) {
                    IconButton(
                        onClick = onGoToToday,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Today,
                            contentDescription = stringResource(R.string.calendar_today),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                IconButton(
                    onClick = onPreviousMonth,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = stringResource(R.string.calendar_previous_month),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                    )
                }

                IconButton(
                    onClick = onNextMonth,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = stringResource(R.string.calendar_next_month),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        // Days of week header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            daysOfWeek.forEach { dayOfWeek ->
                val dayName = dayOfWeek.getDisplayName(TextStyle.SHORT, locale)
                    .take(2)
                    .uppercase(locale)
                Text(
                    text = dayName,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Animated Calendar Grid with directional slide & fade transition
        AnimatedContent(
            targetState = currentMonth,
            transitionSpec = {
                if (targetState > initialState) {
                    (slideInHorizontally { width -> width / 2 } + fadeIn()).togetherWith(
                        slideOutHorizontally { width -> -width / 2 } + fadeOut()
                    )
                } else {
                    (slideInHorizontally { width -> -width / 2 } + fadeIn()).togetherWith(
                        slideOutHorizontally { width -> width / 2 } + fadeOut()
                    )
                }
            },
            label = "MonthTransition"
        ) { targetMonth ->
            val days = remember(targetMonth, locale) { calculateCalendarGrid(targetMonth, locale) }
            val weeks = days.chunked(7)

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                weeks.forEach { week ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        week.forEach { date ->
                            Box(
                                modifier = Modifier.weight(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                CalendarDayCell(
                                    date = date,
                                    isSelected = date == selectedDate,
                                    isToday = date == LocalDate.now(),
                                    hasEntry = datesWithEntries.contains(date),
                                    isCurrentMonth = YearMonth.from(date) == targetMonth,
                                    onDateClick = onDateSelected
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun calculateCalendarGrid(yearMonth: YearMonth, locale: Locale): List<LocalDate> {
    val firstDayOfMonth = yearMonth.atDay(1)
    val lastDayOfMonth = yearMonth.atEndOfMonth()

    val firstDayOfWeek = java.time.temporal.WeekFields.of(locale).firstDayOfWeek
    val leadingDaysCount = (firstDayOfMonth.dayOfWeek.value - firstDayOfWeek.value + 7) % 7

    val days = mutableListOf<LocalDate>()

    val previousMonth = yearMonth.minusMonths(1)
    val lastDayOfPreviousMonth = previousMonth.atEndOfMonth().dayOfMonth
    for (i in (lastDayOfPreviousMonth - leadingDaysCount + 1)..lastDayOfPreviousMonth) {
        days.add(previousMonth.atDay(i))
    }

    for (day in 1..lastDayOfMonth.dayOfMonth) {
        days.add(yearMonth.atDay(day))
    }

    val remainingDays = (7 - (days.size % 7)) % 7
    val nextMonth = yearMonth.plusMonths(1)
    for (day in 1..remainingDays) {
        days.add(nextMonth.atDay(day))
    }

    return days
}
