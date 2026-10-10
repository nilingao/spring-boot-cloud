package cn.com.nla.common.facesdk.core.pool;

import com.seeta.sdk.AgePredictor;
import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.apache.commons.pool2.impl.GenericObjectPool;

/** Pool owning the native AgePredictor handles. */
public class AgePredictorPool extends GenericObjectPool<AgePredictor> {
    public AgePredictorPool(SeetaConfSetting<AgePredictor> config) {
        super(new BasePooledObjectFactory<>() {
            @Override
            public AgePredictor create() throws Exception { return new AgePredictor(config.getSeetaModelSetting()); }
            @Override
            public PooledObject<AgePredictor> wrap(AgePredictor object) { return new DefaultPooledObject<>(object); }
            @Override
            public void destroyObject(PooledObject<AgePredictor> object) { object.getObject().close(); }
            @Override
            public boolean validateObject(PooledObject<AgePredictor> object) { return object.getObject().impl != 0; }
        }, config);
    }
}
