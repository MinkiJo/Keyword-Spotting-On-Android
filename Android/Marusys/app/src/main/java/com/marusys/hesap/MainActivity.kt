package com.marusys.hesap

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.marusys.hesap.feature.MemoryUsageManager
import com.marusys.hesap.feature.VoiceRecognitionState
import com.marusys.hesap.feature.VoiceStateManager
import com.marusys.hesap.presentation.screen.AudioScreen
import com.marusys.hesap.presentation.viewmodel.MainViewModel
import com.marusys.hesap.service.AudioService
import com.marusys.hesap.service.HotWordService
import kotlinx.coroutines.launch
import java.security.Permissions


class MainActivity : ComponentActivity() {
    // 여러 페이지에서 사용하는 값을 관리하는 viewModel
    private val mainViewModel = MainViewModel()
    private lateinit var memoryUsageManager: MemoryUsageManager
    private val OVERLAY_PERMISSION_REQUEST_CODE = 1

    private lateinit var audioRecognitionServiceIntent: Intent

    // AudioService에서 인식한 음성 명령어를 MainActivity에서 받는 콜백함수
    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                "SPEECH_RECOGNITION_RESULT" -> {
                    val matches = intent.getStringArrayListExtra("matches")
                    val recognizedText = matches?.firstOrNull() ?: ""
                    Log.d("MainActivity", "Received text: $recognizedText")
                    // 현재 표시 중인 윈도우 매니져가 있다면 메시지 업데이트
                    mainViewModel.setCommandText(recognizedText)
                }

                "AUDIO_SERVICE_STATE_CHANGED" -> {
                    val isRunning = intent.getBooleanExtra("isRunning", false)
                    Log.d("AUDIO_SERVICE_STATE_CHANGED", isRunning.toString())
                    mainViewModel.setAudioServiceRunning(isRunning)
                }

                "RESULT_UPDATE" -> {
                    val result = intent.getStringExtra("result")
                    mainViewModel.setResultText(result ?: "")
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 오디오 녹음 권한이 있는지 확인
        checkAndRequestPermissions()

        // 오버레이 권한 체크 및 요청
        checkOverlayPermission()
        // BroadcastReceiver 등록
        LocalBroadcastManager.getInstance(this).registerReceiver(
            receiver,
            IntentFilter().apply {
                addAction("SPEECH_RECOGNITION_RESULT")
                addAction("AUDIO_SERVICE_STATE_CHANGED")
                addAction("RESULT_UPDATE")
            }
        )

        // 상태 관찰
        lifecycleScope.launch {
            VoiceStateManager.voiceState.collect { state ->
                when (state) {
                    is VoiceRecognitionState.WaitingForHotword -> {
                        Log.e("", "호출어 대기 상태")
                        startHotWordService()
                    }

                    is VoiceRecognitionState.HotwordDetecting -> {
                        Log.e("", "호출어 인식 상태")
                        startAudioService()
                    }

                    else -> {
                        // 다른 상태 처리
                    }
                }
            }
        }

        setContent {
            Surface(modifier = Modifier.fillMaxSize()) {
                AudioScreen(
                    viewModel = mainViewModel,
                )
            }
        }
    }

    /*
     *  각종 권한 체크 함수
     */
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            1 -> {
                if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                    // 모든 권한이 승인된 경우
                    startHotWordService()
                } else {
                    // 권한이 거부된 경우
                    val deniedPermissions = permissions.filterIndexed { index, _ ->
                        grantResults[index] != PackageManager.PERMISSION_GRANTED
                    }
                    // 권한이 거부된 경우
                    Log.d("PermissionResult", "거부된 권한: ${deniedPermissions.joinToString()}")
                    Toast.makeText(
                        this,
                        "권한이 필요합니다. ${deniedPermissions.joinToString()}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    // 권한 체크 및 요청
    private fun checkAndRequestPermissions() {
        // Android 13 (API 33) 이상
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED ||
                checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED ||
                checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissions(
                    arrayOf(
                        Manifest.permission.RECORD_AUDIO,
                        Manifest.permission.CAMERA,
                        Manifest.permission.POST_NOTIFICATIONS,
                    ),
                    1
                )
            }
        } else {
            if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED ||
                checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED ||
                checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissions(
                    arrayOf(
                        Manifest.permission.RECORD_AUDIO,
                        Manifest.permission.CAMERA,
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                    ),
                    1
                )
            }
        }
    }

    private fun checkOverlayPermission() {
        if (!Settings.canDrawOverlays(this)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivityForResult(intent, OVERLAY_PERMISSION_REQUEST_CODE)
        }
    }

    override fun onResume() {
        super.onResume()
    }

    override fun onPause() {
        super.onPause()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == OVERLAY_PERMISSION_REQUEST_CODE) {
            if (!Settings.canDrawOverlays(this)) {
                Toast.makeText(this, "오버레이 권한이 필요합니다", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver)
        stopService(audioRecognitionServiceIntent)
    }

    // 호출어 인식 서비스 시작
    private fun startHotWordService() {
        audioRecognitionServiceIntent = Intent(this, HotWordService::class.java)
        startForegroundService(audioRecognitionServiceIntent)
    }

    // 로출어 인식 -> 서비스 시작
    private fun startAudioService() {
        audioRecognitionServiceIntent = Intent(this, AudioService::class.java)
        startService(audioRecognitionServiceIntent)
    }

    // 서비스 종료
    private fun stopAudioService() {
        // 배경 변경
        mainViewModel.setAudioServiceRunning(false)
        val serviceIntent = Intent(this, AudioService::class.java)
        stopService(serviceIntent)
        VoiceStateManager.updateState(VoiceRecognitionState.WaitingForHotword) // 호출어 대기
    }
}



