package com.example.s_journal.data.local.database

import com.example.s_journal.data.model.EntryType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class ConvertersTest {

    private lateinit var converters: Converters

    @Before
    fun setUp() {
        converters = Converters()
    }

    @Test
    fun localDate_roundTrip_and_nullHandling() {
        val date = LocalDate.of(2026, 10, 16)
        val serialized = converters.fromLocalDate(date)
        assertEquals("2026-10-16", serialized)
        assertEquals(date, converters.toLocalDate(serialized))

        assertNull(converters.fromLocalDate(null))
        assertNull(converters.toLocalDate(null))
    }

    @Test
    fun instant_roundTrip_and_nullHandling() {
        val instant = Instant.ofEpochMilli(1760616000000L)
        val millis = converters.fromInstant(instant)
        assertEquals(1760616000000L, millis)
        assertEquals(instant, converters.toInstant(millis))

        assertNull(converters.fromInstant(null))
        assertNull(converters.toInstant(null))
    }

    @Test
    fun entryType_roundTrip_and_nullHandling() {
        for (type in EntryType.entries) {
            val name = converters.fromEntryType(type)
            assertEquals(type.name, name)
            assertEquals(type, converters.toEntryType(name))
        }

        assertNull(converters.fromEntryType(null))
        assertNull(converters.toEntryType(null))
    }

    @Test
    fun stringList_roundTrip_empty_and_specialCharacters() {
        val tags = listOf("personal", "s-pen", "año 2026", "quote \"test\"")
        val json = converters.fromStringList(tags)
        val restored = converters.toStringList(json)
        assertEquals(tags, restored)

        assertTrue(converters.toStringList(null).isEmpty())
        assertTrue(converters.toStringList("").isEmpty())
        assertNull(converters.fromStringList(null))
    }
}
