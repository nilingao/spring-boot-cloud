package cn.com.nla.common.facesdk.core.pool;

import com.seeta.sdk.QualityOfLBN;
import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.apache.commons.pool2.impl.GenericObjectPool;

/** Pool owning the native QualityOfLBN handles. */
public class QualityOfLBNPool extends GenericObjectPool<QualityOfLBN> {
    public QualityOfLBNPool(SeetaConfSetting<QualityOfLBN> config) {
        super(new BasePooledObjectFactory<>() {
            @Override
            public QualityOfLBN create() throws Exception { return new QualityOfLBN(config.getSeetaModelSetting()); }
            @Override
            public PooledObject<QualityOfLBN> wrap(QualityOfLBN object) { return new DefaultPooledObject<>(object); }
            @Override
            public void destroyObject(PooledObject<QualityOfLBN> object) { object.getObject().close(); }
            @Override
            public boolean validateObject(PooledObject<QualityOfLBN> object) { return object.getObject().impl != 0; }
        }, config);
    }
}
