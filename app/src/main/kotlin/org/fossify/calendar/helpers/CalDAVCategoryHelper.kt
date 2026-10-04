package org.fossify.calendar.helpers

import android.annotation.SuppressLint
import android.content.ContentProviderOperation
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.provider.CalendarContract
import android.provider.CalendarContract.Calendars
import android.provider.CalendarContract.ExtendedProperties
import org.fossify.calendar.models.CalDAVCalendar
import org.fossify.calendar.models.Event
import org.fossify.commons.extensions.getStringValue

@SuppressLint("MissingPermission")
class CalDAVCategoryHelper(
    private val context: Context,
    private val getCalendar: (Int) -> CalDAVCalendar?
) {
    private companion object {
        const val DAVX5_CATEGORIES_PROPERTY = "categories"
        const val DAVX5_CATEGORIES_SEPARATOR = '\\'
    }

    /**
     * DAVx⁵/ical4android stores VEVENT CATEGORIES in this Calendar Provider
     * extended property. Keeping its format makes category edits round-trip to
     * the standard iCalendar CATEGORIES property during the next DAVx⁵ sync.
     */
    fun getCalDAVEventCategories(eventId: Long): List<String> {
        val categories = ArrayList<String>()
        context.contentResolver.query(
            ExtendedProperties.CONTENT_URI,
            arrayOf(ExtendedProperties.VALUE),
            "${ExtendedProperties.EVENT_ID}=? AND ${ExtendedProperties.NAME}=?",
            arrayOf(eventId.toString(), DAVX5_CATEGORIES_PROPERTY),
            null
        )?.use { cursor ->
            while (cursor.moveToNext()) {
                cursor.getStringValue(ExtendedProperties.VALUE)
                    ?.split(DAVX5_CATEGORIES_SEPARATOR)
                    ?.map { it.trim() }
                    ?.filter { it.isNotEmpty() }
                    ?.let(categories::addAll)
            }
        }
        return categories.distinct()
    }

    fun setupCalDAVEventCategories(event: Event) {
        val eventId = event.getCalDAVEventId()
        context.contentResolver.delete(
            getCalDAVCategoriesUri(event),
            "${ExtendedProperties.EVENT_ID}=? AND ${ExtendedProperties.NAME}=?",
            arrayOf(eventId.toString(), DAVX5_CATEGORIES_PROPERTY)
        )

        val serializedCategories = serializeCategories(event.categories)
        if (serializedCategories.isNotEmpty()) {
            context.contentResolver.insert(
                getCalDAVCategoriesUri(event),
                ContentValues().apply {
                    put(ExtendedProperties.EVENT_ID, eventId)
                    put(ExtendedProperties.NAME, DAVX5_CATEGORIES_PROPERTY)
                    put(ExtendedProperties.VALUE, serializedCategories)
                }
            )
        }
    }

    /**
     * The Calendar Provider stores DAVx⁵'s own extended properties in its
     * sync-adapter namespace. Using the same URI prevents it from treating the
     * property as foreign metadata and removing it on the next adapter pass.
     * The event row itself is deliberately still updated through the regular
     * URI, so it is marked dirty and DAVx⁵ uploads the change.
     */
    private fun getCalDAVCategoriesUri(event: Event): Uri {
        val calendar = getCalendar(event.getCalDAVCalendarId())
            ?: return ExtendedProperties.CONTENT_URI
        return ExtendedProperties.CONTENT_URI.buildUpon()
            .appendQueryParameter(CalendarContract.CALLER_IS_SYNCADAPTER, "true")
            .appendQueryParameter(Calendars.ACCOUNT_NAME, calendar.accountName)
            .appendQueryParameter(Calendars.ACCOUNT_TYPE, calendar.accountType)
            .build()
    }

    /**
     * DAVx⁵ observes Calendar Provider changes.  A category property and the
     * corresponding VEVENT update must therefore be committed together: an
     * intermediate notification lets DAVx⁵ re-import the server version and
     * discard a freshly entered category.
     */
    fun updateCalDAVEventAndCategoriesAtomically(
        event: Event,
        eventUri: Uri,
        eventValues: ContentValues,
    ) {
        val eventId = event.getCalDAVEventId()
        val categoriesUri = getCalDAVCategoriesUri(event)
        val operations = arrayListOf(
            ContentProviderOperation.newDelete(categoriesUri)
                .withSelection(
                    "${ExtendedProperties.EVENT_ID}=? AND ${ExtendedProperties.NAME}=?",
                    arrayOf(eventId.toString(), DAVX5_CATEGORIES_PROPERTY)
                )
                .build()
        )

        val serializedCategories = serializeCategories(event.categories)
        if (serializedCategories.isNotEmpty()) {
            operations.add(
                ContentProviderOperation.newInsert(categoriesUri)
                    .withValue(ExtendedProperties.EVENT_ID, eventId)
                    .withValue(ExtendedProperties.NAME, DAVX5_CATEGORIES_PROPERTY)
                    .withValue(ExtendedProperties.VALUE, serializedCategories)
                    .build()
            )
        }
        operations.add(ContentProviderOperation.newUpdate(eventUri).withValues(eventValues).build())
        context.contentResolver.applyBatch(CalendarContract.AUTHORITY, operations)
    }

    private fun serializeCategories(categories: List<String>): String = categories
        .map { it.trim().replace(DAVX5_CATEGORIES_SEPARATOR.toString(), "") }
        .filter { it.isNotEmpty() }
        .distinct()
        .joinToString(DAVX5_CATEGORIES_SEPARATOR.toString())
}
