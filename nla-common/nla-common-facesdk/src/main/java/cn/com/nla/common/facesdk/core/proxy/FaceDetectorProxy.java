package cn.com.nla.common.facesdk.core.proxy;

import cn.com.nla.common.facesdk.core.pool.FaceDetectorPool;
import cn.com.nla.common.facesdk.core.pool.SeetaConfSetting;
import com.seeta.sdk.FaceDetector;
import com.seeta.sdk.SeetaImageData;
import com.seeta.sdk.SeetaRect;

/**
 * 人脸位置评估器
 */
public class FaceDetectorProxy implements AutoCloseable {

    private FaceDetectorPool faceDetectorPool;

    private FaceDetectorProxy(){}

    public FaceDetectorProxy(SeetaConfSetting<FaceDetector> config) {
        faceDetectorPool = new FaceDetectorPool(config);
    }


    public SeetaRect[] detect(SeetaImageData image) throws Exception {
        FaceDetector faceDetector = null;

        SeetaRect[] detect;

        try {
            faceDetector = faceDetectorPool.borrowObject();
            detect = faceDetector.Detect(image);
        }finally {
            if (faceDetector != null) {
                faceDetectorPool.returnObject(faceDetector);
            }
        }

        return detect;
    }


//    public void setProperty(FaceDetector.Property property, double value) throws Exception {
//
//        FaceDetector faceDetector = null;
//        SeetaRect[] detect;
//        try {
//            faceDetector = faceDetectorPool.borrowObject();
//
//        }finally {
//            if (faceDetector != null) {
//                faceDetectorPool.returnObject(faceDetector);
//            }
//        }
//
//    }

    @Override
    public void close() { faceDetectorPool.close(); }
}
