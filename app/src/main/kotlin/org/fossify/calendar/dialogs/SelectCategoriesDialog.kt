package org.fossify.calendar.dialogs

import android.app.Activity
import android.widget.ScrollView
import android.widget.TextView
import org.fossify.calendar.R
import org.fossify.calendar.databinding.DialogVerticalLinearLayoutBinding
import org.fossify.calendar.databinding.MyCheckboxBinding
import org.fossify.calendar.models.CategorySelection
import org.fossify.commons.extensions.getAlertDialogBuilder
import org.fossify.commons.extensions.setupDialogStuff
import org.fossify.commons.extensions.toast

class SelectCategoriesDialog(
    activity: Activity,
    currentCategories: List<String>,
    knownCategories: List<String>,
    callback: (List<String>) -> Unit
) {
    init {
        val selection = CategorySelection(currentCategories, knownCategories)
        if (selection.categories.isEmpty()) {
            activity.toast(org.fossify.commons.R.string.no_items_found)
        } else {
            val binding = DialogVerticalLinearLayoutBinding.inflate(activity.layoutInflater)
            selection.categories.forEach { category ->
                MyCheckboxBinding.inflate(activity.layoutInflater).root.apply {
                    text = category
                    isChecked = selection.isSelected(category)
                    setOnCheckedChangeListener { _, checked -> selection.setSelected(category, checked) }
                    binding.dialogVerticalLinearLayout.addView(this)
                }
            }
            val scrollView = ScrollView(activity).apply { addView(binding.root) }
            activity.getAlertDialogBuilder()
                .setPositiveButton(org.fossify.commons.R.string.ok) { _, _ ->
                    callback(selection.getSelectedCategories())
                }
                .setNegativeButton(org.fossify.commons.R.string.cancel, null)
                .apply {
                    activity.setupDialogStuff(scrollView, this, R.string.select_existing_categories) { dialog ->
                        dialog.findViewById<TextView>(androidx.appcompat.R.id.alertTitle)?.apply {
                            setSingleLine(false)
                            maxLines = Int.MAX_VALUE
                            ellipsize = null
                        }
                    }
                }
        }
    }
}
