package dev.jvmname.acquisitive.repo.db

import app.cash.sqldelight.ColumnAdapter
import dev.jvmname.acquisitive.network.model.ItemId
import dev.jvmname.acquisitive.util.ItemIdArray
import java.util.regex.Pattern
import kotlin.time.Instant

val InstantColumnAdapter = object : ColumnAdapter<Instant, Long> {
    override fun decode(databaseValue: Long): Instant = Instant.fromEpochMilliseconds(databaseValue)
    override fun encode(value: Instant): Long = value.toEpochMilliseconds()
}

val ItemIdColumnAdapter = object : ColumnAdapter<ItemId, Long> {
    override fun decode(databaseValue: Long): ItemId = ItemId(databaseValue.toInt())
    override fun encode(value: ItemId): Long = value.id.toLong()
}

val ItemIdArrayColumnAdapter = object : ColumnAdapter<ItemIdArray, String> {
    private val separator = Pattern.compile(",")

    override fun decode(databaseValue: String): ItemIdArray {
        return if (databaseValue.isEmpty()) ItemIdArray(0)
        else {
            val split = databaseValue.split(separator)
            ItemIdArray(split.size) { ItemId(split[it].toInt()) }
        }
    }

    override fun encode(value: ItemIdArray): String {
        return if (value.isEmpty()) ""
        else value.joinToString(separator = ",") { it.id.toString()}
    }
}

val IntLongColumnAdapter = object : ColumnAdapter<Int, Long> {
    override fun decode(databaseValue: Long): Int = databaseValue.toInt()

    override fun encode(value: Int): Long = value.toLong()

}