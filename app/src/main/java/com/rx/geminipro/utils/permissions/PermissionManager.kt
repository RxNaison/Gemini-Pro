package com.rx.geminipro.utils.permissions

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat


private const val PERMISSION_PREFS_NAME = "gemini_pro_permission_prefs"
private const val KEY_DENIAL_TOAST_SHOWN_ONCE = "denial_toast_shown_once"

@Composable
fun GetPermissions(context: Context)
{
    val basePermissions = mutableListOf(
        android.Manifest.permission.CAMERA,
        android.Manifest.permission.MODIFY_AUDIO_SETTINGS,
        android.Manifest.permission.RECORD_AUDIO
    )

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        basePermissions.add(android.Manifest.permission.POST_NOTIFICATIONS)
    }

    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
        basePermissions.add(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
    }

    val requiredPermissions = basePermissions.toTypedArray()

    var allPermissionsCurrentlyGranted by remember {
        mutableStateOf(false)
    }

    val prefs = remember(context) {
        context.getSharedPreferences(PERMISSION_PREFS_NAME, Context.MODE_PRIVATE)
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissionsResult ->
        val allPermissionsWereGrantedInThisDialog = permissionsResult.values.all { it }
        allPermissionsCurrentlyGranted = allPermissionsWereGrantedInThisDialog

        if (!allPermissionsWereGrantedInThisDialog) {
            val toastAlreadyShownForDenialCycle = prefs.getBoolean(KEY_DENIAL_TOAST_SHOWN_ONCE, false)
            if (!toastAlreadyShownForDenialCycle) {
                Toast.makeText(context, "You haven't granted all the necessary permissions", Toast.LENGTH_SHORT).show()
                prefs.edit().putBoolean(KEY_DENIAL_TOAST_SHOWN_ONCE, true).apply()
            }
        } else {
            if (prefs.getBoolean(KEY_DENIAL_TOAST_SHOWN_ONCE, false)) {
                prefs.edit().putBoolean(KEY_DENIAL_TOAST_SHOWN_ONCE, false).apply()
            }
        }
    }

    LaunchedEffect(key1 = Unit) {
        val doWeCurrentlyHaveAllPermissions = requiredPermissions.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
        allPermissionsCurrentlyGranted = doWeCurrentlyHaveAllPermissions

        if (!doWeCurrentlyHaveAllPermissions) {
            permissionLauncher.launch(requiredPermissions)
        } else {
            if (prefs.getBoolean(KEY_DENIAL_TOAST_SHOWN_ONCE, false)) {
                prefs.edit().putBoolean(KEY_DENIAL_TOAST_SHOWN_ONCE, false).apply()
            }
        }
    }
}