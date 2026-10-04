package org.fossify.calendar.models

import java.util.Locale

class CategorySelection(currentCategories: List<String>, knownCategories: List<String>) {
    // Keep the current order: the first category determines the event color.
    val categories = (currentCategories + knownCategories.sortedWith(String.CASE_INSENSITIVE_ORDER))
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .distinctBy { it.lowercase(Locale.ROOT) }
    private val selected = currentCategories.map { it.trim().lowercase(Locale.ROOT) }.toMutableSet()

    fun isSelected(category: String) = category.lowercase(Locale.ROOT) in selected

    fun setSelected(category: String, isSelected: Boolean) {
        val key = category.lowercase(Locale.ROOT)
        if (isSelected) {
            selected.add(key)
        } else {
            selected.remove(key)
        }
    }

    fun getSelectedCategories() = categories.filter { isSelected(it) }
}
