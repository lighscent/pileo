package com.pileo

import android.Manifest
import android.app.AlarmManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.pileo.ui.PileoViewModel
import com.pileo.ui.PileoViewModelFactory
import com.pileo.ui.screens.AddEditScreen
import com.pileo.ui.screens.HomeScreen
import com.pileo.ui.screens.SettingsScreen
import com.pileo.ui.theme.PileoTheme

class MainActivity : ComponentActivity() {

    private lateinit var vm: PileoViewModel
    private var notifGranted by mutableStateOf(true)
    private var exactAlarmGranted by mutableStateOf(true)
    private var batteryUnrestricted by mutableStateOf(true)

    private val notifPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            notifGranted = granted
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        vm = ViewModelProvider(
            this,
            PileoViewModelFactory(application)
        )[PileoViewModel::class.java]

        refreshPermissionStates()
        // Reprogramme les alarmes à chaque démarrage : couvre les cas où le système
        // les a effacées (mise à jour, « forcer l'arrêt » puis réouverture…).
        vm.refreshAlarms()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this, Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        setContent {
            PileoTheme {
                val nav = rememberNavController()
                NavHost(navController = nav, startDestination = "home") {
                    composable("home") {
                        HomeScreen(
                            vm = vm,
                            onAdd = { nav.navigate("edit?medId=0") },
                            onEdit = { id -> nav.navigate("edit?medId=$id") },
                            onSettings = { nav.navigate("settings") },
                            showExactAlarmBanner = !exactAlarmGranted,
                            showNotificationBanner = !notifGranted,
                            showBatteryBanner = !batteryUnrestricted,
                            onRequestExactAlarm = { openExactAlarmSettings() },
                            onRequestNotifications = { openAppSettings() },
                            onRequestBatteryExemption = { openBatteryExemption() }
                        )
                    }
                    composable("settings") {
                        SettingsScreen(onBack = { nav.popBackStack() })
                    }
                    composable(
                        route = "edit?medId={medId}",
                        arguments = listOf(
                            navArgument("medId") {
                                type = NavType.LongType
                                defaultValue = 0L
                            }
                        )
                    ) { backStack ->
                        val id = backStack.arguments?.getLong("medId") ?: 0L
                        AddEditScreen(
                            vm = vm,
                            medId = if (id == 0L) null else id,
                            onDone = { nav.popBackStack() },
                            onBack = { nav.popBackStack() }
                        )
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        refreshPermissionStates()
    }

    private fun refreshPermissionStates() {
        notifGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                this, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else true

        exactAlarmGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val am = getSystemService(ALARM_SERVICE) as AlarmManager
            am.canScheduleExactAlarms()
        } else true

        val pm = getSystemService(POWER_SERVICE) as android.os.PowerManager
        batteryUnrestricted = pm.isIgnoringBatteryOptimizations(packageName)
    }

    private fun openExactAlarmSettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                    data = Uri.parse("package:$packageName")
                })
                return
            } catch (_: Exception) { }
        }
        openAppSettings()
    }

    private fun openBatteryExemption() {
        try {
            startActivity(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:$packageName")
            })
        } catch (_: Exception) {
            openAppSettings()
        }
    }

    private fun openAppSettings() {
        try {
            startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:$packageName")
            })
        } catch (_: Exception) { }
    }
}
