package cn.com.nla.common.facesdk.core.proxy;

import cn.com.nla.common.facesdk.core.pool.FaceLandmarkerPool;
import cn.com.nla.common.facesdk.core.pool.SeetaConfSetting;
import com.seeta.sdk.*;
import com.seeta.sdk.*;


/**
 * 人脸特征点检测器  有 5点和68点
 */
public class FaceLandmarkerProxy implements AutoCloseable {

    private FaceLandmarkerPool pool;

    private FaceLandmarkerProxy() {
    }

    public FaceLandmarkerProxy(SeetaConfSetting<FaceLandmarker> config) {
        pool = new FaceLandmarkerPool(config);
    }

    public SeetaPointF[] mark(SeetaImageData imageData, SeetaRect seetaRect) {

        FaceLandmarker faceLandmarker = null;
        SeetaPointF[] pointFS = null;

        try {

            faceLandmarker = pool.borrowObject();
            pointFS = new SeetaPointF[faceLandmarker.number()];
            faceLandmarker.mark(imageData, seetaRect, pointFS);

        } catch (Exception e) {
            throw new IllegalStateException("Face SDK operation failed", e);
        } finally {
            if (faceLandmarker != null) {
                pool.returnObject(faceLandmarker);
            }
        }

        return pointFS;
    }

    public LandmarkerMask isMask(SeetaImageData imageData, SeetaRect seetaRect) {
        FaceLandmarker faceLandmarker = null;
        LandmarkerMask landmarkerMask = new LandmarkerMask();
        try {
            faceLandmarker = pool.borrowObject();
            SeetaPointF[] pointFS = new SeetaPointF[faceLandmarker.number()];
            int[] masks = new int[faceLandmarker.number()];
            faceLandmarker.mark(imageData, seetaRect, pointFS, masks);
            landmarkerMask.setMasks(masks);
            landmarkerMask.setSeetaPointFS(pointFS);
        } catch (Exception e) {
            throw new IllegalStateException("Face SDK operation failed", e);
        } finally {
            if (faceLandmarker != null) {
                pool.returnObject(faceLandmarker);
            }
        }
        return landmarkerMask;
    }

    public int number() {
        FaceLandmarker faceLandmarker = null;
        try {
            faceLandmarker = pool.borrowObject();
            if (faceLandmarker != null) {
                return faceLandmarker.number();
            }
        } catch (Exception e) {
            throw new IllegalStateException("Face SDK operation failed", e);
        } finally {
            if (faceLandmarker != null) {
                pool.returnObject(faceLandmarker);
            }
        }
        return 0;
    }


    @Override
    public void close() { pool.close(); }
}
