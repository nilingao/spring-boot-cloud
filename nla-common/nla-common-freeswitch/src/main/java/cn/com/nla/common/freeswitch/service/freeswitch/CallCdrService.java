package cn.com.nla.common.freeswitch.service.freeswitch;

import cn.com.nla.common.freeswitch.model.call.CallDetail;
import cn.com.nla.common.freeswitch.model.fs.CallDeviceInfo;
import cn.com.nla.common.freeswitch.model.fs.CallLogInfo;

import java.util.List;

public interface CallCdrService {
    /**
     * 保存话单信息
     */
    void saveOrUpdateCallLog(CallLogInfo callLog);

    /**
     * 保存话单设备信息
     */
    void saveCallDevice(CallDeviceInfo callDevice);

    /**
     * 保存话单明细详情
     */
    void saveCallDetail(List<CallDetail> callDetails);
}
