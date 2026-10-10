package cn.com.nla.common.facesdk.core.pool;

import com.seeta.sdk.GenderPredictor;
import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.apache.commons.pool2.impl.GenericObjectPool;

/** Pool owning the native GenderPredictor handles. */
public class GenderPredictorPool extends GenericObjectPool<GenderPredictor> {
    public GenderPredictorPool(SeetaConfSetting<GenderPredictor> config) {
        super(new BasePooledObjectFactory<>() {
            @Override
            public GenderPredictor create() throws Exception { return new GenderPredictor(config.getSeetaModelSetting()); }
            @Override
            public PooledObject<GenderPredictor> wrap(GenderPredictor object) { return new DefaultPooledObject<>(object); }
            @Override
            public void destroyObject(PooledObject<GenderPredictor> object) { object.getObject().close(); }
            @Override
            public boolean validateObject(PooledObject<GenderPredictor> object) { return object.getObject().impl != 0; }
        }, config);
    }
}
