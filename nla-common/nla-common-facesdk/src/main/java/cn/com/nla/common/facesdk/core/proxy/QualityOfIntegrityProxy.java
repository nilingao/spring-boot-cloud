package cn.com.nla.common.facesdk.core.proxy;

import cn.com.nla.common.facesdk.core.pool.QualityOfIntegrityPool;
import cn.com.nla.common.facesdk.core.pool.SeetaConfSetting;
import com.seeta.sdk.QualityOfIntegrity;
import com.seeta.sdk.SeetaImageData;
import com.seeta.sdk.SeetaPointF;
import com.seeta.sdk.SeetaRect;

public class QualityOfIntegrityProxy implements AutoCloseable {

    private QualityOfIntegrityPool pool;

    private QualityOfIntegrityProxy() {
    }

    public QualityOfIntegrityProxy(SeetaConfSetting<QualityOfIntegrity> setting) {

        pool = new QualityOfIntegrityPool(setting);
    }

    public IntegrityItem check(SeetaImageData imageData, SeetaRect face, SeetaPointF[] landmarks) {
        float[] score = new float[1];
        QualityOfIntegrity.QualityLevel qualityLevel = null;

        QualityOfIntegrity qualityOfIntegrity = null;

        try {
            qualityOfIntegrity = pool.borrowObject();
            qualityLevel = qualityOfIntegrity.check(imageData, face, landmarks, score);
        } catch (Exception e) {
            throw new IllegalStateException("Face SDK operation failed", e);
        } finally {
            if (qualityOfIntegrity != null) {
                pool.returnObject(qualityOfIntegrity);
            }
        }

        return new IntegrityItem(qualityLevel, score[0]);

    }

    public class IntegrityItem {
        private QualityOfIntegrity.QualityLevel qualityLevel;

        private float score;

        public IntegrityItem(QualityOfIntegrity.QualityLevel qualityLevel, float score) {
            this.qualityLevel = qualityLevel;
            this.score = score;
        }

        public QualityOfIntegrity.QualityLevel getQualityLevel() {
            return qualityLevel;
        }

        public void setQualityLevel(QualityOfIntegrity.QualityLevel qualityLevel) {
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
