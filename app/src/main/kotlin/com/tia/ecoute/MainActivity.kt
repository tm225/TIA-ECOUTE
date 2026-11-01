package com.tia.ecoute

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import com.tia.ecoute.ui.TiaRoot
import com.tia.ecoute.ui.TiaTheme

class MainActivity : ComponentActivity() {

    private val vm: MainViewModel by viewModels()

    private val permLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            vm.hasPermission = hasAudioPermission()
            if (vm.hasPermission) vm.refresh()
        }

    private fun audioPermission(): String =
        if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_AUDIO
        else Manifest.permission.READ_EXTERNAL_STORAGE

    private fun hasAudioPermission(): Boolean =
        ContextCompat.checkSelfPermission(this, audioPermission()) == PackageManager.PERMISSION_GRANTED

    private fun requestPermissions() {
        val list = mutableListOf(audioPermission())
        if (Build.VERSION.SDK_INT >= 33) list.add(Manifest.permission.POST_NOTIFICATIONS)
        permLauncher.launch(list.toTypedArray())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )
        vm.hasPermission = hasAudioPermission()
        vm.connect()
        if (vm.hasPermission) vm.refresh()
        setContent {
            TiaTheme {
                TiaRoot(vm) { requestPermissions() }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        vm.hasPermission = hasAudioPermission()
        if (vm.hasPermission) vm.refresh()
    }

    override fun onStop() {
        vm.persist()
        super.onStop()
    }
}
