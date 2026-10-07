package com.example.tools

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.ContactsContract
import androidx.core.content.ContextCompat
import com.example.data.JarvisDatabase
import java.net.URLEncoder

data class ContactMatch(
    val name: String,
    val number: String,
    val isTrusted: Boolean = false,
    val relationship: String? = null,
    val source: String = "Device"
)

object ContactResolver {

    suspend fun resolveContacts(context: Context, queryName: String): List<ContactMatch> {
        val matches = mutableListOf<ContactMatch>()
        val cleanQuery = queryName.trim().lowercase()

        // 1. Search Local JARVIS Vault (Trusted & Saved Contacts)
        try {
            val db = JarvisDatabase.getInstance(context)
            val localContacts = db.trustedContactDao().searchContacts(cleanQuery)
            for (c in localContacts) {
                matches.add(
                    ContactMatch(
                        name = c.name,
                        number = c.phoneNumber,
                        isTrusted = c.isTrusted,
                        relationship = c.relationship,
                        source = "JARVIS Vault"
                    )
                )
            }
        } catch (_: Exception) {}

        // 2. Search Device Contacts via ContactsContract (if permission is granted)
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
            val projection = arrayOf(
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER
            )
            val selection = "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?"
            val selectionArgs = arrayOf("%$cleanQuery%")

            try {
                context.contentResolver.query(
                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    projection,
                    selection,
                    selectionArgs,
                    "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC"
                )?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                    val numberIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                    while (cursor.moveToNext() && matches.size < 12) {
                        val name = if (nameIndex >= 0) cursor.getString(nameIndex) else cleanQuery
                        val number = if (numberIndex >= 0) cursor.getString(numberIndex) else ""
                        val normalizedNum = number.replace("[^0-9+]".toRegex(), "")
                        val exists = matches.any {
                            it.name.equals(name, ignoreCase = true) ||
                                    (normalizedNum.isNotEmpty() && it.number.replace("[^0-9+]".toRegex(), "") == normalizedNum)
                        }
                        if (!exists && number.isNotBlank()) {
                            matches.add(
                                ContactMatch(
                                    name = name,
                                    number = number,
                                    isTrusted = false,
                                    source = "Device Contacts"
                                )
                            )
                        }
                    }
                }
            } catch (_: Exception) {}
        }

        return matches
    }
}

class CallTool : JarvisTool {
    override val toolId: String = "make_call"
    override val name: String = "Make Phone Call"
    override val requiredPermission: String = Manifest.permission.CALL_PHONE
    override val requiresConfirmation: Boolean = true

    override suspend fun execute(context: Context, params: Map<String, Any?>): ToolResult {
        val contactName = params["contactName"] as? String ?: return ToolResult(
            success = false,
            spokenResponse = "Kisko call karna hai? Kripya naam bataiye.",
            displayMessage = "Contact name required."
        )
        val isConfirmed = params["isConfirmed"] as? Boolean ?: false
        val isTrustedBypass = params["isTrustedBypass"] as? Boolean ?: false
        var phoneNumber = params["phoneNumber"] as? String
        var resolvedName = contactName

        // Step 1: Identify Contact & Check Ambiguity
        if (phoneNumber.isNullOrBlank()) {
            val matches = ContactResolver.resolveContacts(context, contactName)
            when {
                matches.size > 1 -> {
                    // AMBIGUOUS CONTACTS DETECTED: NEVER call silently!
                    val candidateNames = matches.joinToString(" ya ") { it.name }
                    val spokenClarification = "Kaunsa contact? $candidateNames?"
                    return ToolResult(
                        success = false,
                        spokenResponse = spokenClarification,
                        displayMessage = "Ambiguous contacts found for '$contactName':\n" +
                                matches.joinToString("\n") { "• ${it.name} (${it.number})" },
                        requiresUiInteraction = true,
                        payload = mapOf(
                            "type" to "disambiguation",
                            "candidates" to matches,
                            "originalQuery" to contactName
                        )
                    )
                }

                matches.size == 1 -> {
                    phoneNumber = matches[0].number
                    resolvedName = matches[0].name
                }

                else -> {
                    // Contact not found
                    return ToolResult(
                        success = false,
                        spokenResponse = "Mujhe '$contactName' naam ka koi contact nahi mila. Kya phone dialer open karun?",
                        displayMessage = "No contact found matching '$contactName'.",
                        requiresUiInteraction = true,
                        payload = mapOf("type" to "not_found", "query" to contactName)
                    )
                }
            }
        }

        // Step 2: Confirmation Check (Bypassed ONLY if trusted-contact calling is enabled and contact is trusted)
        if (!isConfirmed && !isTrustedBypass) {
            val displayPhone = phoneNumber?.let { " ($it)" } ?: ""
            return ToolResult(
                success = false,
                spokenResponse = "$resolvedName ko call lagau?",
                displayMessage = "Confirmation needed: Call $resolvedName$displayPhone?",
                requiresUiInteraction = true,
                payload = mapOf(
                    "type" to "confirmation",
                    "contactName" to resolvedName,
                    "phoneNumber" to phoneNumber
                )
            )
        }

        // Step 3: Initiate Call using Android APIs (ACTION_DIAL first with proper permission handling)
        val cleanNumber = phoneNumber?.replace("[^0-9+]".toRegex(), "") ?: ""
        val callUri = if (cleanNumber.isNotBlank()) Uri.parse("tel:$cleanNumber") else Uri.parse("tel:")

        // Requirement 3: Implement Call action using ACTION_DIAL first
        val dialIntent = Intent(Intent.ACTION_DIAL, callUri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        return try {
            context.startActivity(dialIntent)
            ToolResult(
                success = true,
                spokenResponse = "Ji, $resolvedName ke liye dialer open kar diya hai.",
                displayMessage = "Opened dialer for $resolvedName ($cleanNumber)."
            )
        } catch (e: Exception) {
            // Fallback to ACTION_CALL if dialer intent resolution fails and CALL_PHONE is granted
            val hasCallPermission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CALL_PHONE
            ) == PackageManager.PERMISSION_GRANTED

            if (hasCallPermission) {
                try {
                    val callIntent = Intent(Intent.ACTION_CALL, callUri).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(callIntent)
                    ToolResult(
                        success = true,
                        spokenResponse = "Ji, $resolvedName ko call lagaya ja raha hai.",
                        displayMessage = "Directly placed call to $resolvedName."
                    )
                } catch (ex: Exception) {
                    ToolResult(
                        success = false,
                        spokenResponse = "Call lagane mein problem aayi: ${ex.message}",
                        displayMessage = "Call failed: ${ex.message}"
                    )
                }
            } else {
                ToolResult(
                    success = false,
                    spokenResponse = "Phone dialer open nahi ho saka: ${e.message}",
                    displayMessage = "Unable to open dialer: ${e.message}"
                )
            }
        }
    }
}

