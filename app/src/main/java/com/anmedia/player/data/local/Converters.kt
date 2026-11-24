package com.anmedia.player.data.local

import androidx.room.TypeConverter
import com.anmedia.player.domain.model.SourceType
import com.anmedia.player.domain.model.StreamMode

class Converters {
    @TypeConverter
    fun fromSourceType(value: SourceType): String = value.name

    @TypeConverter
    fun toSourceType(value: String): SourceType = SourceType.valueOf(value)

    @TypeConverter
    fun fromStreamMode(value: StreamMode): String = value.name

    @TypeConverter
    fun toStreamMode(value: String): StreamMode = StreamMode.valueOf(value)
}
