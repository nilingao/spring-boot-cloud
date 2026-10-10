package cn.com.nla.common.facesdk.core.proxy;

import cn.com.nla.common.facesdk.core.pool.QualityOfClarityPool;
import cn.com.nla.common.facesdk.core.pool.SeetaConfSetting;
import com.seeta.sdk.QualityOfClarity;
import com.seeta.sdk.SeetaImageData;
import com.seeta.sdk.SeetaPointF;
import com.seeta.sdk.SeetaRect;

public class QualityOfClarityProxy implements AutoCloseable {

    private QualityOfClarityPool pool;


    private QualityOfClarityProxy() {
    }

    public QualityOfClarityProxy(SeetaConfSetting<QualityOfClarity> setting) {

        pool = new QualityOfClarityPool(setting);
    }


    public ClarityItem check(SeetaImageData imageData, SeetaRect face, SeetaPointF[] landmarks) {

        float[] score = new float[1];

        QualityOfClarity qualityOfClarity = null;
        QualityOfClarity.QualityLevel check = null;
        try {

            qualityOfClarity = pool.borrowObject();
            check = qualityOfClarity.check(imageData, face, landmarks, score);

        } catch (Exception e) {
            throw new IllegalStateException("Face SDK operation failed", e);
        } finally {
            if (qualityOfClarity != null) {
                pool.returnObject(qualityOfClarity);
            }
        }

        return new ClarityItem(check, score[0]);
    }

    public class ClarityItem {
        private QualityOfClarity.QualityLevel qualityLevel;
        private float score;

        public ClarityItem(QualityOfClarity.QualityLevel qualityLevel, float score) {
            this.qualityLevel = qualityLevel;
            this.score = score;
        }

        public QualityOfClarity.QualityLevel getQualityLevel() {
            return qualityLevel;
        }

        public void setQualityLevel(QualityOfClarity.QualityLevel qualityLevel) {
            this.qualityLevel = qualityLevel;
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