class WhatsAppTool : JarvisTool {
    override val toolId: String = "open_whatsapp"
    override val name: String = "WhatsApp Integration"
    override val requiredPermission: String? = null
    override val requiresConfirmation: Boolean = true

    override suspend fun execute(context: Context, params: Map<String, Any?>): ToolResult {
        val contactName = params["contactName"] as? String
        val message = params["message"] as? String
        val isConfirmed = params["isConfirmed"] as? Boolean ?: false

        val pm = context.packageManager
        val isInstalled = try {
            pm.getPackageInfo("com.whatsapp", 0)
            true
        } catch (_: Exception) {
            try {
                pm.getPackageInfo("com.whatsapp.w4b", 0)
                true
            } catch (_: Exception) {
                false
            }
        }

        // Requirement 4: Check if installed, otherwise show clear fallback message
        if (!isInstalled) {
            return ToolResult(
                success = false,
                spokenResponse = "WhatsApp is device par installed nahi hai.",
                displayMessage = "WhatsApp is not installed on this device."
            )
        }

        // Just open WhatsApp (e.g. "Open WhatsApp" or "WhatsApp kholo")
        if (contactName.isNullOrBlank() && message.isNullOrBlank()) {
            val launchIntent = pm.getLaunchIntentForPackage("com.whatsapp")
                ?: pm.getLaunchIntentForPackage("com.whatsapp.w4b")
            return if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(launchIntent)
                ToolResult(
                    success = true,
                    spokenResponse = "WhatsApp open kar diya hai.",
                    displayMessage = "Opening WhatsApp application..."
                )
            } else {
                ToolResult(
                    success = false,
                    spokenResponse = "WhatsApp open nahi ho saka.",
                    displayMessage = "Unable to launch WhatsApp."
                )
            }
        }

        // Sending message requires confirmation!
        if (!message.isNullOrBlank() && !isConfirmed) {
            val target = contactName ?: "Contact"
            return ToolResult(
                success = false,
                spokenResponse = "$target ko ye message bhejun: '$message'?",
                displayMessage = "Confirm: Send message to $target: '$message'?",
                requiresUiInteraction = true,
                payload = mapOf("contactName" to contactName, "message" to message)
            )
        }

        // Resolve phone number if contact given
        var phoneNumber: String? = null
        if (!contactName.isNullOrBlank()) {
            val matches = ContactResolver.resolveContacts(context, contactName)
            if (matches.isNotEmpty()) {
                phoneNumber = matches[0].number.replace("[^0-9+]".toRegex(), "")
            }
        }

        // Official WhatsApp API Intent
        return try {
            val intent = if (!phoneNumber.isNullOrBlank()) {
                val encodedMsg = URLEncoder.encode(message ?: "", "UTF-8")
                val url = "https://api.whatsapp.com/send?phone=$phoneNumber&text=$encodedMsg"
                Intent(Intent.ACTION_VIEW, Uri.parse(url))
            } else if (!message.isNullOrBlank()) {
                Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, message)
                    setPackage("com.whatsapp")
                }
            } else {
                pm.getLaunchIntentForPackage("com.whatsapp")
                    ?: pm.getLaunchIntentForPackage("com.whatsapp.w4b")
                    ?: Intent(Intent.ACTION_VIEW, Uri.parse("https://whatsapp.com"))
            }

            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)

            val spoken = if (!contactName.isNullOrBlank() && !message.isNullOrBlank()) {
                "Ji, $contactName ke liye WhatsApp message draft kar diya hai."
            } else if (!contactName.isNullOrBlank()) {
                "$contactName ki WhatsApp chat open kar raha hoon."
            } else {
                "WhatsApp open kar raha hoon."
            }

            ToolResult(
                success = true,
                spokenResponse = spoken,
                displayMessage = "Opened WhatsApp for action."
            )
        } catch (e: Exception) {
            ToolResult(
                success = false,
                spokenResponse = "WhatsApp open karte waqt error aaya: ${e.message}",
                displayMessage = "WhatsApp intent failed: ${e.message}"
            )
        }
    }
}
