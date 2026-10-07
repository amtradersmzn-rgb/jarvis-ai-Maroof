package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.JarvisDatabase
import com.example.data.TrustedContactEntity
import com.example.engine.AppTarget
import com.example.engine.IntentClassifier
import com.example.engine.JarvisIntent
import com.example.engine.SettingsType
import com.example.tools.CallTool
import com.example.tools.ContactMatch
import com.example.tools.ContactResolver
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("JARVIS AI", appName)
    }

    @Test
    fun `test intent classifier for Hindi Hinglish commands`() {
        val classifier = IntentClassifier()

        // 1. YouTube kholo
        val yt = classifier.classify("Hey Jarvis, YouTube kholo.")
        assertTrue(yt is JarvisIntent.OpenApp && (yt as JarvisIntent.OpenApp).target == AppTarget.YOUTUBE)

        // 2. Call Ammi
        val call = classifier.classify("Jarvis, Ammi ko call karo.")
        assertTrue(call is JarvisIntent.MakeCall && (call as JarvisIntent.MakeCall).contactName.equals("Ammi", true))

        // 3. Timer
        val timer = classifier.classify("Jarvis, 10 minute ka timer lagao.")
        assertTrue(timer is JarvisIntent.SetTimer && (timer as JarvisIntent.SetTimer).seconds == 600)

        // 4. Alarm
        val alarm = classifier.classify("Jarvis, kal subah 7 baje alarm laga do.")
        assertTrue(alarm is JarvisIntent.SetAlarm && (alarm as JarvisIntent.SetAlarm).hour == 7)

        // 5. Battery
        val battery = classifier.classify("Jarvis, meri battery kitni hai?")
        assertTrue(battery is JarvisIntent.BatteryStatus)

        // 6. Bluetooth
        val bt = classifier.classify("Jarvis, Bluetooth settings kholo.")
        assertTrue(bt is JarvisIntent.OpenSetting && (bt as JarvisIntent.OpenSetting).type == SettingsType.BLUETOOTH)

        // 7. Weather and Context Retention ("Shaam ko?")
        val weather1 = classifier.classify("Jarvis, Srinagar ka weather batao.")
        assertTrue(weather1 is JarvisIntent.Weather && (weather1 as JarvisIntent.Weather).location.equals("Srinagar", true))

        val weatherFollowUp = classifier.classify("Shaam ko?")
        assertTrue(
            weatherFollowUp is JarvisIntent.Weather &&
                    (weatherFollowUp as JarvisIntent.Weather).location.equals("Srinagar", true) &&
                    (weatherFollowUp as JarvisIntent.Weather).isFollowUp
        )
    }

    @Test
    fun `test exact user requested commands`() {
        val classifier = IntentClassifier()

        // 1. "Hello JARVIS"
        val hello = classifier.classify("Hello JARVIS")
        assertTrue(hello is JarvisIntent.GeneralChat)
        assertTrue((hello as JarvisIntent.GeneralChat).query.contains("Hello", ignoreCase = true) ||
                (hello as JarvisIntent.GeneralChat).query.contains("JARVIS", ignoreCase = true) ||
                (hello as JarvisIntent.GeneralChat).query.contains("madad", ignoreCase = true))

        // 2. "Call Ali"
        val call = classifier.classify("Call Ali")
        assertTrue(call is JarvisIntent.MakeCall && (call as JarvisIntent.MakeCall).contactName.equals("Ali", true))

        // 3. "WhatsApp Ammi"
        val wa = classifier.classify("WhatsApp Ammi")
        assertTrue(wa is JarvisIntent.OpenWhatsApp && (wa as JarvisIntent.OpenWhatsApp).contactName.equals("Ammi", true))

        // 4. "Open Camera"
        val cam = classifier.classify("Open Camera")
        assertTrue(cam is JarvisIntent.OpenApp && (cam as JarvisIntent.OpenApp).target == AppTarget.CAMERA)

        // 5. "Open YouTube"
        val yt = classifier.classify("Open YouTube")
        assertTrue(yt is JarvisIntent.OpenApp && (yt as JarvisIntent.OpenApp).target == AppTarget.YOUTUBE)

        // 6. "Open Maps"
        val maps = classifier.classify("Open Maps")
        assertTrue(maps is JarvisIntent.OpenApp && (maps as JarvisIntent.OpenApp).target == AppTarget.MAPS)

        // 7. "Open Settings"
        val settings = classifier.classify("Open Settings")
        assertTrue(settings is JarvisIntent.OpenSetting && (settings as JarvisIntent.OpenSetting).type == SettingsType.GENERAL)

        // 8. "Set timer for 10 minutes"
        val timer = classifier.classify("Set timer for 10 minutes")
        assertTrue(timer is JarvisIntent.SetTimer && (timer as JarvisIntent.SetTimer).seconds == 600)

        // 9. "YouTube kholo"
        val ytKholo = classifier.classify("YouTube kholo")
        assertTrue(ytKholo is JarvisIntent.OpenApp && (ytKholo as JarvisIntent.OpenApp).target == AppTarget.YOUTUBE)

        // 10. "WhatsApp kholo"
        val waKholo = classifier.classify("WhatsApp kholo")
        assertTrue(waKholo is JarvisIntent.OpenApp && (waKholo as JarvisIntent.OpenApp).target == AppTarget.WHATSAPP)

        // 11. "Chrome kholo"
        val chromeKholo = classifier.classify("Chrome kholo")
        assertTrue(chromeKholo is JarvisIntent.OpenApp && (chromeKholo as JarvisIntent.OpenApp).target == AppTarget.CHROME)

        // 12. "Mera battery percentage batao"
        val battQuery = classifier.classify("Mera battery percentage batao")
        assertTrue(battQuery is JarvisIntent.BatteryStatus)

        // 13. "Time kya hua hai"
        val timeQuery = classifier.classify("Time kya hua hai")
        assertTrue(timeQuery is JarvisIntent.CurrentTime)

        // 14. "Ammi ko call karo"
        val callAmmi = classifier.classify("Ammi ko call karo")
        assertTrue(callAmmi is JarvisIntent.MakeCall && (callAmmi as JarvisIntent.MakeCall).contactName.equals("Ammi", true))
    }

    @Test
    fun `test all 14 functional requirements execution and safety`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val actionPlanner = com.example.engine.ActionPlanner(context)

        // 1. SpeechRecognizer availability check: must not crash
        val isSpeechAvailable = android.speech.SpeechRecognizer.isRecognitionAvailable(context)
        // Passes without crash

        // 2. "Hello JARVIS" action planner execution
        val helloResult = actionPlanner.planAndExecute(JarvisIntent.GeneralChat("Hello! Main JARVIS hoon."))
        assertTrue(helloResult.success)
        assertTrue(helloResult.spokenResponse.isNotEmpty())

        // 3. "Call Ali" execution with CallTool: ACTION_DIAL first
        val callResult = com.example.tools.CallTool().execute(
            context,
            mapOf("contactName" to "Ali", "phoneNumber" to "+919876500001", "isConfirmed" to true)
        )
        assertTrue(callResult.success)
        assertTrue(callResult.spokenResponse.contains("dialer open"))

        // 4. "WhatsApp Ammi" execution with WhatsAppTool (handles missing WhatsApp gracefully)
        val waResult = com.example.tools.WhatsAppTool().execute(
            context,
            mapOf("contactName" to "Ammi", "isConfirmed" to true)
        )
        // On emulator or device without WhatsApp, reports missing app gracefully without crash
        assertNotNull(waResult.spokenResponse)

        // 5. "Open Camera" execution (handles missing camera gracefully)
        val camResult = actionPlanner.planAndExecute(JarvisIntent.OpenApp(AppTarget.CAMERA, "Camera"))
        assertNotNull(camResult.spokenResponse)

        // 6. "Open YouTube" execution (handles missing app gracefully)
        val ytResult = actionPlanner.planAndExecute(JarvisIntent.OpenApp(AppTarget.YOUTUBE, "YouTube"))
        assertNotNull(ytResult.spokenResponse)

        // 7. "Open Maps" execution
        val mapsResult = actionPlanner.planAndExecute(JarvisIntent.OpenApp(AppTarget.MAPS, "Google Maps"))
        assertNotNull(mapsResult.spokenResponse)

        // 8. "Open Settings" execution
        val settingsResult = actionPlanner.planAndExecute(JarvisIntent.OpenSetting(SettingsType.GENERAL))
        assertTrue(settingsResult.success)

        // 9. "Set timer for 10 minutes" execution
        val timerResult = actionPlanner.planAndExecute(JarvisIntent.SetTimer(seconds = 600, message = "JARVIS Timer"))
        assertNotNull(timerResult.spokenResponse)

        // 9b. "Time kya hua hai" execution
        val timeResult = actionPlanner.planAndExecute(JarvisIntent.CurrentTime)
        assertTrue(timeResult.success)
        assertTrue(timeResult.spokenResponse.contains("time", ignoreCase = true))

        // 10-14. Validate all navigation screens are distinct and defined
        val screens = com.example.ui.ScreenNav.values()
        assertTrue(screens.contains(com.example.ui.ScreenNav.MAIN))
        assertTrue(screens.contains(com.example.ui.ScreenNav.CONTACTS))     // 10. Contacts screen
        assertTrue(screens.contains(com.example.ui.ScreenNav.COLOROS_HUB))   // 11. ColorOS screen
        assertTrue(screens.contains(com.example.ui.ScreenNav.MEMORY_VAULT))  // 12. Vault
        assertTrue(screens.contains(com.example.ui.ScreenNav.ROUTINES))      // 13. Routines
        assertTrue(screens.contains(com.example.ui.ScreenNav.SETUP_WIZARD))  // 14. Wizard
    }

    @Test
    fun `test contact identification and ambiguity handling`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = JarvisDatabase.getInstance(context)
        val contactDao = db.trustedContactDao()

        // Seed two contacts with same prefix "Ali"
        contactDao.insert(
            TrustedContactEntity(name = "Ali Khan", phoneNumber = "+919876500001", isTrusted = false)
        )
        contactDao.insert(
            TrustedContactEntity(name = "Ali Ahmad", phoneNumber = "+919876500002", isTrusted = false)
        )
        // Seed trusted contact
        contactDao.insert(
            TrustedContactEntity(name = "Ammi", phoneNumber = "+919876543210", isTrusted = true)
        )

        // 1. Ambiguous resolution: "Ali" should find both Ali Khan and Ali Ahmad
        val matches = ContactResolver.resolveContacts(context, "Ali")
        assertTrue("Expected at least 2 matches for Ali, found ${matches.size}", matches.size >= 2)

        // 2. CallTool execution with ambiguous name: MUST NOT place call silently
        val callTool = CallTool()
        val ambiguousResult = callTool.execute(
            context,
            mapOf("contactName" to "Ali", "isConfirmed" to false)
        )
        assertFalse("Ambiguous call must not succeed silently", ambiguousResult.success)
        assertTrue(ambiguousResult.requiresUiInteraction)
        assertTrue(ambiguousResult.spokenResponse.contains("Kaunsa contact?"))

        // 3. Call with single contact that is NOT confirmed and NOT trusted bypass:
        // Must prompt confirmation
        val unconfirmedResult = callTool.execute(
            context,
            mapOf("contactName" to "Ali Khan", "isConfirmed" to false, "isTrustedBypass" to false)
        )
        assertFalse("Unconfirmed call must require user confirmation", unconfirmedResult.success)
        assertTrue(unconfirmedResult.requiresUiInteraction)
        assertTrue(unconfirmedResult.spokenResponse.contains("Ali Khan ko call lagau?"))

        // 4. Call with single contact when confirmed OR trusted bypass:
        // Places call safely
        val confirmedResult = callTool.execute(
            context,
            mapOf("contactName" to "Ammi", "isConfirmed" to true, "isTrustedBypass" to true)
        )
        assertTrue("Confirmed or trusted call should succeed", confirmedResult.success)
    }

    @Test
    fun `test ColorOSHelper does not recurse or throw StackOverflow`() {
        // Must return safely without recursive stack overflow
        val isOppo = com.example.engine.ColorOSHelper.isOppoOrColorOS()
        val version = com.example.engine.ColorOSHelper.getColorOsVersion()
        val model = com.example.engine.ColorOSHelper.getDeviceModel()
        assertNotNull(model)
    }
}
