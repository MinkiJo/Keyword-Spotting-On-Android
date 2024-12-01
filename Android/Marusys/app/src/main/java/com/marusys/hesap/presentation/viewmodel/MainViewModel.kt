package com.marusys.hesap.presentation.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.marusys.hesap.feature.MemoryUsageManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel : ViewModel() {
    private val memoryUsageManager = MemoryUsageManager()

    private val _resultText = MutableStateFlow("") // viewModel 내에서 함수를 통해서만 수정하기 위해
    val resultText = _resultText.asStateFlow() // 외부에서 바꿔 쓰지 않지만, 읽기 위해 정의

    private val _commandText = MutableStateFlow( "헤이 싸피를 인식했습니다!")
    val commandText: StateFlow<String> = _commandText.asStateFlow()

    // AudioScreen background color
    private val _isAudioServiceRunning = MutableStateFlow(false)
    val isAudioServiceRunning: StateFlow<Boolean> = _isAudioServiceRunning.asStateFlow()

    private val _memoryText = MutableStateFlow("")
    val memoryText: StateFlow<String> = _memoryText.asStateFlow()

    fun setResultText(text : String){
        _resultText.value = text
    }

    fun setCommandText(text : String){
        _commandText.value = text
    }

    fun setAudioServiceRunning(isRunning: Boolean) {
        _isAudioServiceRunning.value = isRunning
    }

    fun updateMemoryUsage(context: Context) {
        viewModelScope.launch {
            while (true) {
                _memoryText.value = memoryUsageManager.getMemoryUsage(context)
//                Log.e("메모리 값", memoryText.value)
                delay(1000)
            }
        }
    }
}