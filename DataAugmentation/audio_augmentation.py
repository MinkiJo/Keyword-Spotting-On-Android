import os
import random
import librosa
import soundfile as sf
from audiomentations import Compose, AddGaussianNoise, TimeStretch, PitchShift, Gain, Shift, AddBackgroundNoise
from pydub import AudioSegment

# 오디오 증강 파이프라인 정의
augment = Compose([
    AddGaussianNoise(min_amplitude=0.001, max_amplitude=0.015, p=random.randint(2, 8) / 10),  # 랜덤 확률
    TimeStretch(min_rate=0.8, max_rate=1.3, p=random.randint(2, 8) / 10),
    PitchShift(min_semitones=-2, max_semitones=2, p=random.randint(2, 8) / 10),
    Gain(min_gain_in_db=-10, max_gain_in_db=10, p=random.randint(2, 8) / 10),
    Shift(min_shift=-0.5, max_shift=0.5, rollover=True, p=random.randint(2, 8) / 10),
    AddBackgroundNoise(sounds_path="1.wav", min_snr_in_db=5, max_snr_in_db=20, p=random.randint(2, 8) / 10)
])

def create_augmented_audio(input_file, output_path, num_augmentations=300, sample_length=32000, sample_rate=16000):
    """
    생성된 오디오 파일에 대해 오디오 증강을 수행하고 길이를 2초로 고정
    """
    os.makedirs(output_path, exist_ok=True)
    samples, _ = librosa.load(input_file, sr=sample_rate)
    
    for i in range(num_augmentations):
        # 오디오 증강 적용
        augmented_samples = augment(samples=samples, sample_rate=sample_rate)
        
        # 길이 고정 (2초, 16000Hz 샘플링 기준 32000 샘플)
        if len(augmented_samples) < sample_length:
            augmented_samples = librosa.util.fix_length(augmented_samples, size=sample_length)
        else:
            augmented_samples = augmented_samples[:sample_length]
        
        # 증강된 오디오 파일 저장 (aug 접두어 추가)
        output_filename = f"aug_{os.path.splitext(os.path.basename(input_file))[0]}_{i+1}.wav"
        output_filepath = os.path.join(output_path, output_filename)
        sf.write(output_filepath, augmented_samples, sample_rate)

if __name__ == "__main__":
    input_audio_path = "realOutput"  # 증강할 오디오 파일들이 있는 폴더
    augmented_audio_path = "augmented_audio"  # 증강된 오디오 파일 저장 폴더
    os.makedirs(augmented_audio_path, exist_ok=True)

    # 입력 폴더의 모든 파일을 증강
    for filename in os.listdir(input_audio_path):
        if filename.endswith(".wav"):  # WAV 파일만 처리
            input_file = os.path.join(input_audio_path, filename)
            print(f"Processing {input_file}")
            create_augmented_audio(input_file, augmented_audio_path, num_augmentations=300)
    
    print("모든 오디오 증강이 완료되었습니다.")
