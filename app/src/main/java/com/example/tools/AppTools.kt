package com.example.tools

import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import android.provider.Settings
import com.example.engine.AppTarget

class AppLauncherTool : JarvisTool {
    override val toolId: String = "open_app"
    override val name: String = "App Launcher"
    override val requiredPermission: String? = null
    override val requiresConfirmation: Boolean = false

    override suspend fun execute(context: Context, params: Map<String, Any?>): ToolResult {
        val target = params["target"] as? AppTarget ?: AppTarget.UNKNOWN
        val appName = params["appName"] as? String ?: "App"

        if (target == AppTarget.PHONE) {
            val dialIntent = Intent(Intent.ACTION_DIAL).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            return try {
                context.startActivity(dialIntent)
                ToolResult(true, "Phone dialer open kar diya hai.", "Opening Phone Dialer...")
            } catch (e: Exception) {
                ToolResult(false, "Dialer open nahi ho saka: ${e.message}", "Failed to open Phone.")
            }
        }

        if (target == AppTarget.SETTINGS) {
            val settingsIntent = Intent(Settings.ACTION_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            return try {
                context.startActivity(settingsIntent)
                ToolResult(true, "Settings khol di hai.", "Opening Android Settings...")
            } catch (e: Exception) {
                ToolResult(false, "Settings open nahi ho saka.", "Failed to open Settings.")
            }
        }

        if (target == AppTarget.MESSAGES) {
            return try {
                val smsIntent = Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_APP_MESSAGING)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(smsIntent)
                ToolResult(true, "Messages app open kar diya hai.", "Opening Messages...")
            } catch (_: Exception) {
                val fallbackIntent = Intent(Intent.ACTION_VIEW).apply {
                    data = Uri.parse("sms:")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(fallbackIntent)
                ToolResult(true, "Messages app open kar diya hai.", "Opening Messages...")
            }
        }

        val packageName = when (target) {
            AppTarget.YOUTUBE -> "com.google.android.youtube"
            AppTarget.INSTAGRAM -> "com.instagram.android"
            AppTarget.WHATSAPP -> "com.whatsapp"
            AppTarget.CHROME -> "com.android.chrome"
            AppTarget.GMAIL -> "com.google.android.gm"
            AppTarget.MAPS -> "com.google.android.apps.maps"
            AppTarget.GALLERY -> "com.google.android.apps.photos"
            else -> null
        }

        val pm = context.packageManager
        if (packageName != null) {
            val launchIntent = pm.getLaunchIntentForPackage(packageName)
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(launchIntent)
                return ToolResult(
                    success = true,
                    spokenResponse = "Ji, $appName open kar raha hoon.",
                    displayMessage = "Opening $appName..."
                )
            }
        }

        // Generic launch by app name
        val intent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val apps = pm.queryIntentActivities(intent, 0)
        val matchedApp = apps.firstOrNull {
            it.loadLabel(pm).toString().contains(appName, ignoreCase = true)
        }

        if (matchedApp != null) {
            val launchIntent = pm.getLaunchIntentForPackage(matchedApp.activityInfo.packageName)
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(launchIntent)
                return ToolResult(
                    success = true,
                    spokenResponse = "Ji, $appName khol diya hai.",
                    displayMessage = "Opening $appName..."
                )
            }
        }

        // Fallback for Chrome to any web browser
        if (target == AppTarget.CHROME) {
            try {
                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com")).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(browserIntent)
                return ToolResult(
                    success = true,
                    spokenResponse = "Ji, Chrome browser open kar diya hai.",
                    displayMessage = "Opening Web Browser..."
                )
            } catch (_: Exception) {}
        }

        // Fallback for YouTube to web
        if (target == AppTarget.YOUTUBE) {
            try {
                val ytIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com")).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(ytIntent)
                return ToolResult(
                    success = true,
                    spokenResponse = "Ji, YouTube open kar diya hai.",
                    displayMessage = "Opening YouTube..."
                )
            } catch (_: Exception) {}
        }

