package cn.com.nla.common.facesdk.core.proxy;

import cn.com.nla.common.facesdk.core.pool.QualityOfPosePool;
import cn.com.nla.common.facesdk.core.pool.SeetaConfSetting;
import com.seeta.sdk.QualityOfPose;
import com.seeta.sdk.SeetaImageData;
import com.seeta.sdk.SeetaPointF;
import com.seeta.sdk.SeetaRect;

public class QualityOfPoseProxy implements AutoCloseable {

    private QualityOfPosePool pool;

    private QualityOfPoseProxy() {
    }

    public QualityOfPoseProxy(SeetaConfSetting<QualityOfPose> confSetting) {
        pool = new QualityOfPosePool(confSetting);
    }

    public QualityOfPose.QualityLevel check(SeetaImageData imageData, SeetaRect face, SeetaPointF[] landmarks) {

        float[] score = new float[1];
        QualityOfPose qualityOfPose = null;
        QualityOfPose.QualityLevel check = null;
        try {
            qualityOfPose = pool.borrowObject();
            check = qualityOfPose.check(imageData, face, landmarks, score);

        } catch (Exception e) {
            throw new IllegalStateException("Face SDK operation failed", e);
        } finally {
            if (qualityOfPose != null) {
                pool.returnObject(qualityOfPose);
            }
        }
        return check;
    }


    @Override
    public void close() { pool.close(); }
}
