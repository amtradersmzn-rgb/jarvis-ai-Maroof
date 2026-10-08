package com.example.engine

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

        // Introduction prompt
        if (text.contains("introduce yourself") || text.contains("apna introduction do") || text.contains("introduce karo")) {
            return JarvisIntent.GeneralChat("Hello. I am JARVIS, your personal AI assistant. I'm ready to help.")
        }

        // Compare Summarization command
        if (text.contains("summarize both") || text.contains("summarize both answers") || text.contains("dono answers summarize karo") ||
            text.contains("dono ko summarize karo") || text.contains("summarize answers") || text.contains("dono answer summarize")) {
            return JarvisIntent.SummarizeComparison
        }

        // Explicit Compare command
        if (text.startsWith("compare ") || text.contains("compare both") || text.contains("dono se pucho") || text.contains("chatgpt aur gemini")) {
            val q = text.replace("^(compare both|compare|dono se pucho ki|chatgpt aur gemini se pucho ki|chatgpt aur gemini compare karo)[: ]*".toRegex(), "").trim()
            return JarvisIntent.CompareAi(q.ifBlank { rawInput })
        }

        // Explicit ChatGPT Query
        if (text.startsWith("chatgpt se pucho") || text.startsWith("ask chatgpt") || text.startsWith("chatgpt ") || text.contains("chatgpt se")) {
            val q = text.replace("^(chatgpt se pucho ki|chatgpt se pucho|ask chatgpt to|ask chatgpt|chatgpt)[: ]*".toRegex(), "").trim()
            return JarvisIntent.AskChatGpt(q.ifBlank { rawInput })
        }

        // Explicit Gemini Query
        if (text.startsWith("gemini se pucho") || text.startsWith("ask gemini") || text.startsWith("gemini ") || text.contains("gemini se")) {
            val q = text.replace("^(gemini se pucho ki|gemini se pucho|ask gemini to|ask gemini|gemini)[: ]*".toRegex(), "").trim()
            return JarvisIntent.AskGemini(q.ifBlank { rawInput })
        }

        // 1. Contextual follow-up check (e.g. "kal ka?", "shaam ko?", "tomorrow?", "kal?")
        if (lastTopic == "weather" && lastLocation != null) {
            if (text == "kal ka" || text == "kal ka?" || text == "kal" || text == "tomorrow" || text.contains("shaam") ||
                text.contains("evening") || text.contains("dopahar") || text.contains("afternoon") || text.contains("raat") ||
                text.contains("subah") || text.contains("morning")) {
                return JarvisIntent.Weather(
                    location = lastLocation!!,
                    timeFrame = text,
                    isFollowUp = true
                )
            }
        }

        // 1b. Phone Lock & Unlock commands
        if (text == "phone lock karo" || text == "phone lock" || text == "lock phone" || text == "lock screen" || text == "screen lock karo" || text == "screen lock") {
            return JarvisIntent.LockPhone()
        }
        if (text == "phone unlock karo" || text == "phone unlock" || text == "unlock phone" || text == "unlock screen" || text == "screen unlock karo" || text == "screen unlock") {
            return JarvisIntent.UnlockPhone()
        }

        // 2. Flashlight / Torch
        if (text.contains("torch") || text.contains("flashlight") || text.contains("batti")) {
            if (text.contains("off") || text.contains("band") || text.contains("bujha") || text.contains("turn off")) {
                return JarvisIntent.ToggleTorch(enable = false)
            }
            if (text.contains("on") || text.contains("jala") || text.contains("chalu") || text.contains("turn on") || text.contains("start")) {
                return JarvisIntent.ToggleTorch(enable = true)
            }
        }

        // 3. Volume controls
        if (text.contains("volume") || text.contains("aawaz")) {
            if (text.contains("kam") || text.contains("ghatao") || text.contains("down") || text.contains("lower") || text.contains("slow")) {
                return JarvisIntent.AdjustVolume(VolumeDirection.DOWN)
            }
            if (text.contains("badhao") || text.contains("tez") || text.contains("up") || text.contains("increase") || text.contains("raise") || text.contains("zyada") || text.contains("jyada")) {
                return JarvisIntent.AdjustVolume(VolumeDirection.UP)
            }
            if (text.contains("mute") || text.contains("silent") || text.contains("chup")) {
                return JarvisIntent.AdjustVolume(VolumeDirection.MUTE)
            }
        }

        // 4. Battery query
        if (text.contains("battery") || text.contains("charge") || text.contains("charging")) {
            if (text.contains("kitni") || text.contains("status") || text.contains("level") ||
                text.contains("batao") || text.contains("check") || text.contains("percent")) {
                lastTopic = "battery"
                return JarvisIntent.BatteryStatus
            }
        }

        // 5. Time query
        if (text.contains("time") || text.contains("samay") || text.contains("waqt") || text.contains("baje hai") || text.contains("ghadi")) {
            if (text.contains("kya") || text.contains("kitna") || text.contains("batao") || text.contains("what") || text == "time") {
                return JarvisIntent.CurrentTime
            }
        }

        // 6. Device info / Model
        if ((text.contains("device") || text.contains("phone") || text.contains("model")) &&
            (text.contains("info") || text.contains("kounsa") || text.contains("batao") || text.contains("specs") || text.contains("details"))) {
            lastTopic = "device"
            return JarvisIntent.DeviceInfo
        }

        // 7. Messaging (SMS / General Message)
        val msgMatch = Regex("([a-zA-Z0-9_]+?) (?:ko )?(?:message|sms|sandesh) (?:bhejo|karo|likho)[: ]+(.*)").find(text)
            ?: Regex("^(?:send |write )?(?:a )?(?:message|sms) (?:to )?([a-zA-Z0-9_]+?)[: ]+(?:ki )?(.*)").find(text)
        if (msgMatch != null && !text.contains("whatsapp")) {
            val contact = msgMatch.groupValues[1].replace("ko", "").trim()
            val msg = msgMatch.groupValues[2].trim()
            lastTopic = "sms"
            lastContact = contact
            return JarvisIntent.SendSms(contactName = contact, message = msg, isConfirmed = false)
        }

        // 8. WhatsApp: Sending message
        val whatsappMsgMatch = Regex("(?:whatsapp (?:par|pe)? )?([a-zA-Z0-9_]+?) (?:ko )?(?:whatsapp (?:par|pe)? )?(?:message|msg) (?:bhejo|karo|send karo|likho)[: ]+(.*)").find(text)
            ?: Regex("^(?:send|write) (?:a )?whatsapp (?:message )?to ([a-zA-Z0-9_]+?)[: ]+(.*)").find(text)
        if (whatsappMsgMatch != null) {
            val contact = whatsappMsgMatch.groupValues[1].replace("whatsapp", "").trim()
            val msg = whatsappMsgMatch.groupValues[2].trim()
            lastTopic = "whatsapp"
            lastContact = contact
            return JarvisIntent.OpenWhatsApp(contactName = contact, message = msg, isConfirmed = false)
        }

        // 9. WhatsApp: Open chat or open app
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
            lastTopic = "whatsapp"
            return JarvisIntent.OpenApp(AppTarget.WHATSAPP, "WhatsApp")
        }

        // 10. Calling
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

        // 11. Alarms
        if (text.contains("alarm") || text.contains("utha dena") || text.contains("jaga dena")) {
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

        // 12. Timers
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

        // 13. Weather
        if (text.contains("weather") || text.contains("mausam") || text.contains("taapmaan") || text.contains("temperature")) {
            var location = "Delhi"
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

        // 14. Smart Home Architecture commands
        if (text.contains("bedroom light") || text.contains("light on") || text.contains("light off") ||
            text.contains("set ac") || text.contains("air conditioner") || text.contains("fan on") || text.contains("fan off")) {
            return JarvisIntent.SmartHome(rawInput)
        }

        // 15. Maps / Route
        if (text == "open maps" || text == "maps kholo" || text == "maps" || text == "google maps" || text == "open google maps") {
            lastTopic = "maps"
            return JarvisIntent.OpenApp(AppTarget.MAPS, "Google Maps")
        }
        if (text.contains("map") || text.contains("route") || text.contains("navigation") || text.contains("rasta")) {
            val destMatch = Regex("([a-zA-Z0-9_ ]+) (?:ka|ke) route").find(text)
                ?: Regex("(?:to|for) ([a-zA-Z0-9_ ]+)").find(text)
            val dest = destMatch?.groupValues?.get(1)?.trim() ?: "Home"
            lastTopic = "maps"
            return JarvisIntent.NavigateMaps(destination = dest)
        }

        // 16. Camera
        if (text.contains("camera") || text.contains("photo") || text.contains("selfie")) {
            lastTopic = "camera"
            return JarvisIntent.OpenApp(AppTarget.CAMERA, "Camera")
        }

        // 17. Settings
        if (text.contains("bluetooth")) {
            return JarvisIntent.OpenSetting(SettingsType.BLUETOOTH)
        }
        if (text.contains("wifi") || text.contains("wi-fi")) {
            return JarvisIntent.OpenSetting(SettingsType.WIFI)
        }
        if (text.contains("display") || text.contains("brightness")) {
            return JarvisIntent.OpenSetting(SettingsType.DISPLAY)
        }
        if (text.contains("sound setting") || text.contains("volume setting")) {
            return JarvisIntent.OpenSetting(SettingsType.SOUND)
        }
        if (text.contains("notification setting") || text.contains("notification listener")) {
            return JarvisIntent.OpenSetting(SettingsType.NOTIFICATION_LISTENER)
        }
        if (text.contains("assistant setting") || text.contains("default assistant")) {
            return JarvisIntent.OpenSetting(SettingsType.DEFAULT_ASSISTANT)
        }
        if (text.contains("setting") || text.contains("settings")) {
            return JarvisIntent.OpenSetting(SettingsType.GENERAL)
        }

        // 18. Notifications
        if (text.contains("notification") || text.contains("notifications") || text.contains("notif")) {
            lastTopic = "notifications"
            return JarvisIntent.NotificationSummary
        }

        // 19. Specific Apps
        if (text.contains("youtube")) {
            return JarvisIntent.OpenApp(AppTarget.YOUTUBE, "YouTube")
        }
        if (text.contains("instagram") || text.contains("insta")) {
            return JarvisIntent.OpenApp(AppTarget.INSTAGRAM, "Instagram")
        }
        if (text.contains("chrome") || text.contains("browser")) {
            return JarvisIntent.OpenApp(AppTarget.CHROME, "Chrome")
        }
        if (text.contains("gallery") || text.contains("photos")) {
            return JarvisIntent.OpenApp(AppTarget.GALLERY, "Gallery")
        }
        if (text.contains("gmail") || text.contains("email") || text.contains("mail")) {
            return JarvisIntent.OpenApp(AppTarget.GMAIL, "Gmail")
        }
        if (text.contains("dialer") || text == "open phone" || text == "phone kholo") {
            return JarvisIntent.OpenApp(AppTarget.PHONE, "Phone")
        }
        if (text == "open messages" || text == "messages kholo") {
            return JarvisIntent.OpenApp(AppTarget.MESSAGES, "Messages")
        }

        // 20. Routines
        if (text.contains("work mode")) {
            return JarvisIntent.TriggerRoutine("Work Mode")
        }
        if (text.contains("good night") || text.contains("shubh ratri") || text.contains("sleep routine")) {
            return JarvisIntent.TriggerRoutine("Good Night")
        }

        // 21. Live Web search
        if (text.startsWith("search") || text.contains("search karo") || text.contains("google karo") ||
            text.contains("today's news") || text.contains("aaj ki news") || text.contains("current news") ||
            text.contains("stock price") || text.contains("cricket score") || text.contains("match score")) {
            val q = text.replace("search karo", "").replace("search", "").replace("google karo", "").trim()
            return JarvisIntent.WebSearch(q.ifBlank { rawInput }, isLiveSearch = true)
        }

        // General chat / Fallback
        return JarvisIntent.GeneralChat(rawInput)
    }
}
