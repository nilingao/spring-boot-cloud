package cn.com.nla.common.facesdk.core.pool;

import com.seeta.sdk.QualityOfClarity;
import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.apache.commons.pool2.impl.GenericObjectPool;

/** Pool owning the native QualityOfClarity handles. */
public class QualityOfClarityPool extends GenericObjectPool<QualityOfClarity> {
    public QualityOfClarityPool(SeetaConfSetting<QualityOfClarity> config) {
        super(new BasePooledObjectFactory<>() {
            @Override
            public QualityOfClarity create() throws Exception { return new QualityOfClarity(); }
            @Override
            public PooledObject<QualityOfClarity> wrap(QualityOfClarity object) { return new DefaultPooledObject<>(object); }
            @Override
            public void destroyObject(PooledObject<QualityOfClarity> object) { object.getObject().close(); }
            @Override
            public boolean validateObject(PooledObject<QualityOfClarity> object) { return object.getObject().impl != 0; }
        }, config);
    }
}
