package com.vetstop.app.domain.model

import java.util.concurrent.TimeUnit

/** Filter for locations by how long ago they were last visited. */
enum class VisitFilter(val label: String) {
    ANY("All"),
    NEVER("Never visited"),
    OVER_1_WEEK("1+ week ago"),
    OVER_1_MONTH("1+ month ago"),
    OVER_3_MONTHS("3+ months ago");

    fun matches(lastVisitAt: Long?, now: Long = System.currentTimeMillis()): Boolean =
        when (this) {
            ANY -> true
            NEVER -> lastVisitAt == null
            OVER_1_WEEK -> lastVisitAt == null || now - lastVisitAt >= TimeUnit.DAYS.toMillis(7)
            OVER_1_MONTH -> lastVisitAt == null || now - lastVisitAt >= TimeUnit.DAYS.toMillis(30)
            OVER_3_MONTHS -> lastVisitAt == null || now - lastVisitAt >= TimeUnit.DAYS.toMillis(90)
        }
}
