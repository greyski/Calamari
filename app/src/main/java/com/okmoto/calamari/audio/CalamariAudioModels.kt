package com.okmoto.calamari.audio

/**
 * An intent used to break down the context driven Calamari audio parsing into calendar-specific
 * components to populate the user's calendar.
 */
sealed class CalamariIntent {

    /**
     * Adding a calendar event for a specific time of the current day based on user's device time.
     *
     * For example, "Create an event for 5 PM (pee em)" will add an event for the current day at the
     * given time (5:00PM).
     */
    data class ThisDayIntent(
        val hour: Hour,
        val minute: Minutes?,
        val dayNight: DayNight?,
    ) : CalamariIntent() {
        constructor(slots: Map<String, String>) : this(
            hour = slots[Hour.SLOT_NAME]?.let { Hour.valueOf(it.clean()) } ?: Hour.MIDNIGHT,
            minute = slots[Minutes.SLOT_NAME]?.let { Minutes.valueOf(it.clean()) },
            dayNight = slots[DayNight.SLOT_NAME]?.let { DayNight.valueOf(it.clean()) },
        )
    }

    /**
     * Adding a calendar event for a specific day and maybe time of the day, within the week.
     *
     * For example, "Add an event for Tuesday" if the user's device date is currently Monday we
     * add an event for tomorrow (Tuesday), or if the user's device date is Tuesday, we add an event
     * for the Tuesday next week.
     *
     * If no exact time is given, we default to an all-day event.
     */
    data class ThisWeekIntent(
        val day: Day,
        val hour: Hour?,
        val minute: Minutes?,
        val dayNight: DayNight?,
    ) : CalamariIntent() {
        constructor(slots: Map<String, String>) : this(
            day = slots[Day.SLOT_NAME]?.let { Day.valueOf(it.clean()) } ?: Day.MONDAY,
            hour = slots[Hour.SLOT_NAME]?.let { Hour.valueOf(it.clean()) },
            minute = slots[Minutes.SLOT_NAME]?.let { Minutes.valueOf(it.clean()) },
            dayNight = slots[DayNight.SLOT_NAME]?.let { DayNight.valueOf(it.clean()) },
        )
    }

    /**
     * Adding a calendar event for a specific date within the current month, and may include a given
     * time for the event.
     *
     * For example, "Add an event for Friday the 13th" will add an event for the 13th of the current
     * month.
     *
     * If no exact time is given, we default to an all-day event.
     */
    data class ThisMonthIntent(
        val date: Date,
        val day: Day?,
        val hour: Hour?,
        val minute: Minutes?,
        val dayNight: DayNight?,
    ) : CalamariIntent() {
        constructor(slots: Map<String, String>) : this(
            date = slots[Date.SLOT_NAME]?.let { Date.valueOf(it.clean()) } ?: Date.FIRST,
            day = slots[Day.SLOT_NAME]?.let { Day.valueOf(it.clean()) },
            hour = slots[Hour.SLOT_NAME]?.let { Hour.valueOf(it.clean()) },
            minute = slots[Minutes.SLOT_NAME]?.let { Minutes.valueOf(it.clean()) },
            dayNight = slots[DayNight.SLOT_NAME]?.let { DayNight.valueOf(it.clean()) },
        )
    }

    /**
     * Adding a calendar event for a specific date within a 12-month span, which may include a given
     * time for the event.
     *
     * For example, "Make an event for August 5th, at 3 o'clock AM (ay em)" will add an event for
     * August 5th, at 3:00AM.
     *
     * If no exact time is given, we default to an all-day event.
     */
    data class ThisYearIntent(
        val month: Month,
        val date: Date,
        val day: Day?,
        val hour: Hour?,
        val minute: Minutes?,
        val dayNight: DayNight?,
    ) : CalamariIntent() {
        constructor(slots: Map<String, String>) : this(
            month = slots[Month.SLOT_NAME]?.let { Month.valueOf(it.clean()) } ?: Month.JANUARY,
            date = slots[Date.SLOT_NAME]?.let { Date.valueOf(it.clean()) } ?: Date.FIRST,
            day = slots[Day.SLOT_NAME]?.let { Day.valueOf(it.clean()) },
            hour = slots[Hour.SLOT_NAME]?.let { Hour.valueOf(it.clean()) },
            minute = slots[Minutes.SLOT_NAME]?.let { Minutes.valueOf(it.clean()) },
            dayNight = slots[DayNight.SLOT_NAME]?.let { DayNight.valueOf(it.clean()) },
        )
    }

