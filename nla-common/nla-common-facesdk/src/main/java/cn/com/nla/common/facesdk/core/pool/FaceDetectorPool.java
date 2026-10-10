package cn.com.nla.common.facesdk.core.pool;

import com.seeta.sdk.FaceDetector;
import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.apache.commons.pool2.impl.GenericObjectPool;

/** Pool owning the native FaceDetector handles. */
public class FaceDetectorPool extends GenericObjectPool<FaceDetector> {
    public FaceDetectorPool(SeetaConfSetting<FaceDetector> config) {
        super(new BasePooledObjectFactory<>() {
            @Override
            public FaceDetector create() throws Exception { return new FaceDetector(config.getSeetaModelSetting()); }
            @Override
            public PooledObject<FaceDetector> wrap(FaceDetector object) { return new DefaultPooledObject<>(object); }
            @Override
            public void destroyObject(PooledObject<FaceDetector> object) { object.getObject().close(); }
            @Override
            public boolean validateObject(PooledObject<FaceDetector> object) { return object.getObject().impl != 0; }
        }, config);
    }
}
