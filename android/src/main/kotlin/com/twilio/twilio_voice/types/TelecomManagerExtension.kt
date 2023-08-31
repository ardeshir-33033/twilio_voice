package com.twilio.twilio_voice.types

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.telecom.PhoneAccount
import android.telecom.PhoneAccountHandle
import android.telecom.TelecomManager
import androidx.annotation.RequiresPermission
import androidx.core.content.PermissionChecker

object TelecomManagerExtension {

    /**
     *  Register a phone account with the system telecom manager
     *  @param ctx application context
     *  @param phoneAccountHandle The handle for the phone account
     *  @param label The label for the phone account
     *  @param shortDescription The short description for the phone account
     */
    @RequiresPermission(value = "android.permission.READ_PHONE_STATE")
    fun TelecomManager.registerPhoneAccount(ctx: Context, phoneAccountHandle: PhoneAccountHandle, label: String, shortDescription: String = "") {
        if (hasCallCapableAccount(ctx, phoneAccountHandle.componentName.className)) {
            // phone account already registered
            return
        }
        // register phone account
        val phoneAccount = PhoneAccount.builder(phoneAccountHandle, label)
            .setCapabilities(PhoneAccount.CAPABILITY_CALL_PROVIDER or PhoneAccount.CAPABILITY_CONNECTION_MANAGER or PhoneAccount.CAPABILITY_CALL_SUBJECT)
            .setShortDescription(shortDescription)
            .addSupportedUriScheme(PhoneAccount.SCHEME_TEL)
            .build()

        registerPhoneAccount(phoneAccount)
    }

    fun TelecomManager.openPhoneAccountSettings(activity: Activity) {
        if (Build.MANUFACTURER.equals("Samsung", ignoreCase = true)) {
            val intent = Intent(TelecomManager.ACTION_CHANGE_PHONE_ACCOUNTS)
            intent.component = ComponentName(
                "com.android.server.telecom",
                "com.android.server.telecom.settings.EnableAccountPreferenceActivity"
            )
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            activity.startActivity(intent, null)
        } else {
            val intent = Intent(TelecomManager.ACTION_CHANGE_PHONE_ACCOUNTS)
            activity.startActivity(intent, null)
        }
    }


    /**
     * Check if a phone account has been registered with the system telecom manager
     * @param ctx application context
     * @param name The name of the componentName Class (i.e. ConnectionService)
     */
    @RequiresPermission(value = "android.permission.READ_PHONE_STATE")
    fun TelecomManager.hasCallCapableAccount(ctx: Context, name: String): Boolean {
        if (!hasReadPhonePermission(ctx)) return false
        return callCapablePhoneAccounts.any { it.componentName.className == name }
    }

    /**
     * Check if the app has the READ_PHONE_STATE permission
     * @param ctx application context
     * @return Boolean True if the app has the READ_PHONE_STATE permission
     */
    fun TelecomManager.hasReadPhonePermission(ctx: Context): Boolean {
        return PermissionChecker.checkSelfPermission(ctx, android.Manifest.permission.READ_PHONE_STATE) == PermissionChecker.PERMISSION_GRANTED
    }

    @RequiresPermission(value = "android.permission.READ_PHONE_STATE")
    fun TelecomManager.isOnCall(ctx: Context): Boolean {
        if (!hasReadPhonePermission(ctx)) return false
        return if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.O) isInCall else isInManagedCall
    }
}