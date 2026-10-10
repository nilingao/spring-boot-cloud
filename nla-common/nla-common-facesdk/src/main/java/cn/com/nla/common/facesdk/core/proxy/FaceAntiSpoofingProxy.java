package cn.com.nla.common.facesdk.core.proxy;

import cn.com.nla.common.facesdk.core.pool.FaceAntiSpoofingPool;
import cn.com.nla.common.facesdk.core.pool.SeetaConfSetting;
import com.seeta.sdk.FaceAntiSpoofing;
import com.seeta.sdk.SeetaImageData;
import com.seeta.sdk.SeetaPointF;
import com.seeta.sdk.SeetaRect;

public class FaceAntiSpoofingProxy implements AutoCloseable {

    private FaceAntiSpoofingPool pool;

    private FaceAntiSpoofingProxy() {
    }

    public FaceAntiSpoofingProxy(SeetaConfSetting<FaceAntiSpoofing> setting) {
        pool = new FaceAntiSpoofingPool(setting);
    }

    public FaceAntiSpoofing.Status predict(SeetaImageData image, SeetaRect face, SeetaPointF[] landmarks) {

        FaceAntiSpoofing.Status status = null;
        FaceAntiSpoofing faceAntiSpoofing = null;
        try {

            faceAntiSpoofing = pool.borrowObject();
            status = faceAntiSpoofing.Predict(image, face, landmarks);

        } catch (Exception e) {
            throw new IllegalStateException("Face SDK operation failed", e);
        } finally {
            if (faceAntiSpoofing != null) {
                pool.returnObject(faceAntiSpoofing);
            }
        }

        return status;
    }
    @Override
    public void close() { pool.close(); }
}
