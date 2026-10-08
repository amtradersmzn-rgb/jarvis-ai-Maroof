package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.JarvisDatabase
import com.example.data.TrustedContactEntity
import com.example.engine.AppTarget
import com.example.engine.IntentClassifier
import com.example.engine.JarvisIntent
import com.example.engine.SettingsType
import com.example.engine.VolumeDirection
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
        assertEquals("JARVIS V3", appName)
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
        val alarm = classifier.classify("Jarvis, kal subah 7 baje mujhe utha dena.")
        assertTrue(alarm is JarvisIntent.SetAlarm && (alarm as JarvisIntent.SetAlarm).hour == 7)

        // 5. Battery
        val battery = classifier.classify("Jarvis, meri battery kitni hai?")
        assertTrue(battery is JarvisIntent.BatteryStatus)

        // 6. Bluetooth
        val bt = classifier.classify("Jarvis, Bluetooth settings kholo.")
        assertTrue(bt is JarvisIntent.OpenSetting && (bt as JarvisIntent.OpenSetting).type == SettingsType.BLUETOOTH)

        // 7. Flashlight / Torch
        val torchOn = classifier.classify("Torch jala do")
        assertTrue(torchOn is JarvisIntent.ToggleTorch && (torchOn as JarvisIntent.ToggleTorch).enable)

        val torchOff = classifier.classify("Torch band karo")
        assertTrue(torchOff is JarvisIntent.ToggleTorch && !(torchOff as JarvisIntent.ToggleTorch).enable)

        // 8. Volume Controls
        val volDown = classifier.classify("Volume kam karo")
        assertTrue(volDown is JarvisIntent.AdjustVolume && (volDown as JarvisIntent.AdjustVolume).direction == VolumeDirection.DOWN)

        val volUp = classifier.classify("Volume badhao")
        assertTrue(volUp is JarvisIntent.AdjustVolume && (volUp as JarvisIntent.AdjustVolume).direction == VolumeDirection.UP)

        // 9. Weather and Context Retention ("Shaam ko?")
        val weather1 = classifier.classify("Jarvis, Srinagar ka weather batao.")
        assertTrue(weather1 is JarvisIntent.Weather && (weather1 as JarvisIntent.Weather).location.equals("Srinagar", true))

        val weatherFollowUp = classifier.classify("Kal ka?")
        assertTrue(
            weatherFollowUp is JarvisIntent.Weather &&
                    (weatherFollowUp as JarvisIntent.Weather).location.equals("Srinagar", true) &&
                    (weatherFollowUp as JarvisIntent.Weather).isFollowUp
        )
    }

    @Test
    fun `test AI engines and compare commands`() {
        val classifier = IntentClassifier()

        // 1. ChatGPT
        val chatGpt = classifier.classify("ChatGPT se pucho ki 25 square meter kitne square feet hote hain")
        assertTrue(chatGpt is JarvisIntent.AskChatGpt)

        // 2. Gemini
        val gemini = classifier.classify("Gemini se iska answer lo")
        assertTrue(gemini is JarvisIntent.AskGemini)

        // 3. Compare
        val compare = classifier.classify("compare both models for quantum computing")
        assertTrue(compare is JarvisIntent.CompareAi)

        // 4. Summarize Both
        val summarize = classifier.classify("JARVIS, summarize both answers")
        assertTrue(summarize is JarvisIntent.SummarizeComparison)

        // 5. SMS Message
        val sms = classifier.classify("Ahmed ko message bhejo: main 10 minute mein aa raha hoon")
        assertTrue(sms is JarvisIntent.SendSms && (sms as JarvisIntent.SendSms).contactName.equals("Ahmed", true))
    }

    @Test
    fun `test all 14 functional requirements execution and safety`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val actionPlanner = com.example.engine.ActionPlanner(context)

        // 1. SpeechRecognizer availability check: must not crash
        val isSpeechAvailable = android.speech.SpeechRecognizer.isRecognitionAvailable(context)

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

        // 4. "WhatsApp Ammi" execution with WhatsAppTool
        val waResult = com.example.tools.WhatsAppTool().execute(
            context,
            mapOf("contactName" to "Ammi", "isConfirmed" to true)
        )
        assertNotNull(waResult.spokenResponse)

        // 5. "Open Camera" execution
        val camResult = actionPlanner.planAndExecute(JarvisIntent.OpenApp(AppTarget.CAMERA, "Camera"))
        assertNotNull(camResult.spokenResponse)

        // 6. "Open YouTube" execution
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

        // 10. Torch tool execution
        val torchResult = actionPlanner.planAndExecute(JarvisIntent.ToggleTorch(enable = true))
        assertNotNull(torchResult.spokenResponse)

        // 11. Volume tool execution
        val volResult = actionPlanner.planAndExecute(JarvisIntent.AdjustVolume(VolumeDirection.DOWN))
        assertNotNull(volResult.spokenResponse)

        // 12. Validate all navigation screens
        val screens = com.example.ui.ScreenNav.values()
        assertTrue(screens.contains(com.example.ui.ScreenNav.MAIN))
        assertTrue(screens.contains(com.example.ui.ScreenNav.CONTACTS))
        assertTrue(screens.contains(com.example.ui.ScreenNav.COLOROS_HUB))
        assertTrue(screens.contains(com.example.ui.ScreenNav.MEMORY_VAULT))
        assertTrue(screens.contains(com.example.ui.ScreenNav.ROUTINES))
        assertTrue(screens.contains(com.example.ui.ScreenNav.SETUP_WIZARD))
        assertTrue(screens.contains(com.example.ui.ScreenNav.COMPARE))
        assertTrue(screens.contains(com.example.ui.ScreenNav.PERMISSION_CENTER))
        assertTrue(screens.contains(com.example.ui.ScreenNav.HISTORY))
    }

    @Test
    fun `test contact identification and ambiguity handling`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = JarvisDatabase.getInstance(context)
        val contactDao = db.trustedContactDao()

        contactDao.insert(
            TrustedContactEntity(name = "Abdul Mateen", phoneNumber = "+919876500001", isTrusted = false)
        )
        contactDao.insert(
            TrustedContactEntity(name = "Abdul Khan", phoneNumber = "+919876500002", isTrusted = false)
        )
        contactDao.insert(
            TrustedContactEntity(name = "Ammi", phoneNumber = "+919876543210", isTrusted = true)
        )

        // Ambiguous resolution: "Abdul" should find both
        val matches = ContactResolver.resolveContacts(context, "Abdul")
        assertTrue("Expected at least 2 matches for Abdul, found ${matches.size}", matches.size >= 2)

        val callTool = CallTool()
        val ambiguousResult = callTool.execute(
            context,
            mapOf("contactName" to "Abdul", "isConfirmed" to false)
        )
        assertFalse("Ambiguous call must not succeed silently", ambiguousResult.success)
        assertTrue(ambiguousResult.requiresUiInteraction)
        assertTrue(ambiguousResult.spokenResponse.contains("Kaunsa contact?"))
    }

    @Test
    fun `test ColorOSHelper does not recurse or throw StackOverflow`() {
        val isOppo = com.example.engine.ColorOSHelper.isOppoOrColorOS()
        val version = com.example.engine.ColorOSHelper.getColorOsVersion()
        val model = com.example.engine.ColorOSHelper.getDeviceModel()
        assertNotNull(model)
    }

    @Test
    fun `test voice biometrics threshold and verification`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val biometricManager = com.example.engine.VoiceBiometricManager(context)
        biometricManager.loadBiometricProfile()

        // Enrolled owner voice test: similarity score should be >= 85%
        val ownerResult = biometricManager.verifyVoice("Phone lock karo")
        assertTrue("Owner voice should be verified", ownerResult.isVerified)
        assertTrue("Owner similarity score should be >= 85%, was ${ownerResult.similarityScore}", ownerResult.similarityScore >= 85.0f)

        // Simulated guest voice test: similarity score should be < 85% and rejected
        biometricManager.setSimulatedGuestMode(true)
        val guestResult = biometricManager.verifyVoice("Phone unlock karo")
        assertFalse("Guest voice must be rejected", guestResult.isVerified)
        assertTrue("Guest similarity score must be < 85%, was ${guestResult.similarityScore}", guestResult.similarityScore < 85.0f)
        assertTrue(guestResult.failureReason?.contains("85%") == true)
    }

    @Test
    fun `test phone lock and unlock intent classification and security rejection`() {
        val classifier = IntentClassifier()

        // 1. Classification
        val lockCmd = classifier.classify("phone lock karo")
        assertTrue(lockCmd is JarvisIntent.LockPhone)

        val unlockCmd = classifier.classify("phone unlock karo")
        assertTrue(unlockCmd is JarvisIntent.UnlockPhone)

        val directCallCmd = classifier.classify("Abdul ko call karo")
        assertTrue(directCallCmd is JarvisIntent.MakeCall && (directCallCmd as JarvisIntent.MakeCall).contactName.equals("Abdul", true))

        // 2. KeyguardUnlockHelper lockPhone security check
        val context = ApplicationProvider.getApplicationContext<Context>()
        val rejectLock = com.example.engine.KeyguardUnlockHelper.lockPhone(context, biometricScore = 70.0f)
        assertTrue(rejectLock.contains("rejected for security"))

        // 3. KeyguardUnlockHelper unlockPhone security check
        var unlockRejected = false
        com.example.engine.KeyguardUnlockHelper.unlockPhone(
            activity = null,
            biometricScore = 65.0f,
            onDismissed = {},
            onError = { err ->
                if (err.contains("rejected for security")) unlockRejected = true
            }
        )
        assertTrue("Unlock with score < 85% must be rejected", unlockRejected)
    }
}
