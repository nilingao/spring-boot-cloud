package cn.com.nla.common.facesdk.core.pool;

import com.seeta.sdk.QualityOfResolution;
import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.apache.commons.pool2.impl.GenericObjectPool;

/** Pool owning the native QualityOfResolution handles. */
public class QualityOfResolutionPool extends GenericObjectPool<QualityOfResolution> {
    public QualityOfResolutionPool(SeetaConfSetting<QualityOfResolution> config) {
        super(new BasePooledObjectFactory<>() {
            @Override
            public QualityOfResolution create() throws Exception { return new QualityOfResolution(); }
            @Override
            public PooledObject<QualityOfResolution> wrap(QualityOfResolution object) { return new DefaultPooledObject<>(object); }
            @Override
            public void destroyObject(PooledObject<QualityOfResolution> object) { object.getObject().close(); }
            @Override
            public boolean validateObject(PooledObject<QualityOfResolution> object) { return object.getObject().impl != 0; }
        }, config);
    }
}
