package cn.com.nla.common.facesdk.core.pool;

import com.seeta.sdk.QualityOfPose;
import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.apache.commons.pool2.impl.GenericObjectPool;

/** Pool owning the native QualityOfPose handles. */
public class QualityOfPosePool extends GenericObjectPool<QualityOfPose> {
    public QualityOfPosePool(SeetaConfSetting<QualityOfPose> config) {
        super(new BasePooledObjectFactory<>() {
            @Override
            public QualityOfPose create() throws Exception { return new QualityOfPose(); }
            @Override
            public PooledObject<QualityOfPose> wrap(QualityOfPose object) { return new DefaultPooledObject<>(object); }
            @Override
            public void destroyObject(PooledObject<QualityOfPose> object) { object.getObject().close(); }
            @Override
            public boolean validateObject(PooledObject<QualityOfPose> object) { return object.getObject().impl != 0; }
        }, config);
    }
}