        // Fallback for Maps
        if (target == AppTarget.MAPS) {
            try {
                val mapIntent = Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=")).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(mapIntent)
                return ToolResult(
                    success = true,
                    spokenResponse = "Ji, Maps open kar diya hai.",
                    displayMessage = "Opening Maps..."
                )
            } catch (_: Exception) {}
        }

        return ToolResult(
            success = false,
            spokenResponse = "$appName is device par installed nahi hai.",
            displayMessage = "$appName is not installed on this device."
        )
    }
}

class CameraTool : JarvisTool {
    override val toolId: String = "open_camera"
    override val name: String = "Camera"
    override val requiredPermission: String? = null
    override val requiresConfirmation: Boolean = false

    override suspend fun execute(context: Context, params: Map<String, Any?>): ToolResult {
        return try {
            val intent = Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            ToolResult(
                success = true,
                spokenResponse = "Camera open kar diya hai.",
                displayMessage = "Opening camera..."
            )
        } catch (_: Exception) {
            try {
                val fallback = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(fallback)
                ToolResult(
                    success = true,
                    spokenResponse = "Camera open kar diya hai.",
                    displayMessage = "Opening camera..."
                )
            } catch (e: Exception) {
                ToolResult(
                    success = false,
                    spokenResponse = "Camera open nahi ho saka: ${e.message}",
                    displayMessage = "Unable to open camera: ${e.message}"
                )
            }
        }
    }
}

class MapsNavigationTool : JarvisTool {
    override val toolId: String = "open_maps"
    override val name: String = "Maps & Route"
    override val requiredPermission: String? = null
    override val requiresConfirmation: Boolean = false

    override suspend fun execute(context: Context, params: Map<String, Any?>): ToolResult {
        val destination = params["destination"] as? String ?: "home"
        val cleanDest = if (destination.equals("ghar", ignoreCase = true) || destination.equals("home", ignoreCase = true)) {
            "Home"
        } else {
            destination
        }

        return try {
            val uri = Uri.parse("google.navigation:q=${Uri.encode(cleanDest)}")
            val mapIntent = Intent(Intent.ACTION_VIEW, uri).apply {
                setPackage("com.google.android.apps.maps")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (mapIntent.resolveActivity(context.packageManager) != null) {
                context.startActivity(mapIntent)
            } else {
                val webUri = Uri.parse("https://www.google.com/maps/dir/?api=1&destination=${Uri.encode(cleanDest)}")
                val webIntent = Intent(Intent.ACTION_VIEW, webUri).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(webIntent)
            }

            ToolResult(
                success = true,
                spokenResponse = "Google Maps mein $cleanDest ka route dikha raha hoon.",
                displayMessage = "Navigating to $cleanDest via Maps..."
            )
        } catch (e: Exception) {
            ToolResult(
                success = false,
                spokenResponse = "Maps open nahi ho saka: ${e.message}",
                displayMessage = "Maps navigation error: ${e.message}"
            )
        }
    }
}

class WebSearchTool : JarvisTool {
    override val toolId: String = "web_search"
    override val name: String = "Web Search"
    override val requiredPermission: String? = null
    override val requiresConfirmation: Boolean = false

    override suspend fun execute(context: Context, params: Map<String, Any?>): ToolResult {
        val query = params["query"] as? String ?: return ToolResult(
            success = false,
            spokenResponse = "Aap kya search karna chahte hain?",
            displayMessage = "Search query missing."
        )

        return try {
            val intent = Intent(Intent.ACTION_WEB_SEARCH).apply {
                putExtra(SearchManager.QUERY, query)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            ToolResult(
                success = true,
                spokenResponse = "I'll check the latest information. Web par '$query' search kar raha hoon.",
                displayMessage = "Searching web for '$query'..."
            )
        } catch (_: Exception) {
            val browserIntent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://www.google.com/search?q=${Uri.encode(query)}")
            ).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
            context.startActivity(browserIntent)
            ToolResult(
                success = true,
                spokenResponse = "I'll check the latest information. Browser mein '$query' search kar raha hoon.",
                displayMessage = "Searching web for '$query'..."
            )
        }
    }
}
