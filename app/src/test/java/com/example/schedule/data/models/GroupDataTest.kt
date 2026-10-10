package com.example.schedule.data.models

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GroupDataTest {

    @Test
    fun testNormalizeQueryExtractsNumberFromGroupWithLetters() {
        assertEquals("105", GroupData.normalizeQuery("105ТП"))
        assertEquals("105", GroupData.normalizeQuery("105тп"))
        assertEquals("88", GroupData.normalizeQuery("88 ТП"))
        assertEquals("9", GroupData.normalizeQuery("9СР"))
        assertEquals("100", GroupData.normalizeQuery("№100МНЭ"))
    }

    @Test
    fun testNormalizeQueryPreservesTeacherNames() {
        assertEquals("Козел Г.В.", GroupData.normalizeQuery("Козел Г.В."))
        assertEquals("Иванов", GroupData.normalizeQuery("  Иванов  "))
    }

    @Test
    fun testFindGroupsByNumberPrefix() {
        val matches = GroupData.findGroups("10")
        assertTrue(matches.isNotEmpty())
        assertTrue(matches.any { it.number == "105" })
        assertTrue(matches.any { it.number == "100" })
    }

    @Test
    fun testFindGroupsBySpecialty() {
        val tpGroups = GroupData.findGroups("ТП")
        assertTrue(tpGroups.isNotEmpty())
        assertTrue(tpGroups.all { it.specialty.contains("ТП", ignoreCase = true) })
        assertTrue(tpGroups.any { it.fullName == "105ТП" })
        assertTrue(tpGroups.any { it.fullName == "88ТП" })
    }
}
