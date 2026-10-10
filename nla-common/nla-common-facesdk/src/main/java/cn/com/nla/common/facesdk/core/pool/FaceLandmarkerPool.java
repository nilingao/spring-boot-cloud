package cn.com.nla.common.facesdk.core.pool;

import com.seeta.sdk.FaceLandmarker;
import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.apache.commons.pool2.impl.GenericObjectPool;

/** Pool owning the native FaceLandmarker handles. */
public class FaceLandmarkerPool extends GenericObjectPool<FaceLandmarker> {
    public FaceLandmarkerPool(SeetaConfSetting<FaceLandmarker> config) {
        super(new BasePooledObjectFactory<>() {
            @Override
            public FaceLandmarker create() throws Exception { return new FaceLandmarker(config.getSeetaModelSetting()); }
            @Override
            public PooledObject<FaceLandmarker> wrap(FaceLandmarker object) { return new DefaultPooledObject<>(object); }
            @Override
            public void destroyObject(PooledObject<FaceLandmarker> object) { object.getObject().close(); }
            @Override
            public boolean validateObject(PooledObject<FaceLandmarker> object) { return object.getObject().impl != 0; }
        }, config);
    }
}
