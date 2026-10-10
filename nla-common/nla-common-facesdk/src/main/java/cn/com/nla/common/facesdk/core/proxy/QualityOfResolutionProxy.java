package cn.com.nla.common.facesdk.core.proxy;

import cn.com.nla.common.facesdk.core.pool.QualityOfResolutionPool;
import cn.com.nla.common.facesdk.core.pool.SeetaConfSetting;
import com.seeta.sdk.QualityOfResolution;
import com.seeta.sdk.SeetaImageData;
import com.seeta.sdk.SeetaPointF;
import com.seeta.sdk.SeetaRect;

public class QualityOfResolutionProxy implements AutoCloseable {

    private QualityOfResolutionPool pool;


    private QualityOfResolutionProxy() {
    }

    public QualityOfResolutionProxy(SeetaConfSetting<QualityOfResolution> confSetting) {

        pool = new QualityOfResolutionPool(confSetting);
    }


    /**
     * 评估人脸尺寸
     *
     * @param imageData
     * @param face
     * @param landmarks [output] quality score
     * @return QualityLevel
     */
    public QualityOfResolution.QualityLevel check(SeetaImageData imageData, SeetaRect face, SeetaPointF[] landmarks) {

        float[] score = new float[1];

        QualityOfResolution.QualityLevel qualityLevel = null;
        QualityOfResolution qualityOfResolution = null;
        try {
            qualityOfResolution = pool.borrowObject();
            qualityLevel = qualityOfResolution.check(imageData, face, landmarks, score);
        } catch (Exception e) {
            throw new IllegalStateException("Face SDK operation failed", e);
        } finally {
            if (qualityOfResolution != null) {
                pool.returnObject(qualityOfResolution);
            }
        }
        return qualityLevel;
    }

    @Override
    public void close() { pool.close(); }
}
