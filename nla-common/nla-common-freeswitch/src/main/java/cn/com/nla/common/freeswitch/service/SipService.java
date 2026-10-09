package cn.com.nla.common.freeswitch.service;

import cn.com.nla.common.freeswitch.service.sip.ParentPlatformService;
import cn.hutool.extra.spring.SpringUtil;

public class SipService {
    public static ParentPlatformService getParentPlatformService() {
        return SpringUtil.getBean(ParentPlatformService.class);
    }
}
