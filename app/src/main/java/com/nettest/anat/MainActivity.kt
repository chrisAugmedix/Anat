package com.nettest.anat

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.os.PowerManager
import android.util.Log
import android.view.KeyEvent
import android.view.WindowInsetsController
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.nettest.anat.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private val TAG = "MainActivity"

    private lateinit var binding: ActivityMainBinding
    private val navController by lazy {
        (supportFragmentManager.findFragmentById(R.id.nav_host_fragment_container) as NavHostFragment).navController
    }

    private var androidPermissions = arrayOf(
        android.Manifest.permission.ACCESS_FINE_LOCATION,
        android.Manifest.permission.ACCESS_BACKGROUND_LOCATION,
        android.Manifest.permission.ACCESS_COARSE_LOCATION,
        android.Manifest.permission.ACCESS_WIFI_STATE,
        android.Manifest.permission.INTERNET,
        android.Manifest.permission.READ_PHONE_STATE,
        android.Manifest.permission.FOREGROUND_SERVICE,
        android.Manifest.permission.CHANGE_WIFI_STATE,
        android.Manifest.permission.WAKE_LOCK,
        android.Manifest.permission.RECEIVE_BOOT_COMPLETED,
        android.Manifest.permission.POST_NOTIFICATIONS
    )

    override fun onStart() {
        requestPermissions(androidPermissions, 101)
        super.onStart()
    }

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        window.insetsController?.setSystemBarsAppearance(
            WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS,
            WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
        )

        registerReceiver(LockButtonReceiver(), IntentFilter(Intent.ACTION_SCREEN_OFF))

        ActivityCompat.requestPermissions(this, androidPermissions, 101)
        val navView: BottomNavigationView = findViewById(R.id.nav_view)
        navView.setupWithNavController(navController)


    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {

        Log.d(TAG, "onKeyDown: $keyCode")
        if (!global_testingState) return super.onKeyDown(keyCode, event)
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            return true
        }
        return super.onKeyDown(keyCode, event)
    }




    
}

class LockButtonReceiver: BroadcastReceiver() {
    override fun onReceive(p0: Context?, p1: Intent?) {
        if (p1?.action.equals(Intent.ACTION_SCREEN_OFF)) {
            if (global_testingState) {
                val powerManager = p0?.getSystemService(Context.POWER_SERVICE) as PowerManager?
                powerManager?.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP, "Anat: Wakeup")?.acquire(1000)
            }
        }
    }

}