    enum class Day {
        MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY;

        companion object {
            const val SLOT_NAME = "day"
        }
    }

    enum class Month(val number: Int) {
        JANUARY(1),
        FEBRUARY(2),
        MARCH(3),
        APRIL(4),
        MAY(5),
        JUNE(6),
        JULY(7),
        AUGUST(8),
        SEPTEMBER(9),
        OCTOBER(10),
        NOVEMBER(11),
        DECEMBER(12);

        companion object {
            const val SLOT_NAME = "month"
        }
    }

    enum class Date(val number: Int) {
        FIRST(1),
        SECOND(2),
        THIRD(3),
        FOURTH(4),
        FIFTH(5),
        SIXTH(6),
        SEVENTH(7),
        EIGHTH(8),
        NINTH(9),
        TENTH(10),
        ELEVENTH(11),
        TWELFTH(12),
        THIRTEENTH(13),
        FOURTEENTH(14),
        FIFTEENTH(15),
        SIXTEENTH(16),
        SEVENTEENTH(17),
        EIGHTEENTH(18),
        NINETEENTH(19),
        TWENTIETH(20),
        TWENTY_FIRST(21),
        TWENTY_SECOND(22),
        TWENTY_THIRD(23),
        TWENTY_FOURTH(24),
        TWENTY_FIFTH(25),
        TWENTY_SIXTH(26),
        TWENTY_SEVENTH(27),
        TWENTY_EIGHTH(28),
        TWENTY_NINTH(29),
        THIRTIETH(30),
        THIRTY_FIRST(31);

        companion object {
            const val SLOT_NAME = "date"
        }
    }

    enum class Hour(val number: Int) {
        ONE(1),
        TWO(2),
        THREE(3),
        FOUR(4),
        FIVE(5),
        SIX(6),
        SEVEN(7),
        EIGHT(8),
        NINE(9),
        TEN(10),
        ELEVEN(11),
        TWELVE(12),
        NOON(12),
        MIDNIGHT(0);

        companion object {
            const val SLOT_NAME = "hour"
        }
    }

    enum class DayNight(val isMorning: Boolean) {
        IN_THE_EVENING(false),
        IN_THE_AFTERNOON(false),
        IN_THE_MORNING(true),
        PEE_EM(false),
        AY_EM(true);

        companion object {
            const val SLOT_NAME = "daynight"
        }
    }

    enum class Minutes(val number: Int) {
        OH_FIVE(5),
        TEN(10),
        FIFTEEN(15),
        TWENTY(20),
        TWENTY_FIVE(25),
        THIRTY(30),
        THIRTY_FIVE(35),
        FORTY(40),
        FORTY_FIVE(45),
        FIFTY(50),
        FIFTY_FIVE(55);

        companion object {
            const val SLOT_NAME = "minutes"
        }
    }

    private enum class Type {
        ADDTHISDAY, ADDTHISWEEK, ADDTHISMONTH, ADDTHISYEAR;
    }

    companion object {
        fun create(
            type: String,
            slots: Map<String, String>
        ): CalamariIntent? {
            return try {
                when (Type.valueOf(type.trim().uppercase())) {
                    Type.ADDTHISDAY -> ThisDayIntent(slots)
                    Type.ADDTHISWEEK -> ThisWeekIntent(slots)
                    Type.ADDTHISMONTH -> ThisMonthIntent(slots)
                    Type.ADDTHISYEAR -> ThisYearIntent(slots)
                }
            } catch (e: IllegalArgumentException) {
                e.printStackTrace()
                null
            }
        }
    }
}

/**
 * Convenience function to allow slot data to match directly with the enum values.
 */
private fun String.clean(): String {
    return uppercase().replace(' ', '_').trim()
}