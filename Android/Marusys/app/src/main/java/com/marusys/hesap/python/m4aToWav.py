# m4a -> wav 컨버트

import os
from pydub import AudioSegment

# FFmpeg 경로 설정
AudioSegment.ffmpeg = "C:/Users/SSAFY/Desktop/workspace/ffmpeg/bin"  # 여기를 FFmpeg 설치 경로로 변경

# 변환할 폴더 경로 설정
input_folder = 'chh'  # `.m4a` 파일이 들어 있는 폴더
output_folder = 'chh_wav'       # 변환한 `.wav` 파일을 저장할 폴더

# 출력 폴더가 없으면 생성
os.makedirs(output_folder, exist_ok=True)

# `.m4a` 파일들을 불러와 순서대로 `.wav`로 변환
for idx, filename in enumerate(os.listdir(input_folder), start=1):
    if filename.endswith('.m4a'):
        # 파일 경로 설정
        input_path = os.path.join(input_folder, filename)
        output_path = os.path.join(output_folder, f'hey{idx}.wav')

        # `.m4a` 파일을 불러와 `.wav`로 변환 후 저장
        audio = AudioSegment.from_file(input_path, format='m4a')
        audio.export(output_path, format='wav')

        print(f"Converted {filename} to {output_path}")

print("All files converted successfully!")