package com.akerusan.fieldincidentreporter.core.designsystem.format

import android.text.format.DateUtils
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.akerusan.fieldincidentreporter.core.designsystem.R
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/** "Just now", "5 min. ago", "Yesterday", or a date, in the device's language. */
@Composable
fun Instant.toRelativeTimeText(now: Instant = Instant.now()): String {
    if (Duration.between(this, now) < Duration.ofMinutes(1)) return stringResource(R.string.time_just_now)
    return DateUtils.getRelativeTimeSpanString(
        toEpochMilli(),
        now.toEpochMilli(),
        DateUtils.MINUTE_IN_MILLIS,
        DateUtils.FORMAT_ABBREV_RELATIVE,
    ).toString()
}

/** A full date and time in the device's locale and time zone, for detail screens. */
fun Instant.toDateTimeText(zone: ZoneId = ZoneId.systemDefault()): String =
    DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT).withZone(zone).format(this)
