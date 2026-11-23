package com.example.sensy.data

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.ContactsContract
import android.util.Log
import androidx.core.content.ContextCompat

/**
 * Tells whether a phone number belongs to one of the user's saved contacts.
 *
 * Uses PhoneLookup, which matches numbers regardless of formatting
 * (e.g. "+91 98765 43210", "098765 43210" and "9876543210" all match the same contact).
 *
 * Fails safe: if the contacts permission is missing or the lookup errors,
 * the number is treated as unknown so no personal details are shared.
 */
class ContactChecker(private val context: Context) {

    fun hasPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) ==
            PackageManager.PERMISSION_GRANTED

    fun isSavedContact(phoneNumber: String): Boolean {
        if (phoneNumber.isBlank() || !hasPermission()) return false

        val uri = Uri.withAppendedPath(
            ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
            Uri.encode(phoneNumber)
        )
        return try {
            context.contentResolver
                .query(uri, arrayOf(ContactsContract.PhoneLookup._ID), null, null, null)
                ?.use { it.moveToFirst() }
                ?: false
        } catch (e: Exception) {
            Log.e("ContactChecker", "Contact lookup failed", e)
            false
        }
    }
}
