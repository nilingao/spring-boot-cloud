package cn.com.nla.common.facesdk.core.pool;

import com.seeta.sdk.MaskDetector;
import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.apache.commons.pool2.impl.GenericObjectPool;

/** Pool owning the native MaskDetector handles. */
public class MaskDetectorPool extends GenericObjectPool<MaskDetector> {
    public MaskDetectorPool(SeetaConfSetting<MaskDetector> config) {
        super(new BasePooledObjectFactory<>() {
            @Override
            public MaskDetector create() throws Exception { return new MaskDetector(config.getSeetaModelSetting()); }
            @Override
            public PooledObject<MaskDetector> wrap(MaskDetector object) { return new DefaultPooledObject<>(object); }
            @Override
            public void destroyObject(PooledObject<MaskDetector> object) { object.getObject().close(); }
            @Override
            public boolean validateObject(PooledObject<MaskDetector> object) { return object.getObject().impl != 0; }
        }, config);
    }
}
