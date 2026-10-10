package cn.com.nla.common.facesdk.core.pool;

import com.seeta.sdk.PoseEstimator;
import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.apache.commons.pool2.impl.GenericObjectPool;

/** Pool owning the native PoseEstimator handles. */
public class PoseEstimatorPool extends GenericObjectPool<PoseEstimator> {
    public PoseEstimatorPool(SeetaConfSetting<PoseEstimator> config) {
        super(new BasePooledObjectFactory<>() {
            @Override
            public PoseEstimator create() throws Exception { return new PoseEstimator(config.getSeetaModelSetting()); }
            @Override
            public PooledObject<PoseEstimator> wrap(PoseEstimator object) { return new DefaultPooledObject<>(object); }
            @Override
            public void destroyObject(PooledObject<PoseEstimator> object) { object.getObject().close(); }
            @Override
            public boolean validateObject(PooledObject<PoseEstimator> object) { return object.getObject().impl != 0; }
        }, config);
    }
}
