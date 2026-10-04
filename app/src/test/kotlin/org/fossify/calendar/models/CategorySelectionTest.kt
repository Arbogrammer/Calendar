package org.fossify.calendar.models

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CategorySelectionTest {
    @Test
    fun preservesUnsavedCategoriesWhenAddingAnExistingCategory() {
        val selection = CategorySelection(listOf("New category"), listOf("Work"))
        selection.setSelected("Work", true)
        assertEquals(listOf("New category", "Work"), selection.getSelectedCategories())
    }

    @Test
    fun confirmingWithoutChangesPreservesFirstCategoryAndSpelling() {
        val selection = CategorySelection(listOf("Work", "Family"), listOf("family", "work", "Travel"))
        assertEquals(listOf("Work", "Family"), selection.getSelectedCategories())
        assertEquals(listOf("Work", "Family", "Travel"), selection.categories)
    }

    @Test
    fun offersUnsavedCategoriesEvenWithoutSavedEvents() {
        val selection = CategorySelection(listOf("New category"), emptyList())
        assertEquals(listOf("New category"), selection.categories)
        assertTrue(selection.isSelected("New category"))
    }

    @Test
    fun canRemoveCategoriesAndSelectDifferentCasing() {
        val selection = CategorySelection(listOf("Work"), listOf("Family"))
        selection.setSelected("work", false)
        selection.setSelected("FAMILY", true)
        assertEquals(listOf("Family"), selection.getSelectedCategories())
    }
}
