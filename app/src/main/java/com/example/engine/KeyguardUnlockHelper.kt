package com.example.engine

import android.app.Activity
import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.view.WindowManager
import com.example.service.JarvisAccessibilityService

object KeyguardUnlockHelper {

    fun lockPhone(context: Context, biometricScore: Float): String {
        if (biometricScore < VoiceBiometricManager.BIOMETRIC_SIMILARITY_THRESHOLD) {
            return "Voice biometric verification failed (${String.format("%.1f", biometricScore)}% < 85%). Phone lock action rejected for security."
        }

        val success = JarvisAccessibilityService.lockScreen()
        return if (success) {
            "Voice verified (${String.format("%.1f", biometricScore)}%). Phone lock kar diya hai."
        } else {
            "Voice verified, lekin Accessibility Service enable nahi hai. Settings se JARVIS Accessibility Service enable karein."
        }
    }

    fun unlockPhone(activity: Activity?, biometricScore: Float, onDismissed: () -> Unit, onError: (String) -> Unit) {
        if (biometricScore < VoiceBiometricManager.BIOMETRIC_SIMILARITY_THRESHOLD) {
            onError("Voice biometric verification failed (${String.format("%.1f", biometricScore)}% < 85%). Phone unlock action rejected for security.")
            return
        }

        if (activity == null) {
            onError("Activity context unavailable for keyguard dismissal.")
            return
        }

        // Configure Screen ON flags
        activity.window.addFlags(
            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            activity.setShowWhenLocked(true)
            activity.setTurnScreenOn(true)
        }

        val keyguardManager = activity.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
        if (keyguardManager == null) {
            onError("Keyguard service unavailable.")
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            keyguardManager.requestDismissKeyguard(
                activity,
                object : KeyguardManager.KeyguardDismissCallback() {
                    override fun onDismissSucceeded() {
                        super.onDismissSucceeded()
                        onDismissed()
                    }

                    override fun onDismissError() {
                        super.onDismissError()
                        onError("Keyguard dismissal error. Face unlock ya PIN verify karein.")
                    }

                    override fun onDismissCancelled() {
                        super.onDismissCancelled()
                        onError("Keyguard dismissal cancel ho gaya.")
                    }
                }
            )
        } else {
            onDismissed()
        }
    }
}
