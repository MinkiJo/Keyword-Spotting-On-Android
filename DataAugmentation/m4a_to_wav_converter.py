import os
from pydub import AudioSegment

def convert_m4a_to_wav(input_folder, output_folder, duration_seconds=2, sample_rate=16000):
    """
    m4a 파일을 wav 형식으로 변환하고, 모든 오디오 파일을 지정된 길이와 샘플링 레이트로 맞춥니다.
    
    Args:
        input_folder (str): 변환할 m4a 파일들이 있는 폴더 경로
        output_folder (str): 변환된 wav 파일들을 저장할 폴더 경로
        duration_seconds (int): 변환된 파일의 길이(초) (기본값: 2초)
        sample_rate (int): 변환할 wav 파일의 샘플링 레이트 (기본값: 16000Hz)
    """
    # 출력 폴더가 없으면 생성
    if not os.path.exists(output_folder):
        os.makedirs(output_folder)

    # 지정한 길이를 밀리초로 변환
    duration_ms = duration_seconds * 1000  # 2초를 밀리초로 변환

    # 파일 번호 초기화
    file_number = 1

    # 폴더 내의 모든 파일 확인
    for filename in os.listdir(input_folder):
        # m4a 파일만 선택
        if filename.endswith(".m4a"):
            # 입력 파일 경로 설정
            input_file = os.path.join(input_folder, filename)
            
            # 출력 파일 경로를 번호 기반으로 설정
            output_file = os.path.join(output_folder, f"{file_number}.wav")
            
            # m4a 파일을 로드
            audio = AudioSegment.from_file(input_file, format="m4a")
            
            # 샘플링 레이트 고정
            audio = audio.set_frame_rate(sample_rate)
            
            # 오디오 길이를 2초로 맞추기
            if len(audio) > duration_ms:
                audio = audio[:duration_ms]  # 길이가 길면 2초로 자름
            else:
                audio = audio + AudioSegment.silent(duration=duration_ms - len(audio))  # 짧으면 2초가 되도록 패딩 추가
            
            # wav 형식으로 변환 후 저장
            audio.export(output_file, format="wav")
            print(f"{input_file} 파일이 {output_file}로 변환되었습니다.")
            
            # 파일 번호 증가
            file_number += 1

# 변환할 폴더 경로 설정
input_folder = "false"  # 변환할 m4a 파일들이 있는 폴더 경로
output_folder = "falseWav"  # 저장할 wav 파일들을 저장할 폴더 경로

# 변환 실행
convert_m4a_to_wav(input_folder, output_folder, duration_seconds=2, sample_rate=16000)