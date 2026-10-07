package com.example.engine

import java.util.regex.Pattern

class IntentClassifier {

    // Context tracking for multi-turn conversation
    var lastLocation: String? = null
    var lastContact: String? = null
    var lastTopic: String? = null

    fun classify(rawInput: String): JarvisIntent {
        var text = rawInput.trim().lowercase()

        // Strip ending punctuation (. , ? !)
        text = text.replace("[.,?!]+$".toRegex(), "").trim()

        // Strip wake-words if present
        text = text.replace("^(hey |ok |okay |sunona |suno )?jarvis[,]?[ ]*".toRegex(), "")
            .replace("^[a-z]+,? jarvis[,]?[ ]*".toRegex(), "")
            .replace("[,?!]+$".toRegex(), "")
            .trim()

        if (text.isEmpty() || text == "hello" || text == "hi" || text == "hey" || text == "suno" ||
            text.contains("hello jarvis") || text.contains("hi jarvis") || text.contains("namaste") || text.contains("salaam")) {
            return JarvisIntent.GeneralChat("Hello! Main JARVIS hoon. Main aapki kya madad kar sakta hoon?")
        }

        // 1. Contextual follow-up check: e.g. "shaam ko?", "kal shaam ko?", "evening?", "tomorrow?"
        if (lastTopic == "weather" && lastLocation != null) {
            if (text.contains("shaam") || text.contains("evening") || text.contains("dopahar") ||
                text.contains("afternoon") || text.contains("raat") || text.contains("night") ||
                text.contains("subah") || text.contains("morning") || text.contains("kal") || text.contains("tomorrow")) {
                return JarvisIntent.Weather(
                    location = lastLocation!!,
                    timeFrame = text,
                    isFollowUp = true
                )
            }
        }

        // 2. Battery query
        if (text.contains("battery") || text.contains("charge") || text.contains("charging")) {
            if (text.contains("kitni") || text.contains("status") || text.contains("level") ||
                text.contains("batao") || text.contains("check") || text.contains("percent")) {
                lastTopic = "battery"
                return JarvisIntent.BatteryStatus
            }
        }

        // 2b. Time query
        // e.g. "Time kya hua hai", "Kya time hua hai", "Time batao", "What time is it"
        if (text.contains("time") || text.contains("samay") || text.contains("waqt") || text.contains("baje hai") || text.contains("ghadi")) {
            if (text.contains("kya") || text.contains("kitna") || text.contains("batao") || text.contains("what") || text == "time") {
                return JarvisIntent.CurrentTime
            }
        }

        // 3. Device info / Model
        if ((text.contains("device") || text.contains("phone") || text.contains("model")) &&
            (text.contains("info") || text.contains("kounsa") || text.contains("batao") || text.contains("specs") || text.contains("details"))) {
            lastTopic = "device"
            return JarvisIntent.DeviceInfo
        }

        // 4. WhatsApp: Sending message
        // e.g. "Ali ko WhatsApp par message bhejo: main 10 minute mein aa raha hoon"
        // or "WhatsApp par Ali ko message karo: hello"
        val whatsappMsgMatch = Regex("(?:whatsapp (?:par|pe)? )?([a-zA-Z0-9_ ]+?) (?:ko )?(?:whatsapp (?:par|pe)? )?(?:message|msg) (?:bhejo|karo|send karo|likho)[: ]+(.*)").find(text)
            ?: Regex("(?:send|write) (?:a )?whatsapp (?:message )?to ([a-zA-Z0-9_ ]+)[: ]+(.*)").find(text)
        if (whatsappMsgMatch != null) {
            val contact = whatsappMsgMatch.groupValues[1].replace("whatsapp", "").trim()
            val msg = whatsappMsgMatch.groupValues[2].trim()
            lastTopic = "whatsapp"
            lastContact = contact
            return JarvisIntent.OpenWhatsApp(contactName = contact, message = msg, isConfirmed = false)
        }

        // 5. WhatsApp: Open chat with contact or open app
        // e.g. "WhatsApp Ammi", "WhatsApp Ali", "WhatsApp kholo aur Ali ki chat open karo"
        if (text.contains("whatsapp")) {
            val directChatMatch = Regex("^whatsapp ([a-zA-Z0-9_ ]+)").find(text)
                ?: Regex("(?:open )?whatsapp (?:chat )?(?:with |for |to )?([a-zA-Z0-9_ ]+)").find(text)
                ?: Regex("(?:aur )?([a-zA-Z0-9_]+) (?:ki )?chat (?:open|kholo)").find(text)
                ?: Regex("([a-zA-Z0-9_]+) (?:ko|ki) whatsapp (?:par|kholo)").find(text)
            if (directChatMatch != null) {
                var contact = directChatMatch.groupValues[1].trim()
                contact = contact.replace("^(chat with|chat for|chat to|open|kholo|par|pe) ".toRegex(), "")
                    .replace(" (kholo|open|chat|open karo|khol do)$".toRegex(), "")
                    .trim()
                if (contact.isNotEmpty() && contact != "kholo" && contact != "open" && contact != "app") {
                    lastTopic = "whatsapp"
                    lastContact = contact
                    return JarvisIntent.OpenWhatsApp(contactName = contact, message = null, isConfirmed = false)
                }
            }
            // General WhatsApp open
            lastTopic = "whatsapp"
            return JarvisIntent.OpenApp(AppTarget.WHATSAPP, "WhatsApp")
        }

        // 6. Calling
        // e.g. "Call Ali", "Ammi ko call karo", "Jarvis, Maroof ko call karo", "Call to Ali"
        val callMatch = Regex("([a-zA-Z0-9_ ]+?) (?:ko )?(?:call|phone) (?:lagao|karo|milao)").find(text)
            ?: Regex("^(?:call|phone|dial) (?:to |karo )?([a-zA-Z0-9_ ]+)").find(text)
            ?: Regex("^(?:call|phone) ([a-zA-Z0-9_ ]+)").find(text)
        if (callMatch != null) {
            var contact = callMatch.groupValues[1].replace("ko", "").trim()
            contact = contact.replace(" (lagao|karo|milao)$".toRegex(), "").trim()
            if (contact.isNotEmpty() && !contact.contains("setting") && contact != "karo") {
                lastTopic = "call"
                lastContact = contact
                return JarvisIntent.MakeCall(contactName = contact)
            }
        }

        // 7. Alarms
        // e.g. "kal subah 7 baje alarm laga do", "7 baje ka alarm", "set alarm for 6 am"
        if (text.contains("alarm")) {
            var hour = 7
            var minute = 0
            val numMatch = Regex("(\\d{1,2})(?::(\\d{2}))?").find(text)
            if (numMatch != null) {
                hour = numMatch.groupValues[1].toIntOrNull() ?: 7
                minute = numMatch.groupValues[2].toIntOrNull() ?: 0
                if ((text.contains("shaam") || text.contains("raat") || text.contains("pm")) && hour < 12) {
                    hour += 12
                }
            }
            lastTopic = "alarm"
            return JarvisIntent.SetAlarm(hour = hour, minute = minute, message = "JARVIS Morning Alarm")
        }

        // 8. Timers
        // e.g. "Set timer for 10 minutes", "10 minute ka timer lagao", "5 min timer"
        if (text.contains("timer")) {
            var seconds = 600
            val numMatch = Regex("(\\d+)\\s*(?:minute|min|sec|second)?").find(text)
            if (numMatch != null) {
                val value = numMatch.groupValues[1].toIntOrNull() ?: 10
                seconds = if (text.contains("sec")) value else value * 60
            }
            lastTopic = "timer"
            return JarvisIntent.SetTimer(seconds = seconds, message = "JARVIS Timer")
        }

        // 9. Weather
        // e.g. "Srinagar ka weather batao", "weather today", "kal Srinagar ka weather kaisa rahega"
        if (text.contains("weather") || text.contains("mausam") || text.contains("taapmaan") || text.contains("temperature")) {
            var location = "Srinagar"
            val locMatch = Regex("([a-zA-Z]+) (?:ka|ke|in|mein|me) (?:weather|mausam)").find(text)
                ?: Regex("(?:weather|mausam) (?:in|of)? ([a-zA-Z]+)").find(text)
            if (locMatch != null) {
                val extracted = locMatch.groupValues[1].trim()
                if (extracted != "kal" && extracted != "aaj" && extracted != "batao" && extracted != "kaisa") {
                    location = extracted
                }
            }
            lastTopic = "weather"
            lastLocation = location
            return JarvisIntent.Weather(location = location, timeFrame = text, isFollowUp = false)
        }

        // 10. Maps / Route
        // e.g. "Open Maps", "Maps kholo", "Google Maps mein ghar ka route dikhao", "Route to airport"
        if (text == "open maps" || text == "maps kholo" || text == "maps" || text == "google maps" || text == "open google maps") {
            lastTopic = "maps"
            return JarvisIntent.OpenApp(AppTarget.MAPS, "Google Maps")
        }
        if (text.contains("map") || text.contains("route") || text.contains("navigation") || text.contains("rasta")) {
            val destMatch = Regex("([a-zA-Z0-9_ ]+) (?:ka|ke) route").find(text)
                ?: Regex("(?:to|for) ([a-zA-Z0-9_ ]+)").find(text)
            val dest = destMatch?.groupValues?.get(1)?.trim() ?: "Ghar"
            lastTopic = "maps"
            return JarvisIntent.NavigateMaps(destination = dest)
        }

        // 11. Camera
        // e.g. "Open Camera", "Camera kholo", "photo", "selfie"
        if (text.contains("camera") || text.contains("photo") || text.contains("selfie")) {
            lastTopic = "camera"
            return JarvisIntent.OpenApp(AppTarget.CAMERA, "Camera")
        }

        // 12. Settings
        // e.g. "Open Settings", "Settings kholo", "Bluetooth settings"
        if (text.contains("bluetooth")) {
            return JarvisIntent.OpenSetting(SettingsType.BLUETOOTH)
        }
        if (text.contains("wifi") || text.contains("wi-fi")) {
            return JarvisIntent.OpenSetting(SettingsType.WIFI)
        }
        if (text.contains("display") || text.contains("brightness")) {
            return JarvisIntent.OpenSetting(SettingsType.DISPLAY)
        }
        if (text.contains("sound") || text.contains("volume") || text.contains("aawaz")) {
            return JarvisIntent.OpenSetting(SettingsType.SOUND)
        }
        if (text.contains("setting") || text.contains("settings")) {
            return JarvisIntent.OpenSetting(SettingsType.GENERAL)
        }

        // 13. Notifications
        if (text.contains("notification") || text.contains("notifications") || text.contains("notif")) {
            lastTopic = "notifications"
            return JarvisIntent.NotificationSummary
        }

        // 14. Specific Apps
        if (text.contains("youtube")) {
            return JarvisIntent.OpenApp(AppTarget.YOUTUBE, "YouTube")
        }
        if (text.contains("instagram") || text.contains("insta")) {
            return JarvisIntent.OpenApp(AppTarget.INSTAGRAM, "Instagram")
        }
        if (text.contains("chrome") || text.contains("browser")) {
            return JarvisIntent.OpenApp(AppTarget.CHROME, "Chrome")
        }
        if (text.contains("gmail") || text.contains("email") || text.contains("mail")) {
            return JarvisIntent.OpenApp(AppTarget.GMAIL, "Gmail")
        }

        // 15. Routines
        if (text.contains("work mode")) {
            return JarvisIntent.TriggerRoutine("Work Mode")
        }
        if (text.contains("good night") || text.contains("shubh ratri") || text.contains("sleep routine")) {
            return JarvisIntent.TriggerRoutine("Good Night")
        }

        // 16. Web search fallback
        if (text.startsWith("search") || text.contains("search karo") || text.contains("google karo")) {
            val q = text.replace("search karo", "").replace("search", "").replace("google karo", "").trim()
            return JarvisIntent.WebSearch(q)
        }

        // 17. Assistant Identity / Who are you?
        if (text.contains("who are you") || text.contains("kaun ho") || text.contains("kon ho") || text.contains("tum kaun ho")) {
            return JarvisIntent.GeneralChat("Main JARVIS hoon — aapka personal AI mobile assistant, specifically optimized for OPPO Reno14 5G aur ColorOS 16.")
        }

        // General chat / Fallback
        return JarvisIntent.GeneralChat(rawInput)
    }
}
