package cn.com.nla.common.facesdk.core.proxy;

import cn.com.nla.common.facesdk.core.pool.QualityOfBrightnessPool;
import cn.com.nla.common.facesdk.core.pool.SeetaConfSetting;
import com.seeta.sdk.QualityOfBrightness;
import com.seeta.sdk.SeetaImageData;
import com.seeta.sdk.SeetaPointF;
import com.seeta.sdk.SeetaRect;

public class QualityOfBrightnessProxy implements AutoCloseable {

    private QualityOfBrightnessPool pool;

    private QualityOfBrightnessProxy() {
    }

    public QualityOfBrightnessProxy(SeetaConfSetting<QualityOfBrightness> confSetting) {
        pool = new QualityOfBrightnessPool(confSetting);
    }

    public BrightnessItem check(SeetaImageData imageData, SeetaRect face, SeetaPointF[] landmarks) {

        float[] score = new float[1];
        QualityOfBrightness.QualityLevel check = null;

        QualityOfBrightness qualityOfBrightness = null;

        try {
            qualityOfBrightness = pool.borrowObject();

            check = qualityOfBrightness.check(imageData, face, landmarks, score);

        } catch (Exception e) {
            throw new IllegalStateException("Face SDK operation failed", e);
        } finally {
            if (qualityOfBrightness != null) {
                pool.returnObject(qualityOfBrightness);
            }
        }
        return new BrightnessItem(check, score[0]);

    }

    public class BrightnessItem {

        private QualityOfBrightness.QualityLevel check;
        private float score;

        public BrightnessItem(QualityOfBrightness.QualityLevel check, float score) {
            this.check = check;
            this.score = score;
        }

        public QualityOfBrightness.QualityLevel getCheck() {
            return check;
        }

        public void setCheck(QualityOfBrightness.QualityLevel check) {
            this.check = check;
        }

        public float getScore() {
            return score;
        }

        public void setScore(float score) {
            this.score = score;
        }
    }

    @Override
    public void close() { pool.close(); }
}
