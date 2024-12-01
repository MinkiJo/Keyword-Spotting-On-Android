package com.marusys.hesap.util;

import android.util.Log;

import java.util.Arrays;

public class ThresholdUtil {

    // Softmax 함수 계산
    public static double[] calculateSoftmax(float[] values) {
        float max = Float.NEGATIVE_INFINITY;
        for (float value : values) {
            if (value > max) max = value;
        }

        double sum = 0.0;
        double[] expValues = new double[values.length];

        // 값들에 대한 지수 계산 및 총합 계산
        for (int i = 0; i < values.length; i++) {
            expValues[i] = Math.exp(values[i] - max); // Overflow 방지
            sum += expValues[i];
        }

        // 각 값에 대한 Softmax 확률 계산
        for (int i = 0; i < expValues.length; i++) {
            expValues[i] /= sum;
        }
        return expValues;
    }

    // Threshold 검사 함수
    public static double checkTrigger(float[] values) {
        double[] softmaxValues = calculateSoftmax(values);

        // 가장 높은 소프트맥스 값을 가진 인덱스 찾기
        double maxSoftmaxValue = -1.0;
        for (double value : softmaxValues) {
            if (value > maxSoftmaxValue) maxSoftmaxValue = value;
        }

        return maxSoftmaxValue;
    }
}
