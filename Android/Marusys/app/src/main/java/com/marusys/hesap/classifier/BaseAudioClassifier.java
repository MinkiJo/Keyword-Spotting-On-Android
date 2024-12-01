package com.marusys.hesap.classifier;

import android.content.Context;

public abstract class BaseAudioClassifier {
    protected Context context;

    public BaseAudioClassifier(Context context) {
        this.context = context;
    }

    // 오디오 데이터를 분류하는 추상 메서드
    public abstract float[] classify(float[] audioData);

    // 기본적으로 빈 구현 제공
    public String getLabel(float[] result) {
        // CNN에서는 호출하지 않으면 됨
        return "unsupported";
    }

}
