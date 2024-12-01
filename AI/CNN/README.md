## ℹ CNN

### **📖 CNN 이론**
**CNN(Convolutional Neural Network)** 은 딥러닝 모델의 한 종류로, 데이터를 처리할 때 **국소적 패턴을 효과적으로 학습**하는 구조를 가집니다.  

특히, **이미지 데이터**나 **스펙트로그램(음성 데이터)** 처럼 **공간적(2D)** 또는 **시간적(1D)** 구조를 가진 데이터를 분석하는 데 뛰어난 성능을 발휘합니다.

---

### **1️⃣ CNN의 작동 과정**

<div align="center">
    <img src="/uploads/9472c769963212a9eee37626e90549b8/cnn1.gif" width="450" height="auto">
</div>

- CNN은 입력 데이터에서 **특징을 추출**하고, 단계별로 중요한 정보를 **압축**하여 최종적으로 분류하는 과정을 수행합니다.

- **Convolutional Layer**를 통해 국소적인 특징을 추출하며, 각 층이 점차 **추상적이고 중요한 특징**을 학습합니다.

- **Fully Connected Layer**는 이 추출된 특징을 기반으로 최종 출력(예: 숫자 분류)을 수행합니다.

---

### **2️⃣ CNN 계층 구조**

<div align="center">
    <img src="/uploads/89e707dba1e487c3119158fde073cafe/cnn2.png" width="450" height="auto">
</div>

CNN은 입력 계층, 출력 계층, 그리고 그 사이의 여러 은닉 계층으로 구성됩니다.  

#### **컨벌루션 계층 (Convolutional Layer)**  
- 입력 데이터를 **컨벌루션 필터**에 통과시켜 특정 특징(예: 가장자리, 질감 등)을 활성화합니다.

#### **ReLU 계층 (Rectified Linear Unit)**  
- 음수 값은 0으로, 양수 값은 그대로 유지하여 **비선형성**을 추가합니다.  
- 활성화된 특징만 다음 계층으로 전달되어 학습 속도와 성능을 향상시킵니다.

#### **풀링 계층 (Pooling Layer)**  
- **비선형 다운샘플링**을 수행하여 데이터 크기를 줄이고, 학습해야 할 파라미터 수를 감소시킵니다.  
- 주요 특징만 남겨 모델의 **일반화 성능**을 향상시킵니다.

---

### **3️⃣ CNN 구조 상세**

<div align="center">
    <img src="/uploads/afc795cc7f1cbf14c4b311f78d72b428/cnn3.png" width="450" height="auto">
</div>

이 이미지는 CNN의 핵심 구조를 시각화한 것입니다:

- **Convolution + ReLU**: 입력 데이터를 필터로 처리하여 특징을 추출하고 비선형성을 추가합니다.

- **Pooling**: 데이터 크기를 줄여 계산 효율성을 높이고, 주요 정보를 유지합니다.

- **Flatten + Fully Connected**: 데이터를 1D 형태로 변환하여 분류나 회귀 작업을 위한 최종 출력을 만듭니다.

이 과정은 이미지 데이터뿐만 아니라 **음성 스펙트로그램** 같은 시간-주파수 데이터에도 효과적으로 적용됩니다.

---

## **🔊 호출어 학습 절차**

### 0️⃣ 음성 데이터 준비

```python
# 데이터셋 로드 및 저장 경로 설정

sample_rate = 16000  # 목표 샘플링 레이트
time_rate = 2 # 몇초 음성
target_length = sample_rate * time_rate

train_ds, val_ds = tf.keras.utils.audio_dataset_from_directory(
    directory=data_dir,
    batch_size=16,
    validation_split=0.2,
    seed=0,
    output_sequence_length=target_length,
    subset='both',
)
```

해당 프로젝트는 2초 길이의 16000hz 주파수음성을 기반으로 학습을 진행하기에 적절한 전처리 과정을 통해 이에 적합한 음성 데이터를 준비하고 이를 불러옵니다.

### **1️⃣ 음성 데이터의 푸리에 변환**

**STFT(Short-Time Fourier Transform)** 를 활용하여 원시 음성 데이터를 2차원 이미지 데이터(스펙트로그램)로 변환합니다.

#### **시간-도메인 신호**

<div align="center">
    <img src="/uploads/5fd9cec725e47a50646adcc3aef0b263/주파수.png" width="450" height="auto">
</div>

- 위와 같은 시간-도메인의 원시 음성 데이터 신호는 시간적 변화 정보만을 담고 있어, 주파수 성분에 대한 분석이 어렵습니다. 

- 이로 인해 음성 데이터의 주파수 패턴을 효과적으로 학습하기 어렵고, 모델이 중요한 특징을 제대로 이해하지 못할 가능성이 높습니다.

#### **시간-주파수 도메인 변환**

<div align="center">
    <img src="/uploads/d47e3f53afaef62b44a6a876af069faa/스펙토.png" width="450" height="auto">
</div>

- 시간-주파수 도메인으로 변환하면 **시간에 따른 주파수 구성의 변화**를 시각화할 수 있어 CNN 학습에 매우 효과적입니다.

---

### **2️⃣ 스펙트로그램 변환을 활용한 모델 구현**

STFT를 활용한 음성 변환 계층을 모델 내부에 레이어로 추가하여 원시 음성데이터가 입력으로 들어오면 스펙트로그램으로 변환하고, 이를 CNN 모델의 입력 데이터로 사용합니다

```python
def get_spectrogram(waveform):
    spectrogram = tf.signal.stft(waveform, frame_length=255, frame_step=128)
    spectrogram = tf.abs(spectrogram)
    spectrogram = spectrogram[..., tf.newaxis]  # 채널 차원 추가
    return spectrogram

model = models.Sequential([
    # 입력 레이어
    layers.Input(shape=input_shape),

    # 음성 데이터 -> 스펙트로그램 변환 레이어(STFT)
    tf.keras.layers.Lambda(get_spectrogram),

    # 이후 CNN 계층 추가
    layers.Conv2D(32, kernel_size=3, activation='relu'),
    
    ...

])
```

## 📘 Jupyter Notebook
[CNN Word Trigger Model](https://lab.ssafy.com/s11-final/S11P31S207/-/blob/develop/AI/CNN/CNN_WordTrigger_3Conv_AddDropOut_Model.ipynb?ref_type=heads)


