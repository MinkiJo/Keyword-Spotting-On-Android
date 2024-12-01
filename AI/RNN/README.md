# RNN based Keyword Spotting.



## 음성 데이터의  시간-주파수 도메인 변환

**STFT(Short-Time Fourier Transform)** 를 활용하여 원시 음성 데이터를 2차원 이미지 데이터(스펙트로그램)로 변환합니다.

![image](https://github.com/user-attachments/assets/16e428cc-d3c1-463c-b3d2-cf03a21e2498)


- 아날로그 음성신호의 시간-도메인 데이터는 시간적 변화 정보만을 담고 있어, 주파수 성분에 대한 분석이 어렵습니다. 

- 이로 인해 음성 데이터의 주파수 패턴을 효과적으로 학습하기 어렵고, 모델이 중요한 특징을 제대로 이해하지 못할 가능성이 높습니다.

- 시간-주파수 도메인으로 변환하면 **시간에 따른 주파수 구성의 변화**를 시각화할 수 있어 호출어 인식 학습에 매우 효과적입니다.



## RNN 기반 음성 인식



RNN 모델은 현재 상태가 이전 상태에 영향을 받아 순차적 특성을 학습함에 있어 장점이 있습니다.

순차 데이터란 순차적 구성 요소가 복잡한 의미와 구문 규칙에 따라 상호 연관되는, 단어, 문장 또는 시계열 데이터 등의 데이터를 말하는데요, 하지만 기울기 소실 문제로 인한 장기적인 의존성을 처리하기 어려움이란 한계를 극복하기 위해 Cell State와 Gate Mechanism을 통해 중요 정보를 선택적으로 유지하거나 버림으로써 장기 의존성을 더 효과적으로 학습가능한 모델이 제시되었습니다. 이에 저희는 기존의 LSTM구조보다 메모리를 절약할 수 있는 GRU 모델을 통해 호출어의 순차적 특성을 학습하고 성능을 증가시키고자 했습니다.

![image](https://github.com/user-attachments/assets/b3558176-e608-47ea-92b6-6fde51af80ca)





## GRU Model 구현



1. Model

```python
 expected_model = [['InputLayer', [(None, 5511, 101)], 0],
                     ['Conv1D', (None, 1375, 196), 297136, 'valid', 'linear', (4,), (15,), 'GlorotUniform'],
                     ['BatchNormalization', (None, 1375, 196), 784],
                     ['Activation', (None, 1375, 196), 0],
                     ['Dropout', (None, 1375, 196), 0, 0.8],
                     ['GRU', (None, 1375, 128), 125184, True],
                     ['Dropout', (None, 1375, 128), 0, 0.8],
                     ['BatchNormalization', (None, 1375, 128), 512],
                     ['GRU', (None, 1375, 128), 99072, True],
                     ['Dropout', (None, 1375, 128), 0, 0.8],
                     ['BatchNormalization', (None, 1375, 128), 512],
                     ['Dropout', (None, 1375, 128), 0, 0.8],
                     ['TimeDistributed', (None, 1375, 1), 129, 'sigmoid']]
    comparator(summary(model), expected_model)
```



2. DataSet

Original Data Set("Hey SSAFY") + Data Argumentation using Python Lib (about 3000 wav files)

3. Accuracy

![image](https://github.com/user-attachments/assets/2297c05f-bbf0-4130-a3b2-3c5c823d3e84)





## TF Lite 변환

- TensorFlow Lite는 경량화와 성능 최적화를 위해 제한된 연산집합만 지원
- Flex delegate를 통해 STFT, 사용자 정의 연산 (Custom Ops), RNN 관련 연산, 고급 연산과 사용자 정의 레이어가 포함된 모델도 변환 가능

```python
# TFLite 변환기 설정
converter = tf.lite.TFLiteConverter.from_keras_model(model)
converter.target_spec.supported_ops = [
    tf.lite.OpsSet.TFLITE_BUILTINS, 
    tf.lite.OpsSet.SELECT_TF_OPS  # Flex delegate 사용
]
```

