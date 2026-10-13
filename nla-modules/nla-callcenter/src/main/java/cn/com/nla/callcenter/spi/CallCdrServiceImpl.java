package cn.com.nla.callcenter.spi;

import cn.com.nla.common.freeswitch.model.call.CallDetail;
import cn.com.nla.common.freeswitch.model.fs.CallDeviceInfo;
import cn.com.nla.common.freeswitch.model.fs.CallLogInfo;
import cn.com.nla.common.freeswitch.service.freeswitch.CallCdrService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 话单落地协议侧实现。
 *
 * <p>旧实现即为空存根（话单持久化域未建），照搬保留契约；话单落库待后续话单域
 * 接入时补齐。仅在 freeswitch.enabled=true 时注册，避免协议组件缺失导致启动失败。</p>
 *
 * @author TZY
 */
@Component
@ConditionalOnProperty(prefix = "freeswitch", name = "enabled", havingValue = "true")
public class CallCdrServiceImpl implements CallCdrService {

    /**
     * 保存话单信息。
     *
     * @param callLog 话单信息
     */
    @Override
    public void saveOrUpdateCallLog(CallLogInfo callLog) {

    }

    /**
     * 保存话单设备信息。
     *
     * @param callDevice 话单设备信息
     */
    @Override
    public void saveCallDevice(CallDeviceInfo callDevice) {

    }

    /**
     * 保存话单明细详情。
     *
     * @param callDetails 话单明细列表
     */
    @Override
    public void saveCallDetail(List<CallDetail> callDetails) {

    }
}
