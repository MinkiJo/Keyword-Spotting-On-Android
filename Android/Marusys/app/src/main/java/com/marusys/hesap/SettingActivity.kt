package com.marusys.hesap.presentation

import SettingScreen
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.marusys.hesap.presentation.viewmodel.MainViewModel

class SettingsActivity : ComponentActivity() {
    private val viewModel = MainViewModel()  // ViewModel 인스턴스

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SettingScreen(viewModel = viewModel)
        }
    }
}
