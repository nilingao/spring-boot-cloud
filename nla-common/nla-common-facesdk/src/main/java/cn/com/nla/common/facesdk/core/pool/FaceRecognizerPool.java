package cn.com.nla.common.facesdk.core.pool;

import com.seeta.sdk.FaceRecognizer;
import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.apache.commons.pool2.impl.GenericObjectPool;

/** Pool owning the native FaceRecognizer handles. */
public class FaceRecognizerPool extends GenericObjectPool<FaceRecognizer> {
    public FaceRecognizerPool(SeetaConfSetting<FaceRecognizer> config) {
        super(new BasePooledObjectFactory<>() {
            @Override
            public FaceRecognizer create() throws Exception { return new FaceRecognizer(config.getSeetaModelSetting()); }
            @Override
            public PooledObject<FaceRecognizer> wrap(FaceRecognizer object) { return new DefaultPooledObject<>(object); }
            @Override
            public void destroyObject(PooledObject<FaceRecognizer> object) { object.getObject().close(); }
            @Override
            public boolean validateObject(PooledObject<FaceRecognizer> object) { return object.getObject().impl != 0; }
        }, config);
    }
}
