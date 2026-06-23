package com.example.healthapp.ui.components

import android.app.DatePickerDialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.healthapp.ui.theme.OnSurfaceColor
import com.example.healthapp.ui.theme.OnSurfaceVariant
import com.example.healthapp.ui.theme.PrimaryBlue
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun DateSelector(
    selectedDateStartMillis: Long,
    selectedRange: HeartRateRange,
    onDateSelected: (Long) -> Unit
) {
    val context = LocalContext.current

    if (selectedRange == HeartRateRange.MONTH) {
        val monthStart = startOfMonth(selectedDateStartMillis)
        val monthLabel = formatMonthLabel(monthStart)

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            IconButton(onClick = { onDateSelected(addMonths(monthStart, -1)) }) {
                Icon(Icons.Default.ChevronLeft, "Previous month", tint = PrimaryBlue)
            }

            Text(
                text = monthLabel,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold,
                color = OnSurfaceColor
            )

            IconButton(onClick = { onDateSelected(addMonths(monthStart, 1)) }) {
                Icon(Icons.Default.ChevronRight, "Next month", tint = PrimaryBlue)
            }

            Surface(
                modifier = Modifier.size(48.dp),
                shape = RoundedCornerShape(16.dp),
                color = PrimaryBlue.copy(alpha = 0.1f)
            ) {
                IconButton(
                    onClick = {
                        val calendar = Calendar.getInstance()
                        calendar.timeInMillis = monthStart

                        DatePickerDialog(
                            context,
                            { _, year, month, dayOfMonth ->
                                val selectedCalendar = Calendar.getInstance()
                                selectedCalendar.set(year, month, dayOfMonth, 0, 0, 0)
                                selectedCalendar.set(Calendar.MILLISECOND, 0)
                                onDateSelected(startOfMonth(selectedCalendar.timeInMillis))
                            },
                            calendar.get(Calendar.YEAR),
                            calendar.get(Calendar.MONTH),
                            calendar.get(Calendar.DAY_OF_MONTH)
                        ).show()
                    }
                ) {
                    Icon(Icons.Default.CalendarMonth, "Calendar", tint = PrimaryBlue)
                }
            }
        }

        return
    }

    if (selectedRange == HeartRateRange.WEEK) {
        val weekStart = startOfWeek(selectedDateStartMillis)
        val weekRangeText = formatWeekRange(weekStart)

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            IconButton(onClick = { onDateSelected(addDays(weekStart, -7)) }) {
                Icon(Icons.Default.ChevronLeft, "Previous week", tint = PrimaryBlue)
            }

            Text(
                text = weekRangeText,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold,
                color = OnSurfaceColor
            )

            IconButton(onClick = { onDateSelected(addDays(weekStart, 7)) }) {
                Icon(Icons.Default.ChevronRight, "Next week", tint = PrimaryBlue)
            }

            Surface(
                modifier = Modifier.size(48.dp),
                shape = RoundedCornerShape(16.dp),
                color = PrimaryBlue.copy(alpha = 0.1f)
            ) {
                IconButton(
                    onClick = {
                        val calendar = Calendar.getInstance()
                        calendar.timeInMillis = weekStart

                        DatePickerDialog(
                            context,
                            { _, year, month, dayOfMonth ->
                                val selectedCalendar = Calendar.getInstance()
                                selectedCalendar.set(year, month, dayOfMonth, 0, 0, 0)
                                selectedCalendar.set(Calendar.MILLISECOND, 0)
                                onDateSelected(startOfWeek(selectedCalendar.timeInMillis))
                            },
                            calendar.get(Calendar.YEAR),
                            calendar.get(Calendar.MONTH),
                            calendar.get(Calendar.DAY_OF_MONTH)
                        ).show()
                    }
                ) {
                    Icon(Icons.Default.CalendarMonth, "Calendar", tint = PrimaryBlue)
                }
            }
        }

        return
    }

    val dateItems = remember(selectedDateStartMillis) {
        (-2..2).map { offset ->
            val dayStart = addDays(selectedDateStartMillis, offset)
            DateSelectorItem(
                dayStartMillis = dayStart,
                label = if (isToday(dayStart)) "Hôm nay" else vietnameseWeekday(dayStart),
                dayNumber = SimpleDateFormat("dd", Locale.getDefault()).format(Date(dayStart)),
                isSelected = dayStart == selectedDateStartMillis
            )
        }
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        LazyRow(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(dateItems) { item ->
                Column(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { onDateSelected(item.dayStartMillis) }
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        item.label.uppercase(),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (item.isSelected) PrimaryBlue else OnSurfaceVariant
                    )

                    Spacer(Modifier.height(8.dp))

                    Surface(
                        modifier = Modifier.size(40.dp),
                        shape = CircleShape,
                        color = if (item.isSelected) PrimaryBlue else androidx.compose.ui.graphics.Color.Transparent,
                        shadowElevation = if (item.isSelected) 4.dp else 0.dp
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                item.dayNumber,
                                color = if (item.isSelected) androidx.compose.ui.graphics.Color.White else OnSurfaceColor,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }

        Surface(
            modifier = Modifier.size(48.dp),
            shape = RoundedCornerShape(16.dp),
            color = PrimaryBlue.copy(alpha = 0.1f)
        ) {
            IconButton(
                onClick = {
                    val calendar = Calendar.getInstance()
                    calendar.timeInMillis = selectedDateStartMillis

                    DatePickerDialog(
                        context,
                        { _, year, month, dayOfMonth ->
                            val selectedCalendar = Calendar.getInstance()
                            selectedCalendar.set(year, month, dayOfMonth, 0, 0, 0)
                            selectedCalendar.set(Calendar.MILLISECOND, 0)
                            onDateSelected(selectedCalendar.timeInMillis)
                        },
                        calendar.get(Calendar.YEAR),
                        calendar.get(Calendar.MONTH),
                        calendar.get(Calendar.DAY_OF_MONTH)
                    ).show()
                }
            ) {
                Icon(Icons.Default.CalendarMonth, "Calendar", tint = PrimaryBlue)
            }
        }
    }
}

private data class DateSelectorItem(
    val dayStartMillis: Long,
    val label: String,
    val dayNumber: String,
    val isSelected: Boolean
)

internal fun startOfWeek(timeMillis: Long): Long {
    if (timeMillis <= 0L) return 0L

    val calendar = Calendar.getInstance()
    calendar.firstDayOfWeek = Calendar.MONDAY
    calendar.timeInMillis = timeMillis
    calendar.set(Calendar.HOUR_OF_DAY, 0)
    calendar.set(Calendar.MINUTE, 0)
    calendar.set(Calendar.SECOND, 0)
    calendar.set(Calendar.MILLISECOND, 0)

    while (calendar.get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY) {
        calendar.add(Calendar.DAY_OF_MONTH, -1)
    }

    return calendar.timeInMillis
}

internal fun startOfMonth(timeMillis: Long): Long {
    if (timeMillis <= 0L) return 0L

    val calendar = Calendar.getInstance()
    calendar.timeInMillis = timeMillis
    calendar.set(Calendar.DAY_OF_MONTH, 1)
    calendar.set(Calendar.HOUR_OF_DAY, 0)
    calendar.set(Calendar.MINUTE, 0)
    calendar.set(Calendar.SECOND, 0)
    calendar.set(Calendar.MILLISECOND, 0)
    return calendar.timeInMillis
}

internal fun daysInMonth(timeMillis: Long): Int {
    if (timeMillis <= 0L) return 0

    val calendar = Calendar.getInstance()
    calendar.timeInMillis = timeMillis
    return calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
}

internal fun addDays(timeMillis: Long, days: Int): Long {
    val calendar = Calendar.getInstance()
    calendar.timeInMillis = timeMillis
    calendar.add(Calendar.DAY_OF_MONTH, days)
    calendar.set(Calendar.HOUR_OF_DAY, 0)
    calendar.set(Calendar.MINUTE, 0)
    calendar.set(Calendar.SECOND, 0)
    calendar.set(Calendar.MILLISECOND, 0)
    return calendar.timeInMillis
}

internal fun addMonths(timeMillis: Long, months: Int): Long {
    if (timeMillis <= 0L) return 0L

    val calendar = Calendar.getInstance()
    calendar.timeInMillis = timeMillis
    calendar.add(Calendar.MONTH, months)
    calendar.set(Calendar.DAY_OF_MONTH, 1)
    calendar.set(Calendar.HOUR_OF_DAY, 0)
    calendar.set(Calendar.MINUTE, 0)
    calendar.set(Calendar.SECOND, 0)
    calendar.set(Calendar.MILLISECOND, 0)
    return calendar.timeInMillis
}

internal fun isToday(timeMillis: Long): Boolean {
    val today = Calendar.getInstance()
    val target = Calendar.getInstance()
    target.timeInMillis = timeMillis

    return today.get(Calendar.YEAR) == target.get(Calendar.YEAR) &&
            today.get(Calendar.DAY_OF_YEAR) == target.get(Calendar.DAY_OF_YEAR)
}

internal fun vietnameseWeekday(timeMillis: Long): String {
    val calendar = Calendar.getInstance()
    calendar.timeInMillis = timeMillis

    return when (calendar.get(Calendar.DAY_OF_WEEK)) {
        Calendar.MONDAY -> "Thứ 2"
        Calendar.TUESDAY -> "Thứ 3"
        Calendar.WEDNESDAY -> "Thứ 4"
        Calendar.THURSDAY -> "Thứ 5"
        Calendar.FRIDAY -> "Thứ 6"
        Calendar.SATURDAY -> "Thứ 7"
        else -> "CN"
    }
}

internal fun formatWeekRange(weekStartMillis: Long): String {
    if (weekStartMillis <= 0L) return "--"

    val formatter = SimpleDateFormat("dd/MM", Locale.getDefault())
    val start = formatter.format(Date(weekStartMillis))
    val end = formatter.format(Date(weekStartMillis + 6 * 24 * 60 * 60 * 1000))

    return "$start - $end"
}

internal fun formatMonthLabel(monthStartMillis: Long): String {
    if (monthStartMillis <= 0L) return "--"

    val formatter = SimpleDateFormat("MM/yyyy", Locale.getDefault())
    return formatter.format(Date(monthStartMillis))
}
