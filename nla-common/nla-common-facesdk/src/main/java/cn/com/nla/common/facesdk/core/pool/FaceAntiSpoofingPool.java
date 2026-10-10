package cn.com.nla.common.facesdk.core.pool;

import com.seeta.sdk.FaceAntiSpoofing;
import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.apache.commons.pool2.impl.GenericObjectPool;

/** Pool owning the native FaceAntiSpoofing handles. */
public class FaceAntiSpoofingPool extends GenericObjectPool<FaceAntiSpoofing> {
    public FaceAntiSpoofingPool(SeetaConfSetting<FaceAntiSpoofing> config) {
        super(new BasePooledObjectFactory<>() {
            @Override
            public FaceAntiSpoofing create() throws Exception { return new FaceAntiSpoofing(config.getSeetaModelSetting()); }
            @Override
            public PooledObject<FaceAntiSpoofing> wrap(FaceAntiSpoofing object) { return new DefaultPooledObject<>(object); }
            @Override
            public void destroyObject(PooledObject<FaceAntiSpoofing> object) { object.getObject().close(); }
            @Override
            public boolean validateObject(PooledObject<FaceAntiSpoofing> object) { return object.getObject().impl != 0; }
        }, config);
    }
}
