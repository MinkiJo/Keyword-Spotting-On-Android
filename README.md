
## 📅 기간

`2024.10.14 ~ 2024.11.19`

## ℹ 프로젝트 소개

사용자가 특정 단어(호출어)를 발화했을 때 이를 인식하고, 이후 음성 명령에 따라 반응하는 **실시간 음성 인식 안드로이드 어플리케이션** 입니다.

사용자가 음성 인식 AI 모델을 손쉽게 구축하고 안드로이드 기기에 포팅할 수 있도록 다양한 예제 문서와 도구를 제공합니다. 

이를 통해 사용자는 간단하게 음성 인식 모델을 만들고, 안드로이드 기반의 온디바이스 AI 예제를 실행할 수 있습니다.
### 주요기능
#### 1. 실시간 호출어 인식 (Keyword Spotting)
   - CNN, RNN, ResNet 세 가지 음성 인식 모델을 결합하여 실시간으로 호출어 감지
   - 다양한 모델의 조합을 통해 호출어를 높은 정확도로 인식

#### 2. 명령어 인식 및 기능 제어
   - 호출어 인식 후, TTS(텍스트 음성 변환) 기능을 통해 명령을 인식하고 해석
   - 사용자는 음성 명령으로 손전등 켜기/끄기와 같은 간단한 기능을 제어할 수 있으며, 향후 다양한 기능 추가 예정

#### 3. 안드로이드 온디바이스 AI
   - 모든 음성 인식과 명령 처리 작업을 기기 내에서 수행하도록 설계하여 온디바이스 AI 경험을 제공
   - 실시간 처리가 가능한 최적화된 구조로, 모바일 환경에서 원활하게 동작하도록 구현

<div align="center">
    <img src="/uploads/f8669126fdb1499810b9408b71a6d5f9/mockup.png" alt="mockup">
</div>

## 🛠 시작하기

### 1️⃣ 음성 데이터 준비하기

프로젝트는 다음 스펙의 음성 데이터를 사용합니다:

- 길이: 2초
- 샘플레이트: 16000Hz

### service/HotWordService.kt 
```kotlin
object AudioConstants {
   const val THRESHOLD = 0.95   // 호출어 성공 여부 판단을 위한 임계값
   const val SAMPLE_RATE = 16000   // 샘플 레이트 16KHz (16000Hz)
   const val RECORDING_TIME = 2    // 녹음 시간 (2초)
   const val WINDOW_SIZE = SAMPLE_RATE * RECORDING_TIME  // 전체 window size
   const val STEP_SIZE = SAMPLE_RATE / 2     // sliding window 사이즈 (겹치는 구간)
}
```
변수들을 통해 다양한 녹음 관련 설정을 변경할 수 있습니다.


## 🎵 오디오 변환 및 증강 도구

해당 프로젝트는 M4A 형식의 오디오 파일을 WAV 형식으로 변환하고, 표준화(길이, 샘플레이트) 및 데이터 증강을 통해 다양한 오디오 샘플을 생성할 수 있는 Python 스크립트를 제공합니다.

이를 활용해 부족한 음성 데이터를 증강시켜 학습 정확도를 높일 수 있습니다.

### 📋 필수 설치
**1. 파이썬 라이브러리**
  - `pydub`, `librosa`, `soundfile`, `audiomentations`
  - 설치 명령어:
    ```bash
    pip install pydub librosa soundfile audiomentations
    ```

**2. FFmpeg 설치**
  - M4A 파일 처리를 위해 필요. [FFmpeg 다운로드](https://ffmpeg.org/download.html) 후 시스템 환경 변수에 추가.

**🔄 M4A → WAV 변환**
  - 샘플레이트: 16,000 Hz로 고정.
  - 파일 길이: 2초로 표준화 (초과 시 자르고, 부족 시 무음 추가).

**🔊 오디오 증강**
  - 가우시안 노이즈, 속도 조절, 음정 조절, 볼륨 증감, 시간 이동, 배경 소음 추가.
  - 확률적으로 적용하여 데이터 다양성 증대.


## 사용법
1. **스크립트 실행**  
   변환 및 증강 작업 수행:
   ```bash
   python augment_wav_files.py
2. **결과 확인**
변환된 파일: output/converted_files/
증강된 파일: output/augmented_files/

## 📂 폴더구조
### 실행전
```plaintext
project/
├── m4a_files/
│   ├── file1.m4a
│   ├── file2.m4a
```
### 실행후
```
project/
├── output/
│   ├── converted_files/
│   │   ├── file1.wav
│   ├── augmented_files/
│       ├── aug_file1_1.wav
```


```kotlin
   // 모델 타입
   enum class ModelType {
       RESNET, CNN, GRU
   }

    var MODEL_TYPE: ModelType = ModelType.CNN
```
ModelType 통해 새로운 AI 모델을 쉽게 추가하고 변경할 수 있습니다.

## 2️⃣ 음성 데이터 AI 학습 

변환 문서 예제를 통해 사전에 준비된 음성 데이터를 학습시키고 tflite / pytorch 모델로 변환 후 안드로이드 기반 컨버젼을 할 수 있습니다.

### CNN / RNN

#### AudioClassifier
```java
   private static final String MODEL_FILE = "CNN_or_RNN_Model.tflite";
```
asset 폴더에 tflite로 변환한 CNN / RNN 모델 경로를 명시해줍니다.

아래 예제를 통해 CNN 모델을 통해 AI 모델을 학습시키고 tflite로 변환할 수 있습니다.

[CNN 학습 및 tflite 변환 예제](https://lab.ssafy.com/s11-final/S11P31S207/-/blob/develop/AI/CNN/README.md?ref_type=heads)


### Resnet
#### ResnetClassifier
```java
 private final String[] labels = {
        "_silence_", 
        "_unknown_",  
        "down",
        "go",
        "left",   
        "no",
        "off",
        "on",
        "right",
        "stop",
        "up",
        "yes",
        "hey_ssafy" // 호출어 인식을 위한 단어 레이블 추가
};
```
label에 커스텀 호출어 레이블을 추가합니다.

```java
private static final String MEL_MODEL_PATH = "mel_spectogram_convert_model.ptl";
private static final String RESNET_MODEL_PATH = "trigger_word_detection_model_with_ResNet.ptl";
```
Asset 폴더에 삽입한 AI 모델을 변환한 PyTorch Script 파일을 불러옵니다.

- **mel_spectogram_model** : 음성 데이터를 **MelSpectogram**으로 변환
- **resnet_model** : Resnet 학습 모델

아래 예제를 통해 RESNET 모델을 통해 AI 모델을 학습시키고 pytorch로 변환할 수 있습니다.

[RESNET 학습 및 PyTorch 변환 예제](https://lab.ssafy.com/s11-final/S11P31S207/-/tree/develop/AI/bcresnet-main?ref_type=heads)

