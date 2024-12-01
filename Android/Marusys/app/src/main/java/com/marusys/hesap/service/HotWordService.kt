package com.marusys.hesap.service

import android.Manifest
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.marusys.hesap.classifier.AudioClassifier
import com.marusys.hesap.classifier.BaseAudioClassifier
import com.marusys.hesap.classifier.ResnetClassifier
import com.marusys.hesap.feature.VoiceRecognitionState
import com.marusys.hesap.feature.VoiceStateManager
import com.marusys.hesap.presentation.components.AudioNotification
import com.marusys.hesap.presentation.components.AudioNotification.Companion.NOTIFICATION_ID
import com.marusys.hesap.service.AudioConstants.RECORDING_TIME
import com.marusys.hesap.service.AudioConstants.SAMPLE_RATE
import com.marusys.hesap.service.AudioConstants.STEP_SIZE
import com.marusys.hesap.service.AudioConstants.THRESHOLD
import com.marusys.hesap.service.AudioConstants.TRIGGER_WORD
import com.marusys.hesap.service.AudioConstants.WINDOW_SIZE
import com.marusys.hesap.util.ThresholdUtil

// 녹음 관련 상수 정의
object AudioConstants {
    // 호출어 성공 여부 판단을 위한 임계값
    const val THRESHOLD = 0.95
    // 샘플 레이트 16KHz (16000Hz)
    const val SAMPLE_RATE = 16000
    // 녹음 시간 (2초)
    const val RECORDING_TIME = 2
    // 전체 window size
    const val WINDOW_SIZE = SAMPLE_RATE * RECORDING_TIME
    // sliding window 사이즈 (겹치는 구간)
    const val STEP_SIZE = SAMPLE_RATE / 2
    // Resnet Softmax 분류를 위한 트리거워드 설정
    const val TRIGGER_WORD = "hey_ssafy"
}

private val TAG = "HotWordService"
class HotWordService : Service() {
    private lateinit var wakeLock: PowerManager.WakeLock
    private lateinit var audioNotification: AudioNotification
    private lateinit var classifier: AudioClassifier

    // 모델 타입
    enum class ModelType {
        RESNET, CNN, GRU
    }
    // CNN & RNN은 같은 AudioClassifer 클래스를 사용하므로 (RNN <-> CNN) 변경 시 클래스 내부 tflite 모델 (RNN <-> CNN) 변경 필요
    var MODEL_TYPE: ModelType = ModelType.GRU

    // 사용자에게 모델 타입을 선택할 수 있게 해주는 메서드
    private fun startRecordingWithModel() {
        when (MODEL_TYPE) {
            ModelType.RESNET -> resnetRealTimeRecordAndClassify()
            ModelType.CNN -> cnnRealTimeRecordAndClassify()
            ModelType.GRU -> gruRealTimeRecordAndClassify()
        }
    }
    override fun onCreate() {
        super.onCreate()
        Log.e(TAG, "호출어 탐지 시작")
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "HotWordService::WakeLock")
        wakeLock.acquire()
        audioNotification = AudioNotification(this)
        startForeground(NOTIFICATION_ID, audioNotification.getNotification()) //알림창 실행
        classifier = AudioClassifier(this)
        startRecordingWithModel() // 호출어 인식
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.e(TAG, "onstartCommand")
        startRecordingWithModel() // 호출어 인식
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.e(TAG, "ondestroy")
        if (wakeLock.isHeld) {
            wakeLock.release()
        }
        stopForeground(STOP_FOREGROUND_DETACH)
        // 알림 유지
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(NOTIFICATION_ID, audioNotification.getNotification())

