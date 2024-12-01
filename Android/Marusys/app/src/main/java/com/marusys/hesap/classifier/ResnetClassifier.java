package com.marusys.hesap.classifier;


import static com.marusys.hesap.service.AudioConstants.RECORDING_TIME;
import static com.marusys.hesap.service.AudioConstants.WINDOW_SIZE;

import android.content.Context;
import android.util.Log;

import org.pytorch.IValue;
import org.pytorch.LiteModuleLoader;
import org.pytorch.Module;
import org.pytorch.Tensor;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class ResnetClassifier extends BaseAudioClassifier {

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
            "hey_ssafy"
    };

    private final Context context;
    private final Module resnetModel, melModel;

    private static final String MEL_MODEL_PATH = "mel_spectogram_convert_model.ptl";
    private static final String RESNET_MODEL_PATH = "trigger_word_detection_model_with_ResNet.ptl";

    private static final long INPUT_HEIGHT = 40;

    // Resnet 모델 입력에 맞게 WIDTH 설정 (16000 주파수 1초 음성 : 101 / 2초 201)
    private static final long INPUT_WIDTH = RECORDING_TIME == 1 ? 101 : 201;

    public ResnetClassifier(Context context) throws IOException {
        // 모델 파일을 로드합니다.
        super(context);
        this.context = context;

        resnetModel = LiteModuleLoader.load(assetFilePath(context, RESNET_MODEL_PATH));
        melModel = LiteModuleLoader.load(assetFilePath(context, MEL_MODEL_PATH));
    }

    @Override
    public float[] classify(float[] audioData) {
        try {

            // 1. 오디오 데이터 적합한 입력 텐서 형태로로 변환
            Tensor audioTensor = Tensor.fromBlob(audioData, new long[]{1, WINDOW_SIZE});

            // 2. Mel spectrogram 계산 -> pytorch 모델 기반 변환 (melModel.ptl)
            Tensor melSpecTensor = melModel.forward(IValue.from(audioTensor)).toTensor();


            // 3. 데이터를 추출한 후 필요한 모양으로 새로운 텐서를 생성
            float[] melSpecData = melSpecTensor.getDataAsFloatArray();

            // Resnet 모델 입력 형태에 맞게 변환
            Tensor reshapedMelTensor = Tensor.fromBlob(melSpecData, new long[]{1L, 1L, INPUT_HEIGHT, INPUT_WIDTH});

            // IValue를 Tensor로 변환하고, shape를 확인하는 예제
//            Tensor tensor = IValue.from(reshapedMelTensor).toTensor();

            // 4. BCResNet으로 분류
            Tensor outputTensor = resnetModel.forward(IValue.from(reshapedMelTensor)).toTensor();

            // 5. 결과 변환 (각 Label 확률값을 담는 배열)
            return outputTensor.getDataAsFloatArray();

        } catch (Exception e) {
            Log.e("AudioProcessor", "Error classifying audio", e);
            return null;
        }
    }

    public String getLabel(float[] result) {

        // 결과 배열에서 최대값의 인덱스를 찾습니다
        int maxIndex = 0;
        for (int i = 1; i < result.length; i++) {
            if (result[i] > result[maxIndex]) {
                maxIndex = i;
            }
        }

        // 최대값 인덱스를 사용하여 레이블을 반환합니다
        return labels[maxIndex];
    }

    // assets 폴더에서 모델 파일을 복사하여 경로를 반환
    private String assetFilePath(Context context, String assetName) throws IOException {
        File file = new File(context.getFilesDir(), assetName);
        if (!file.exists()) {
            try (InputStream is = context.getAssets().open(assetName);
                 FileOutputStream os = new FileOutputStream(file)) {
                byte[] buffer = new byte[4 * 1024];
                int read;
                while ((read = is.read(buffer)) != -1) {
                    os.write(buffer, 0, read);
                }
                os.flush();
            }
        }
        return file.getAbsolutePath();
    }

    // 오디오 데이터를 ByteBuffer로 변환하는 메서드
    public ByteBuffer createInputBuffer(float[] audioData) {

        // 입력 버퍼 생성 (4 bytes per float * 16000 samples * 1 channel)
        ByteBuffer inputBuffer = ByteBuffer.allocateDirect(4 * 1 * 16000);

        inputBuffer.order(ByteOrder.nativeOrder());

        // 오디오 데이터를 버퍼에 넣기
        for (float value : audioData) {
            inputBuffer.putFloat(value);
        }

        // 버퍼 위치를 처음으로 되돌리기
        inputBuffer.rewind();

        return inputBuffer;
    }

    /*
     * wav 음성 파일 기반 분류 메서드
     */

    // byteBuffer -> float[] 변환
    public float[] byteBufferToFloatArray(ByteBuffer buffer) {
        buffer.rewind(); // 버퍼 위치를 처음으로 이동
        int floatCount = buffer.remaining() / Float.BYTES;
        float[] floatArray = new float[floatCount];

        // FloatBuffer로 변환 후 float[]로 가져오기
        buffer.asFloatBuffer().get(floatArray);

        return floatArray;
    }

    // WAV 파일을 읽어 분류 결과 레이블을 반환하는 메서드
    public float[] classifyFromFile(String assetFilePath) {
        try {
            ByteBuffer audioDataBuffer = readWavFile(assetFilePath);
            float[] audioBuffer = byteBufferToFloatArray(audioDataBuffer);
            return classify(audioBuffer);
        } catch (IOException e) {
            Log.e("ResnetClassifier", "Error reading file for classification", e);
            return null;
        }
    }

    // WAV 파일을 읽어와서 ByteBuffer로 변환하는 메서드
    public ByteBuffer readWavFile(String fileName) throws IOException {
        try (InputStream inputStream = context.getAssets().open(fileName)) {
            byte[] data = new byte[inputStream.available()];
            inputStream.read(data);

            int headerSize = 44;
            int audioDataSize = (data.length - headerSize) / 2;
            float[] audioData = new float[audioDataSize];

            for (int i = 0; i < audioDataSize; i++) {
                short sample = (short) ((data[headerSize + 2 * i + 1] << 8) | (data[headerSize + 2 * i] & 0xFF));
                audioData[i] = sample / 32768.0f;
            }

            return createInputBuffer(audioData);
        }
    }


}
