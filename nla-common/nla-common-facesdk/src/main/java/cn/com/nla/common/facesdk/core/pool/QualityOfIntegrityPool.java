package cn.com.nla.common.facesdk.core.pool;

import com.seeta.sdk.QualityOfIntegrity;
import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.apache.commons.pool2.impl.GenericObjectPool;

/** Pool owning the native QualityOfIntegrity handles. */
public class QualityOfIntegrityPool extends GenericObjectPool<QualityOfIntegrity> {
    public QualityOfIntegrityPool(SeetaConfSetting<QualityOfIntegrity> config) {
        super(new BasePooledObjectFactory<>() {
            @Override
            public QualityOfIntegrity create() throws Exception { return new QualityOfIntegrity(); }
            @Override
            public PooledObject<QualityOfIntegrity> wrap(QualityOfIntegrity object) { return new DefaultPooledObject<>(object); }
            @Override
            public void destroyObject(PooledObject<QualityOfIntegrity> object) { object.getObject().close(); }
            @Override
            public boolean validateObject(PooledObject<QualityOfIntegrity> object) { return object.getObject().impl != 0; }
        }, config);
    }
}
