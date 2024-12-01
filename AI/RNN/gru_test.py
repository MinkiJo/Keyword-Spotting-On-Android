import librosa
import tensorflow as tf
import sounddevice as sd
import numpy as np

# TensorFlow Lite 모델 로드
interpreter = tf.lite.Interpreter(model_path='model/trigger_word_detection_model_B32_lr5e-5_pat30.tflite')
interpreter.allocate_tensors()

# 모델의 입력 및 출력 텐서 가져오기
input_details = interpreter.get_input_details()
output_details = interpreter.get_output_details()

# 오디오를 스펙트로그램으로 변환하는 함수
def preprocess_audio(audio, sr=32000, n_mels=128, hop_length=512):
    spectrogram = librosa.feature.melspectrogram(y=audio, sr=sr, n_mels=n_mels, hop_length=hop_length)
    log_spectrogram = librosa.power_to_db(spectrogram, ref=np.max)
    return log_spectrogram.T  # 시간축을 앞으로 변환

# 실시간 트리거 워드 감지 함수
def real_time_trigger_word_detection(duration=2, sample_rate=32000):
    while True:
        print("Recording...")
        # duration 초 동안 오디오 캡처
        audio = sd.rec(int(duration * sample_rate), samplerate=sample_rate, channels=1, dtype='float32')
        sd.wait()  # 캡처 완료 대기
        audio = np.squeeze(audio)  # (샘플 수, ) 형태로 변환
        print("Recording complete.")

        # 오디오 데이터를 스펙트로그램으로 변환
        spectrogram = preprocess_audio(audio, sr=sample_rate)

        # 모델 입력 형식에 맞게 reshape
        spectrogram = np.expand_dims(spectrogram, axis=0).astype(np.float32)  # (1, time_steps, n_mels)

        # 예측 수행
        interpreter.set_tensor(input_details[0]['index'], spectrogram)
        interpreter.invoke()  # 모델 실행
        prediction = interpreter.get_tensor(output_details[0]['index'])  # 결과 가져오기

        print(f"Prediction: {prediction}")

# 실시간 트리거 워드 감지 실행
real_time_trigger_word_detection()
