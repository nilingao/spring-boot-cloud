package cn.com.nla.common.facesdk.core.proxy;

import cn.com.nla.common.facesdk.core.pool.AgePredictorPool;
import cn.com.nla.common.facesdk.core.pool.SeetaConfSetting;
import com.seeta.sdk.AgePredictor;
import com.seeta.sdk.SeetaImageData;
import com.seeta.sdk.SeetaPointF;


/**
 * 年龄评估器
 */
public class AgePredictorProxy implements AutoCloseable {

    private AgePredictorPool pool;

    private AgePredictorProxy() {
    }


    public AgePredictorProxy(SeetaConfSetting<AgePredictor> config) {
        pool = new AgePredictorPool(config);
    }

    public int predictAgeWithCrop(SeetaImageData image, SeetaPointF[] points) {

        AgePredictor agePredictor = null;

        int[] age = new int[1];

        try {
            agePredictor = pool.borrowObject();
            agePredictor.PredictAgeWithCrop(image, points, age);
        } catch (Exception e) {
            throw new IllegalStateException("Face SDK operation failed", e);
        } finally {
            if (agePredictor != null) {
                pool.returnObject(agePredictor);
            }
        }
        return age[0];
    }
    @Override
    public void close() { pool.close(); }
}