        // 서비스 재시작
        val restartServiceIntent = Intent(applicationContext, HotWordService::class.java)
        startForegroundService(restartServiceIntent)
    }
    override fun onBind(intent: Intent): IBinder? = null

    private fun sendResultUpdate(result: String) {
        val intent = Intent("RESULT_UPDATE")
        intent.putExtra("result", result)
        Log.d(TAG, "$result")
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }

    /*
   * 음성인식 수행 메서드
   */

    // 음성 인식 및 Classifer 기반 분석 수행 공통 메서드
    fun startRealTimeRecognition(
        classifierProvider: () -> BaseAudioClassifier, // 모델별 분류기 생성자
    ) {
        val bufferSize = AudioRecord.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        ) * RECORDING_TIME

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        sendResultUpdate("듣고 있는 중이에요.")

        Thread {
            val audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize
            )
            if (audioRecord.state != AudioRecord.STATE_INITIALIZED) {
                Log.e(TAG, "AudioRecord 초기화 실패")
                sendResultUpdate("녹음 초기화 실패")
                return@Thread
            }

            val audioBuffer = ShortArray(bufferSize / 2)
            val slidingWindowBuffer = FloatArray(WINDOW_SIZE)
            var bufferPosition = 0

            audioRecord.startRecording()

            while (VoiceStateManager.voiceState.value == VoiceRecognitionState.WaitingForHotword) {
                val readSize = audioRecord.read(audioBuffer, 0, audioBuffer.size)
                if (readSize > 0) {
                    for (i in 0 until readSize) {
                        slidingWindowBuffer[bufferPosition] = audioBuffer[i] / 32768.0f
                        bufferPosition++

                        if (bufferPosition >= WINDOW_SIZE) {
                            bufferPosition = 0
                            try {
                                // 선택한 classifier 기반 음성 분류 실행
                                val classifier = classifierProvider()
                                val results = classifier.classify(slidingWindowBuffer)

                                // Resnet 음성인식 결과 처리 -> Softmax
                                if (MODEL_TYPE == ModelType.RESNET) {
                                    processResNetResults(results, classifier)
                                } else {
                                    // CNN / GRU 음성인식 결과 처리 -> Sigmoid
                                    processOtherModelResults(results)
                                }

                            } catch (e: Exception) {
                                Log.e("MainActivity", "분류 중 오류 발생", e)
                                sendResultUpdate("분류 중 오류가 발생했습니다: ${e.message}")
                            }

                            // 슬라이딩 윈도우 이동
                            System.arraycopy(
                                slidingWindowBuffer,
                                STEP_SIZE,
                                slidingWindowBuffer,
                                0,
                                WINDOW_SIZE - STEP_SIZE
                            )
                            bufferPosition = WINDOW_SIZE - STEP_SIZE
                        }
                    }
                }
            }

            audioRecord.stop()
            audioRecord.release()
        }.start()
    }
    // 모델별 음성인식 메서드
    fun cnnRealTimeRecordAndClassify() {
        startRealTimeRecognition(
            classifierProvider = {AudioClassifier(this)},
        )
    }

    fun gruRealTimeRecordAndClassify() {
        startRealTimeRecognition(
            classifierProvider = {AudioClassifier(this)},
        )
    }

    fun resnetRealTimeRecordAndClassify() {
        startRealTimeRecognition(
            classifierProvider = {ResnetClassifier(this)},
        )
    }
    // ResNet 결과 처리 메서드
    private fun processResNetResults(
        results: FloatArray,
        classifier: BaseAudioClassifier,
    ) {

        // Softmax 계산 알고리즘을 통해 가장 높은 확률의 라벨 정확도 계산
        val accuracy = ThresholdUtil.checkTrigger(results)

        // 선택된 라벨 할당
        val resultLabel = classifier.getLabel(results)

        val resultText = "${resultLabel} : ${String.format("%.2f%%", accuracy * 100)}"

        // UI에 선택된 라벨과 정확도 출력
        sendResultUpdate(resultText)

        // 정확도가 THRESHOLD 이상이고 내가 원하는 호출어가 맞다면 TTS 음성인식 수행
        if (accuracy >= THRESHOLD && resultLabel == TRIGGER_WORD) {
            VoiceStateManager.updateState(VoiceRecognitionState.HotwordDetecting)
        }
    }

    // CNN & GRU 결과 처리 메서드
    private fun processOtherModelResults(
        results: FloatArray,
    ) {

        val accuracy = String.format("%.2f%%", results[0] * 100)

        val resultText = "확률값 : $accuracy"

        // UI에 정확도 출력
        sendResultUpdate(resultText)

        // 정확도가 THRESHOLD 이상인 경우 TTS 음성인식 수행
        if (results[0] >= THRESHOLD) {
            VoiceStateManager.updateState(VoiceRecognitionState.HotwordDetecting)
        }
    }
}
