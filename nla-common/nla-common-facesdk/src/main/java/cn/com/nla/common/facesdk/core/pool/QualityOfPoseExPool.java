package cn.com.nla.common.facesdk.core.pool;

import com.seeta.sdk.QualityOfPoseEx;
import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.apache.commons.pool2.impl.GenericObjectPool;

/** Pool owning the native QualityOfPoseEx handles. */
public class QualityOfPoseExPool extends GenericObjectPool<QualityOfPoseEx> {
    public QualityOfPoseExPool(SeetaConfSetting<QualityOfPoseEx> config) {
        super(new BasePooledObjectFactory<>() {
            @Override
            public QualityOfPoseEx create() throws Exception { return new QualityOfPoseEx(config.getSeetaModelSetting()); }
            @Override
            public PooledObject<QualityOfPoseEx> wrap(QualityOfPoseEx object) { return new DefaultPooledObject<>(object); }
            @Override
            public void destroyObject(PooledObject<QualityOfPoseEx> object) { object.getObject().close(); }
            @Override
            public boolean validateObject(PooledObject<QualityOfPoseEx> object) { return object.getObject().impl != 0; }
        }, config);
    }
}
