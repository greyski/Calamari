package com.okmoto.calamari.calendar

import com.okmoto.calamari.audio.CalamariIntent
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object CalendarUtil {

    /**
     * Grabs the latest month and day based on the current time.
     */
    fun getMonthAndDateToday(): Pair<String, String> {
        val cal = Calendar.getInstance()
        val monthFmt = SimpleDateFormat("MMM", Locale.getDefault())
        val month = monthFmt.format(cal.time).uppercase(Locale.getDefault())
        val day = cal.get(Calendar.DAY_OF_MONTH).toString()
        return Pair(month, day)
    }

    /**
     * Derives concrete start/end times and all-day flag for a calendar event
     * from a parsed [CalamariIntent].
     *
     * @param intent Parsed Rhino intent describing when the event should occur.
     * @return Triple of (startMillis, endMillis, allDay) in the device's local
     * timezone, or null if the intent cannot be mapped to a valid time range.
     */
    fun buildEventTimesFromIntent(intent: CalamariIntent): Triple<Long, Long, Boolean>? {
        val now = Calendar.getInstance()

        return when (intent) {
            is CalamariIntent.ThisDayIntent -> {
                val cal = Calendar.getInstance().apply {
                    set(Calendar.YEAR, now.get(Calendar.YEAR))
                    set(Calendar.MONTH, now.get(Calendar.MONTH))
                    set(Calendar.DAY_OF_MONTH, now.get(Calendar.DAY_OF_MONTH))
                    set(Calendar.HOUR_OF_DAY, resolveHour24(intent.hour, intent.dayNight))
                    set(Calendar.MINUTE, intent.minute?.number ?: 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                ensureFutureWhenDayNightNull(cal, now, intent.hour, intent.dayNight)
                val start = cal.timeInMillis
                cal.add(Calendar.HOUR_OF_DAY, 1)
                val end = cal.timeInMillis
                Triple(start, end, false)
            }

            is CalamariIntent.ThisWeekIntent -> {
                val cal = Calendar.getInstance().apply {
                    timeInMillis = now.timeInMillis
                    add(
                        Calendar.DAY_OF_YEAR,
                        daysUntilNext(
                            now.get(Calendar.DAY_OF_WEEK),
                            toCalendarDayOfWeek(intent.day),
                        ),
                    )
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }

                val hasTime = intent.hour != null
                if (hasTime) {
                    setTimeOfDay(cal, intent.hour, intent.minute, intent.dayNight)
                    ensureFutureWhenDayNightNull(cal, now, intent.hour, intent.dayNight)
                    val start = cal.timeInMillis
                    cal.add(Calendar.HOUR_OF_DAY, 1)
                    val end = cal.timeInMillis
                    Triple(start, end, false)
                } else {
                    setAllDay(cal)
                }
            }

            is CalamariIntent.ThisMonthIntent -> {
                val cal = Calendar.getInstance().apply {
                    set(Calendar.YEAR, now.get(Calendar.YEAR))
                    set(Calendar.MONTH, now.get(Calendar.MONTH))
                    set(Calendar.DAY_OF_MONTH, intent.date.number)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }

                if (cal.timeInMillis <= now.timeInMillis) {
                    cal.add(Calendar.MONTH, 1)
                }

                val hasTime = intent.hour != null
                if (hasTime) {
                    setTimeOfDay(cal, intent.hour, intent.minute, intent.dayNight)
                    ensureFutureWhenDayNightNull(cal, now, intent.hour, intent.dayNight)
                    val start = cal.timeInMillis
                    cal.add(Calendar.HOUR_OF_DAY, 1)
                    val end = cal.timeInMillis
                    Triple(start, end, false)
                } else {
                    setAllDay(cal)
                }
            }

            is CalamariIntent.ThisYearIntent -> {
                val cal = Calendar.getInstance().apply {
                    set(Calendar.YEAR, now.get(Calendar.YEAR))
                    set(Calendar.MONTH, intent.month.number - 1)
                    set(Calendar.DAY_OF_MONTH, intent.date.number)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }

                if (cal.timeInMillis <= now.timeInMillis) {
                    cal.add(Calendar.YEAR, 1)
                }

                val hasTime = intent.hour != null
                if (hasTime) {
                    setTimeOfDay(cal, intent.hour, intent.minute, intent.dayNight)
                    ensureFutureWhenDayNightNull(cal, now, intent.hour, intent.dayNight)
                    val start = cal.timeInMillis
                    cal.add(Calendar.HOUR_OF_DAY, 1)
                    val end = cal.timeInMillis
                    Triple(start, end, false)
                } else {
                    setAllDay(cal)
                }
            }
        }
    }

    /**
     * Computes how many days ahead the next [targetDow] occurs relative to
     * [todayDow], treating both as [Calendar.DAY_OF_WEEK] constants.
     */
    private fun daysUntilNext(todayDow: Int, targetDow: Int): Int {
        val diff = (targetDow - todayDow + 7) % 7
        return if (diff == 0) 7 else diff
    }

    /**
     * Maps a [CalamariIntent.Day] to the corresponding [Calendar.DAY_OF_WEEK]
     * constant.
     */
    private fun toCalendarDayOfWeek(day: CalamariIntent.Day): Int {
        return when (day) {
            CalamariIntent.Day.MONDAY -> Calendar.MONDAY
            CalamariIntent.Day.TUESDAY -> Calendar.TUESDAY
            CalamariIntent.Day.WEDNESDAY -> Calendar.WEDNESDAY
            CalamariIntent.Day.THURSDAY -> Calendar.THURSDAY
            CalamariIntent.Day.FRIDAY -> Calendar.FRIDAY
            CalamariIntent.Day.SATURDAY -> Calendar.SATURDAY
            CalamariIntent.Day.SUNDAY -> Calendar.SUNDAY
        }
    }

    private fun setTimeOfDay(
        cal: Calendar,
        hour: CalamariIntent.Hour,
        minutes: CalamariIntent.Minutes?,
        dayNight: CalamariIntent.DayNight?,
    ) {
        cal.set(Calendar.HOUR_OF_DAY, resolveHour24(hour, dayNight))
        cal.set(Calendar.MINUTE, minutes?.number ?: 0)
    }

    /**
     * When [dayNight] is null, the intent hour was resolved as AM. If that time is in the past,
     * try the same day at PM; if still in the past, use the next day at AM so we never create events in the past.
     */
    private fun ensureFutureWhenDayNightNull(
        cal: Calendar,
        now: Calendar,
        hour: CalamariIntent.Hour,
        dayNight: CalamariIntent.DayNight?,
    ) {
        if (dayNight != null) return
        if (cal.timeInMillis > now.timeInMillis) return
        val base = hour.number
        val pmHour24 = if (base in 1..11) base + 12 else base
        cal.set(Calendar.HOUR_OF_DAY, pmHour24)
        if (cal.timeInMillis > now.timeInMillis) return
        cal.add(Calendar.DAY_OF_YEAR, 1)
        cal.set(Calendar.HOUR_OF_DAY, if (base == 12) 0 else base)
    }

    private fun resolveHour24(
        hour: CalamariIntent.Hour,
        dayNight: CalamariIntent.DayNight?,
    ): Int {
        val base = hour.number
        return when (dayNight) {
            null -> base
            else -> {
                if (dayNight.isMorning) {
                    if (base == 12) 0 else base
                } else {
                    if (base in 1..11) base + 12 else base
                }
            }
        }
    }

    private fun setAllDay(cal: Calendar): Triple<Long, Long, Boolean> {
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        val start = cal.timeInMillis
        cal.add(Calendar.DAY_OF_YEAR, 1)
        val end = cal.timeInMillis
        return Triple(start, end, true)
    }

    /**
     * Formats a human-friendly day label for a given start time, such as
     * "Today", "Tomorrow", or "Friday the 13th, Feb."
     *
     * @param startMillis Event start time in epoch milliseconds (local time).
     */
    fun formatDayLabel(startMillis: Long): String {
        val nowCal = Calendar.getInstance()
        val eventCal = Calendar.getInstance().apply { timeInMillis = startMillis }

        fun ymd(cal: Calendar) = Triple(
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH),
        )

        val (yNow, mNow, dNow) = ymd(nowCal)
        val (yEvt, mEvt, dEvt) = ymd(eventCal)

        if (yEvt == yNow && mEvt == mNow && dEvt == dNow) return "Today"

        val tomorrowCal = (nowCal.clone() as Calendar).apply {
            add(Calendar.DAY_OF_YEAR, 1)
        }
        val (yTom, mTom, dTom) = ymd(tomorrowCal)
        if (yEvt == yTom && mEvt == mTom && dEvt == dTom) return "Tomorrow"

        val weekday = SimpleDateFormat("EEEE", Locale.getDefault()).format(eventCal.time)
        val month = SimpleDateFormat("MMM", Locale.getDefault()).format(eventCal.time)
        val day = dEvt
        return "$weekday the ${ordinal(day)}, $month."
    }

    /**
     * Formats a human-friendly time label for a given start time, such as
     * "4:00pm", using the device's locale and 12-hour clock.
     *
     * @param startMillis Event start time in epoch milliseconds (local time).
     */
    fun formatTimeLabel(startMillis: Long): String {
        val fmt = SimpleDateFormat("h:mma", Locale.getDefault())
        return fmt.format(Date(startMillis)).lowercase()
    }

    /**
     * Formats an integer day-of-month as an ordinal string ("1st", "2nd", etc).
     */
    private fun ordinal(day: Int): String = when {
        day in 11..13 -> "${day}th"
        day % 10 == 1 -> "${day}st"
        day % 10 == 2 -> "${day}nd"
        day % 10 == 3 -> "${day}rd"
        else -> "${day}th"
    }
}

