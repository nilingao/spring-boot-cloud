package cn.com.nla.common.facesdk.core.proxy;

import cn.com.nla.common.facesdk.core.pool.MaskDetectorPool;
import cn.com.nla.common.facesdk.core.pool.SeetaConfSetting;
import com.seeta.sdk.MaskDetector;
import com.seeta.sdk.SeetaImageData;
import com.seeta.sdk.SeetaRect;

public class MaskDetectorProxy implements AutoCloseable {

    private MaskDetectorPool pool;

    private MaskDetectorProxy() {
    }


    public MaskDetectorProxy(SeetaConfSetting<MaskDetector> confSetting) {

        pool = new MaskDetectorPool(confSetting);
    }


    public MaskItem detect(SeetaImageData imageData, SeetaRect face) {

        float[] score = new float[1];
        boolean detect = false;
        MaskDetector maskDetector = null;
        try {
            maskDetector = pool.borrowObject();
            detect = maskDetector.detect(imageData, face, score);

        } catch (Exception e) {
            throw new IllegalStateException("Face SDK operation failed", e);
        } finally {
            if (maskDetector != null) {
                pool.returnObject(maskDetector);
            }
        }

        return new MaskItem(detect, score[0]);
    }


    public class MaskItem {
        private float score;

        private boolean mask;

        public MaskItem(boolean mask, float score) {
            this.mask = mask;
            this.score = score;
        }

        public float getScore() {
            return score;
        }

        public void setScore(float score) {
            this.score = score;
        }


        public boolean getMask() {
            return mask;
        }

        public void setMask(boolean mask) {
            this.mask = mask;
        }
    }

    @Override
    public void close() { pool.close(); }
}
