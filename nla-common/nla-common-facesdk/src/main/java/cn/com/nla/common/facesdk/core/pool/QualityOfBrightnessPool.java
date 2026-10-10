package cn.com.nla.common.facesdk.core.pool;

import com.seeta.sdk.QualityOfBrightness;
import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.apache.commons.pool2.impl.GenericObjectPool;

/** Pool owning the native QualityOfBrightness handles. */
public class QualityOfBrightnessPool extends GenericObjectPool<QualityOfBrightness> {
    public QualityOfBrightnessPool(SeetaConfSetting<QualityOfBrightness> config) {
        super(new BasePooledObjectFactory<>() {
            @Override
            public QualityOfBrightness create() throws Exception { return new QualityOfBrightness(); }
            @Override
            public PooledObject<QualityOfBrightness> wrap(QualityOfBrightness object) { return new DefaultPooledObject<>(object); }
            @Override
            public void destroyObject(PooledObject<QualityOfBrightness> object) { object.getObject().close(); }
            @Override
            public boolean validateObject(PooledObject<QualityOfBrightness> object) { return object.getObject().impl != 0; }
        }, config);
    }
}
