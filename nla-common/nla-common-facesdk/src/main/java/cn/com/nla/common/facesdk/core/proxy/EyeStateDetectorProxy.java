package cn.com.nla.common.facesdk.core.proxy;

import cn.com.nla.common.facesdk.core.pool.EyeStateDetectorPool;
import cn.com.nla.common.facesdk.core.pool.SeetaConfSetting;
import com.seeta.sdk.EyeStateDetector;
import com.seeta.sdk.SeetaImageData;
import com.seeta.sdk.SeetaPointF;

public class EyeStateDetectorProxy implements AutoCloseable {

    private EyeStateDetectorPool pool;

    private EyeStateDetectorProxy() {
    }

    public EyeStateDetectorProxy(SeetaConfSetting<EyeStateDetector> confSetting) {
        pool = new EyeStateDetectorPool(confSetting);
    }

    public EyeStateDetector.EYE_STATE[] detect(SeetaImageData imageData, SeetaPointF[] points) {

        EyeStateDetector eyeStateDetector = null;
        EyeStateDetector.EYE_STATE[] states = null;

        try {

            eyeStateDetector = pool.borrowObject();
            states = eyeStateDetector.detect(imageData, points);

        } catch (Exception e) {
            throw new IllegalStateException("Face SDK operation failed", e);
        } finally {
            if (eyeStateDetector != null) {
                pool.returnObject(eyeStateDetector);
            }
        }

        return states;
    }
    @Override
    public void close() { pool.close(); }
}
