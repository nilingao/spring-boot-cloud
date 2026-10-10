package cn.com.nla.common.facesdk.core.pool;

import com.seeta.sdk.EyeStateDetector;
import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.apache.commons.pool2.impl.GenericObjectPool;

/** Pool owning the native EyeStateDetector handles. */
public class EyeStateDetectorPool extends GenericObjectPool<EyeStateDetector> {
    public EyeStateDetectorPool(SeetaConfSetting<EyeStateDetector> config) {
        super(new BasePooledObjectFactory<>() {
            @Override
            public EyeStateDetector create() throws Exception { return new EyeStateDetector(config.getSeetaModelSetting()); }
            @Override
            public PooledObject<EyeStateDetector> wrap(EyeStateDetector object) { return new DefaultPooledObject<>(object); }
            @Override
            public void destroyObject(PooledObject<EyeStateDetector> object) { object.getObject().close(); }
            @Override
            public boolean validateObject(PooledObject<EyeStateDetector> object) { return object.getObject().impl != 0; }
        }, config);
    }
}
