package io.github.steven_stu77.lazee.core.data

import androidx.room.TypeConverter
import java.time.Instant
import java.time.LocalDate

/** Dates are stored as epochDay, instants as epoch millis. */
class Converters {
    @TypeConverter fun dateToLong(d: LocalDate?): Long? = d?.toEpochDay()
    @TypeConverter fun longToDate(v: Long?): LocalDate? = v?.let(LocalDate::ofEpochDay)
    @TypeConverter fun instantToLong(i: Instant?): Long? = i?.toEpochMilli()
    @TypeConverter fun longToInstant(v: Long?): Instant? = v?.let(Instant::ofEpochMilli)
}
