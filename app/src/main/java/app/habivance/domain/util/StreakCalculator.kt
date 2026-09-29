package app.habivance.domain.util

import java.time.LocalDate

object StreakCalculator {

    /**
     * Current streak = number of consecutive days (ending today or yesterday) 
     * with a completion. If today is not done but yesterday was, streak is still 
     * counted from yesterday (gives a grace period during the day).
     */
    fun currentStreak(completionEpochDays: Set<Long>, today: LocalDate = LocalDate.now()): Int {
        if (completionEpochDays.isEmpty()) return 0

        val todayEpoch = today.toEpochDay()
        val yesterdayEpoch = today.minusDays(1).toEpochDay()

        // Determine the starting point: today if completed, else yesterday if completed, else 0
        val start = when {
            completionEpochDays.contains(todayEpoch) -> todayEpoch
            completionEpochDays.contains(yesterdayEpoch) -> yesterdayEpoch
            else -> return 0
        }

        var streak = 0
        var cursor = start
        while (completionEpochDays.contains(cursor)) {
            streak++
            cursor--
        }
        return streak
    }

    /**
     * Best streak = longest consecutive run of completions in the entire history.
     */
    fun bestStreak(completionEpochDays: Set<Long>): Int {
        if (completionEpochDays.isEmpty()) return 0
        val sorted = completionEpochDays.sorted()
        var best = 1
        var current = 1
        for (i in 1 until sorted.size) {
            if (sorted[i] == sorted[i - 1] + 1) {
                current++
                if (current > best) best = current
            } else {
                current = 1
            }
        }
        return best
    }
}
