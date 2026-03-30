package com.mathieu.blocker.data

data class PlannerEntry(
    val id: String = java.util.UUID.randomUUID().toString(),
    val profileId: String,
    val startHour: Int, // 0-23
    val startMinute: Int, // 0-59
    val endHour: Int,
    val endMinute: Int,
    val daysOfWeek: Set<Int> = setOf(1, 2, 3, 4, 5, 6, 7), // 1=Monday..7=Sunday
    val enabled: Boolean = true
) {
    fun isActiveNow(): Boolean {
        if (!enabled) return false
        val now = java.util.Calendar.getInstance()
        val currentDay = now.get(java.util.Calendar.DAY_OF_WEEK)
        // Convert Calendar day (1=Sun..7=Sat) to ISO (1=Mon..7=Sun)
        val isoDay = if (currentDay == java.util.Calendar.SUNDAY) 7 else currentDay - 1
        if (isoDay !in daysOfWeek) return false

        val currentMinutes = now.get(java.util.Calendar.HOUR_OF_DAY) * 60 + now.get(java.util.Calendar.MINUTE)
        val startMinutes = startHour * 60 + startMinute
        val endMinutes = endHour * 60 + endMinute

        return if (startMinutes <= endMinutes) {
            currentMinutes in startMinutes until endMinutes
        } else {
            // Overnight (e.g., 22:00 - 06:00)
            currentMinutes >= startMinutes || currentMinutes < endMinutes
        }
    }
}
